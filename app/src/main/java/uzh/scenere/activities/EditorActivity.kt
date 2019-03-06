package uzh.scenere.activities

import android.content.Intent
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import kotlinx.android.synthetic.main.activity_editor.*
import uzh.scenere.R
import uzh.scenere.activities.EditorActivity.EditorState.*
import uzh.scenere.const.Constants
import uzh.scenere.const.Constants.Companion.NOTHING
import uzh.scenere.const.Constants.Companion.SINGLE_SELECT
import uzh.scenere.const.Constants.Companion.SINGLE_SELECT_WITH_PRESET_POSITION
import uzh.scenere.datamodel.*
import uzh.scenere.datamodel.steps.*
import uzh.scenere.datamodel.trigger.AbstractTrigger
import uzh.scenere.datamodel.trigger.communication.*
import uzh.scenere.datamodel.trigger.direct.*
import uzh.scenere.datamodel.trigger.indirect.CallTrigger
import uzh.scenere.datamodel.trigger.indirect.SmsTrigger
import uzh.scenere.datamodel.trigger.indirect.SoundTrigger
import uzh.scenere.datamodel.trigger.sensor.AccelerationTrigger
import uzh.scenere.datamodel.trigger.sensor.GyroscopeTrigger
import uzh.scenere.datamodel.trigger.sensor.LightTrigger
import uzh.scenere.datamodel.trigger.sensor.MagnetometerTrigger
import uzh.scenere.helpers.*
import uzh.scenere.views.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.reflect.KClass


class EditorActivity : AbstractManagementActivity() {

    override fun isInEditMode(): Boolean {
        return editorState == EDIT
    }

    override fun isInAddMode(): Boolean {
        return editorState == ADD
    }

    override fun isInViewMode(): Boolean {
        return (editorState == EditorState.STEP || editorState == EditorState.TRIGGER || editorState == EditorState.INIT)
    }

    override fun getConfiguredInfoString(): Int {
        return R.string.icon_explain_editor
    }

    override fun getConfiguredLayout(): Int {
        return R.layout.activity_editor
    }

    override fun resetToolbar() {
        customizeToolbarId(R.string.icon_back,null,null,R.string.icon_info,null)
    }

    private val explanationMap: HashMap<Int, Map.Entry<Int, Int>> = HashMap<Int, Map.Entry<Int, Int>>()

    enum class EditorState {
        STEP, TRIGGER, INIT, ADD, EDIT
    }

    private var editorState: EditorState = EditorState.INIT
    private val elementAttributes: Array<String> = arrayOf("", "", "", "", "", "", "", "", "", "")
    private var creationUnitClass: KClass<out IElement>? = null
    private var editUnit: IElement? = null
    //Context
    private var activeScenario: Scenario? = null
    private var projectContext: Project? = null
    private var activePath: Path? = null
    private val pathList = ArrayList<Int>()
    private val pathNameList = ArrayList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activeScenario = intent.getSerializableExtra(Constants.BUNDLE_SCENARIO) as Scenario?
        if (activeScenario != null) {
            projectContext = DatabaseHelper.getInstance(applicationContext).readFull(activeScenario!!.projectId, Project::class)
            activeScenario = DatabaseHelper.getInstance(applicationContext).readFull(activeScenario!!.id, Scenario::class)
        } else {
            //DEV
            projectContext = DatabaseHelper.getInstance(applicationContext).readFull("cdbf3429-1e49-4147-b099-71c76a416aa9", Project::class)
            activeScenario = DatabaseHelper.getInstance(applicationContext).readFull("64fd2449-085b-4ad0-b804-ba392c73b855", Scenario::class)
        }
        var stakeholder: Stakeholder? = null
        if (projectContext != null && !projectContext!!.stakeholders.isNullOrEmpty()) {
            stakeholder = projectContext?.getNextStakeholder()
            activePath = activeScenario?.getPath(stakeholder!!, applicationContext, 0)
        }

        getContentWrapperLayout().setBackgroundColor(ContextCompat.getColor(applicationContext,R.color.sreWhite))
        populateExplanationMap()
        execAdaptToOrientationChange()

        refreshState()

        creationButton = SwipeButton(this, stakeholder?.name
                ?: "No Stakeholders, Path cannot be built.")
                .setColors(ContextCompat.getColor(applicationContext,R.color.sreWhite), ContextCompat.getColor(applicationContext,R.color.srePrimaryDisabledDark))
                .setButtonMode(SwipeButton.SwipeButtonMode.QUADRUPLE)
                .setButtonIcons(R.string.icon_backward, R.string.icon_forward, R.string.icon_null, R.string.icon_plus, null)
                .setButtonStates(true, true, false, false)
                .adaptMasterLayoutParams(true)
                .setFirstPosition()
                .setAutoCollapse(true)
                .updateViews(true)
        creationButton?.setExecutable(createControlExecutable())
        editor_linear_layout_control.addView(creationButton)

        getInfoTitle().text = StringHelper.styleString(getSpannedStringFromId(getConfiguredInfoString()), fontAwesome)
        resetToolbar()
        getInfoTitle().textSize = DipHelper.get(resources).dip2_5.toFloat()
        tutorialOpen = true
        visualizeActivePath()
        tutorialOpen = false
        getContentHolderLayout().setOnHierarchyChangeListener(object : ViewGroup.OnHierarchyChangeListener {
            override fun onChildViewRemoved(parent: View?, child: View?) {
                refreshState(child)
            }

            override fun onChildViewAdded(parent: View?, child: View?) {
                //NOP
            }

        })
        tutorialOpen = SreTutorialLayoutDialog(this,screenWidth,"info_editor_stakeholder","info_editor_element").addEndExecutable { tutorialOpen = false }.show(tutorialOpen)
    }

    private fun visualizeActivePath() {
        if (activePath != null) {
            getContentHolderLayout().removeAllViews()
            var element = activePath?.getStartingPoint()
            while (element != null) {
                renderElement(element)
                element = activePath?.getNextElement(element.getElementId())
            }
        }
        refreshState()
        execScroll()
    }

    private fun addAndRenderElement(element: IElement, edit: Boolean = (editorState==EDIT)) {
        activePath?.add(element)
        DatabaseHelper.getInstance(applicationContext).write(element.getElementId(), element)
        if (edit){
            updateRenderedElement(element)
            execScroll(true)
        }else{
            renderElement(element)
        }
    }

    private fun renderElement(iElement: IElement) {
        var title: String? = null
        if (iElement is StandardStep) title = "<b>" + iElement.readableClassName() + "</b><br>" + iElement.title
        if (iElement is AbstractTrigger) title = "<b>" + iElement.readableClassName() + "</b>"
        val previousAvailable = getContentHolderLayout().childCount != 0
        var tutorialDrawable: String? = null
        if (previousAvailable) {
            connectPreviousToNext()
        }else{
            tutorialDrawable = "info_element"
        }
        var right = false
        var left = false
        if (iElement is StakeholderInteractionTrigger){
            var localStakeholder = 0
            var foreignStakeholder = 0
            for (s in 0 until projectContext!!.stakeholders.size){
                if (projectContext!!.stakeholders[s] == activePath!!.stakeholder){
                    localStakeholder = s
                }
                if (projectContext!!.stakeholders[s].id == iElement.interactedStakeholderId){
                    foreignStakeholder = s
                }
            }
            right = (localStakeholder<foreignStakeholder)
            left = (localStakeholder>foreignStakeholder)
        }
        val element = Element(applicationContext, iElement, previousAvailable, left, right, false).withLabel(StringHelper.fromHtml(title))
        element.setEditExecutable { openInput(iElement) }
        element.setDeleteExecutable {
            DatabaseHelper.getInstance(applicationContext).delete(iElement.getElementId(),IElement::class)
            val prevPosition = getContentHolderLayout().indexOfChild(element)-1
            if (prevPosition >= 0){
                (getContentHolderLayout().getChildAt(prevPosition) as Element).disconnectFromNext()
            }
            getContentHolderLayout().removeView(element)
            activePath?.remove(iElement)
            if (iElement is IfElseTrigger){
                //Delete all Paths associated with this Element
                for (entry in iElement.optionLayerLink){
                    if (entry.value != activePath?.layer){ //Don't delete the current path
                        val path = activeScenario?.removePath(activePath!!.stakeholder,entry.value)
                        if (path != null){
                            DatabaseHelper.getInstance(applicationContext).delete(path.id,Path::class)
                        }
                    }
                }
                pathNameList.remove(pathNameList.last())
                renderAndNotifyPath(activePath?.layer != 0)
            }
        }
        when (iElement){
            is IfElseTrigger -> {
                element.setPathData(iElement.getOptions())
                        .setOnPathIndexSelectedExecutable(onPathSelected)
                        .setInitSelectionExecutable(onPathSelectionInit)
                        .setAddExecutable {onPathAdded(iElement)}
                        .setRemoveExecutable {onPathRemoved(iElement)}
                tutorialDrawable = "info_if_else_config"
            }
            is AbstractStep -> {
                element.setWhatIfExecutable { openWhatIfCreation(iElement) }
            }
        }
        getContentHolderLayout().addView(element)
        colorizeZebraPattern()
        if (tutorialDrawable != null){
            tutorialOpen = SreTutorialLayoutDialog(this,screenWidth,tutorialDrawable).addEndExecutable { tutorialOpen = false }.show(tutorialOpen)
        }
    }

    private fun onPathAdded(iElement: IElement?) {
        if (iElement != null){
            openInput(iElement,InputMode.ADD)
        }
    }

    private fun onPathRemoved(iElement: IElement?) {
        if (iElement != null){
            openInput(iElement,InputMode.REMOVE)
        }
    }

    private fun openWhatIfCreation(iElement: IElement?) {
        if (iElement != null){
            openInput(iElement,InputMode.WHAT_IF)
        }
    }

    private val onPathSelectionInit: (String) -> Unit = {
        if (activePath != null){
            val layer = activePath!!.layer
            if (!pathNameList.contains(StringHelper.concatWithIdBrackets(it,layer))){
                pathNameList.add(StringHelper.concatWithIdBrackets(it,layer))
            }
            renderAndNotifyPath(false)
        }
    }

    private val onPathSelected: (Int,Any?) -> Unit = { index: Int, data: Any? ->
        if (activePath != null && data is IfElseTrigger){
            val layer = data.getLayerForOption(index)
            if (layer != 0){
                pathNameList.remove(pathNameList.last())
            }
            pathList.add(activePath!!.layer)
            activePath = activeScenario?.getPath(activePath!!.stakeholder,this, layer)
            visualizeActivePath()
            creationButton?.setButtonIcons(R.string.icon_backward, R.string.icon_forward, if (layer != 0) R.string.icon_undo else R.string.icon_null, R.string.icon_plus, null)
                    ?.setButtonStates(true, true, (layer != 0), false)
                    ?.updateViews(true)
            val pathName = data.pathOptions[layer]
            if (pathName != null && !pathNameList.contains(StringHelper.concatWithIdBrackets(pathName,layer))){
                pathNameList.add(StringHelper.concatWithIdBrackets(pathName,layer))
                renderAndNotifyPath(true)
            }
            if (layer != 0){
                tutorialOpen = SreTutorialLayoutDialog(this,screenWidth,"info_path_switch").addEndExecutable { tutorialOpen = false }.show(tutorialOpen)
            }
        }
    }

    private fun renderAndNotifyPath(alternativePath: Boolean) {
        if (pathNameList.isEmpty()){
            notify("Currently on the main-Path")
            creationButton?.setText(activePath!!.stakeholder.name)?.updateViews(true)
        }else{
            val pathName = StringHelper.concatListWithoutIdBrackets("->",pathNameList)
            notify(if (alternativePath) "Currently on an alternative-Path: $pathName" else "Currently on the main-Path: $pathName" )
            creationButton?.setText(activePath!!.stakeholder.name + " [$pathName]")?.updateViews(true)
        }
    }

    private fun updateRenderedElement(iElement: IElement) {
        var title: String? = null
        if (iElement is StandardStep) title = "<b>" + iElement.readableClassName() + "</b><br>" + iElement.title
        if (iElement is AbstractTrigger) title = "<b>" + iElement.readableClassName() + "</b>"
        for (v in 0 until getContentHolderLayout().childCount){
            if (getContentHolderLayout().getChildAt(v) is Element && (getContentHolderLayout().getChildAt(v) as Element).containsElement(iElement)){
                (getContentHolderLayout().getChildAt(v) as Element).withLabel(StringHelper.fromHtml(title)).updateElement(iElement).setEditExecutable { openInput(iElement) }
                when (iElement){
                    is IfElseTrigger -> {
                        (getContentHolderLayout().getChildAt(v) as Element).setPathData(iElement.getOptions())
                                .resetSelectCount()
                                .setOnPathIndexSelectedExecutable(onPathSelected)
                                .setAddExecutable {onPathAdded(iElement)}
                                .setRemoveExecutable {onPathRemoved(iElement)}
                    }
                }
            }
        }
    }

    private fun refreshState(view: View? = null) {
        if (view != null && view is Element){
            editorState = if ((view as Element).isStep()) EditorState.STEP else EditorState.TRIGGER
        }else if (getContentHolderLayout().childCount > 0){
            for (v in 0 until getContentHolderLayout().childCount) {
                if (getContentHolderLayout().getChildAt(v) is Element) {
                    editorState = if ((getContentHolderLayout().getChildAt(v) as Element).isStep()) EditorState.TRIGGER else EditorState.STEP
                }
            }
        }else{
            editorState = STEP
        }
        when (editorState) {
            EditorState.INIT -> {
                updateSpinner(R.array.spinner_steps)
                editorState = EditorState.STEP
            }
            EditorState.STEP -> updateSpinner(R.array.spinner_steps)
            EditorState.TRIGGER -> updateSpinner(R.array.spinner_triggers)
            else -> return
        }
    }

    private fun populateExplanationMap() {
        explanationMap[R.array.spinner_steps] = AbstractMap.SimpleEntry(R.string.explanation_steps_title, R.string.explanation_steps_content)
        explanationMap[R.string.step_standard] = AbstractMap.SimpleEntry(R.string.step_standard, R.string.explanation_step_standard)
    }

    private fun createControlExecutable(): SwipeButton.SwipeButtonExecution {
        return object : SwipeButton.SwipeButtonExecution {
            override fun execLeft() {
                val stakeholder: Stakeholder? = projectContext?.getPreviousStakeholder(activePath?.stakeholder)
                if (stakeholder != null) {
                    activePath = activeScenario?.getPath(stakeholder, applicationContext, 0)
                    pathList.clear()
                    pathNameList.clear()
                    creationButton?.setText(stakeholder.name)
                            ?.setButtonIcons(R.string.icon_backward, R.string.icon_forward, R.string.icon_null, R.string.icon_plus, null)
                            ?.setButtonStates(true, true, false, false)
                            ?.updateViews(true)
                    visualizeActivePath()
                }
            }

            override fun execRight() {
                val stakeholder: Stakeholder? = projectContext?.getNextStakeholder(activePath?.stakeholder)
                if (stakeholder != null) {
                    activePath = activeScenario?.getPath(stakeholder, applicationContext, 0)
                    pathList.clear()
                    pathNameList.clear()
                    creationButton?.setText(stakeholder.name)
                            ?.setButtonIcons(R.string.icon_backward, R.string.icon_forward, R.string.icon_null, R.string.icon_plus, null)
                            ?.setButtonStates(true, true, false, false)
                            ?.updateViews(true)
                    visualizeActivePath()
                }
            }

            override fun execDown() {
                openInput()
            }

            override fun execUp() {
                if (activePath?.layer != 0){
                    val lastPathLayer = pathList.last()
                    pathList.remove(lastPathLayer)
                    activePath = activeScenario!!.getPath(activePath!!.stakeholder, applicationContext, lastPathLayer)
                    visualizeActivePath()
                    pathNameList.remove(pathNameList.last())
                }
                if (activePath?.layer == 0 && !pathNameList.isEmpty()){
                    pathNameList.remove(pathNameList.last())
                }
            }
        }
    }


    enum class InputMode{
        UNSPECIFIED, ADD, REMOVE, WHAT_IF
    }
    private fun openInput(element: IElement? = null, inputMode: InputMode = InputMode.UNSPECIFIED) {
        getInfoTitle().textSize = DipHelper.get(resources).dip3_5.toFloat()
        editorState = if (element == null) ADD else EDIT
        editor_spinner_selection?.visibility = View.GONE
        creationButton?.visibility = View.GONE
        if (element != null) {//LOAD
            cleanInfoHolder("Edit " + element.readableClassName())
            editUnit = element
            when (element) {
                //STEPS
                is StandardStep -> {
                    creationUnitClass = StandardStep::class
                    if (inputMode == InputMode.WHAT_IF){
                        val pointer = adaptAttributes(getString(R.string.literal_what_if))
                        getInfoContentWrap().addView(createLine(elementAttributes[pointer], LineInputType.MULTI_TEXT, null, createWhatIfs(element)))
                    }else{
                        var pointer = adaptAttributes("Title", "Text")
                        getInfoContentWrap().addView(createLine(elementAttributes[pointer++], LineInputType.SINGLE_LINE_EDIT, element.title))
                        getInfoContentWrap().addView(createLine(elementAttributes[pointer], LineInputType.MULTI_LINE_CONTEXT_EDIT, element.text))
                        (inputMap[elementAttributes[pointer]] as SreMultiAutoCompleteTextView).setObjects(activeScenario?.objects!!)
                    }
                    execMorphInfoBar(InfoState.MAXIMIZED)
                }
                is InputStep -> {/*TODO*/}
                is LoopbackStep -> {/*TODO*/}
                is MergeStep -> {/*TODO*/}
                is ResourceStep -> {/*TODO*/}
                //TRIGGERS
                is ButtonTrigger -> {
                    creationUnitClass = ButtonTrigger::class
                    adaptAttributes("Button-Label")
                    getInfoContentWrap().addView(createLine(elementAttributes[0], LineInputType.SINGLE_LINE_EDIT, element.buttonLabel))
                    execMorphInfoBar(InfoState.MAXIMIZED)
                }
                is IfElseTrigger -> {
                    creationUnitClass = IfElseTrigger::class
                    var tutorialDrawable: String? = null
                    if (inputMode == InputMode.REMOVE){
                        val index = adaptAttributes("Remove Options")
                        getInfoContentWrap().addView(createLine(elementAttributes[index], LineInputType.LOOKUP,null, addToArrayBefore(element.getDeletableIndexedOptions(),"")))
                        tutorialDrawable = "info_option_removal"
                    }else {
                        pathNameList.remove(pathNameList.last())
                        var index = adaptAttributes("Question","Default Option","Option 1","Option 2","Option 3","Option 4","Option 5")
                        getInfoContentWrap().addView(createLine(elementAttributes[index++], LineInputType.SINGLE_LINE_EDIT, element.text))
                        for (string in element.getOptions()){
                            getInfoContentWrap().addView(createLine(elementAttributes[index++], LineInputType.SINGLE_LINE_EDIT, string))
                        }
                        if (inputMode == InputMode.ADD){
                            getInfoContentWrap().addView(createLine(elementAttributes[index], LineInputType.SINGLE_LINE_EDIT, null))
                            tutorialDrawable = "info_option_add"
                        }
                    }
                    execMorphInfoBar(InfoState.MAXIMIZED)
                    if (tutorialDrawable != null){
                        tutorialOpen = SreTutorialLayoutDialog(this,screenWidth, tutorialDrawable).addEndExecutable { tutorialOpen = false }.show(tutorialOpen)
                    }
                }
                is StakeholderInteractionTrigger -> {
                    creationUnitClass = StakeholderInteractionTrigger::class
                    val stakeholderPositionById = projectContext!!.getStakeholderPositionById(element.interactedStakeholderId!!)+1
                    var index = adaptAttributes("Instruction Text","Stakeholder")
                    getInfoContentWrap().addView(createLine(elementAttributes[index++], LineInputType.SINGLE_LINE_EDIT, element.text))
                    getInfoContentWrap().addView(createLine(elementAttributes[index], LineInputType.LOOKUP, SINGLE_SELECT_WITH_PRESET_POSITION.plus(stakeholderPositionById), addToArrayBefore(projectContext!!.stakeholders.toStringArray(),NOTHING)))
                    execMorphInfoBar(InfoState.MAXIMIZED)
                }
                is InputTrigger -> {
                    creationUnitClass = InputTrigger::class
                    var index = adaptAttributes("Instruction Text", "Correct Answer")
                    getInfoContentWrap().addView(createLine(elementAttributes[index++], LineInputType.SINGLE_LINE_EDIT, element.text))
                    getInfoContentWrap().addView(createLine(elementAttributes[index], LineInputType.SINGLE_LINE_EDIT, element.input))
                    execMorphInfoBar(InfoState.MAXIMIZED)
                }
                is TimeTrigger -> {/*TODO*/}
                is SoundTrigger -> {/*TODO*/}
                is BluetoothTrigger -> {/*TODO*/}
                is GpsTrigger -> {/*TODO*/}
                is MobileNetworkTrigger -> {/*TODO*/}
                is NfcTrigger -> {/*TODO*/}
                is WifiTrigger -> {/*TODO*/}
                is CallTrigger -> {/*TODO*/}
                is SmsTrigger -> {/*TODO*/}
                is AccelerationTrigger -> {/*TODO*/}
                is GyroscopeTrigger -> {/*TODO*/}
                is LightTrigger -> {/*TODO*/}
                is MagnetometerTrigger -> {/*TODO*/}
            }
        } else {//CREATE
            cleanInfoHolder("Add " + editor_spinner_selection.selectedItem)
            when ((editor_spinner_selection.selectedItem as String)) {
                //STEP
                resources.getString(R.string.step_standard) -> {
                    creationUnitClass = StandardStep::class
                    var index = adaptAttributes("Title", "Instruction Text")
                    getInfoContentWrap().addView(createLine(elementAttributes[index++], LineInputType.SINGLE_LINE_EDIT, null))
                    getInfoContentWrap().addView(createLine(elementAttributes[index], LineInputType.MULTI_LINE_CONTEXT_EDIT, null))
                    (inputMap[elementAttributes[index]] as SreMultiAutoCompleteTextView).setObjects(activeScenario?.objects!!)
                    execMorphInfoBar(InfoState.MAXIMIZED)
                    tutorialOpen = SreTutorialLayoutDialog(this,screenWidth,"info_editor_context").addEndExecutable { tutorialOpen = false }.show(tutorialOpen)
                }
                resources.getString(R.string.step_input) -> {/*TODO*/ }
                resources.getString(R.string.step_loopback) -> {/*TODO*/ }
                resources.getString(R.string.step_merge) -> {/*TODO*/ }
                resources.getString(R.string.step_resource) -> {/*TODO*/ }
                //TRIGGER
                resources.getString(R.string.trigger_button) -> {
                    creationUnitClass = ButtonTrigger::class
                    val index = adaptAttributes("Button-Label")
                    getInfoContentWrap().addView(createLine(elementAttributes[index], LineInputType.SINGLE_LINE_EDIT, null))
                    execMorphInfoBar(InfoState.MAXIMIZED)
                }
                resources.getString(R.string.trigger_if_else) -> {
                    creationUnitClass = IfElseTrigger::class
                    var index = adaptAttributes("Question","Default Option")
                    getInfoContentWrap().addView(createLine(elementAttributes[index++], LineInputType.SINGLE_LINE_EDIT, null))
                    getInfoContentWrap().addView(createLine(elementAttributes[index], LineInputType.SINGLE_LINE_EDIT, null))
                    execMorphInfoBar(InfoState.MAXIMIZED)
                    tutorialOpen = SreTutorialLayoutDialog(this,screenWidth,"info_if_else_element").addEndExecutable { tutorialOpen = false }.show(tutorialOpen)
                }
                resources.getString(R.string.trigger_stakeholder_interaction) -> {
                    creationUnitClass = StakeholderInteractionTrigger::class
                    var index = adaptAttributes("Instruction Text","Stakeholder")
                    getInfoContentWrap().addView(createLine(elementAttributes[index++], LineInputType.SINGLE_LINE_EDIT, null))
                    getInfoContentWrap().addView(createLine(elementAttributes[index], LineInputType.LOOKUP,SINGLE_SELECT, addToArrayBefore(projectContext!!.stakeholders.toStringArray(),NOTHING)))
                    execMorphInfoBar(InfoState.MAXIMIZED)
                }
                resources.getString(R.string.trigger_input) -> {
                    creationUnitClass = InputTrigger::class
                    var index = adaptAttributes("Instruction Text", "Correct Answer")
                    getInfoContentWrap().addView(createLine(elementAttributes[index++], LineInputType.SINGLE_LINE_EDIT, null))
                    getInfoContentWrap().addView(createLine(elementAttributes[index], LineInputType.SINGLE_LINE_EDIT, null))
                    execMorphInfoBar(InfoState.MAXIMIZED)
                }
                resources.getString(R.string.trigger_timer) -> {/*TODO*/ }
                resources.getString(R.string.trigger_bt) -> {/*TODO*/ }
                resources.getString(R.string.trigger_gps) -> {/*TODO*/ }
                resources.getString(R.string.trigger_mobile) -> {/*TODO*/ }
                resources.getString(R.string.trigger_nfc) -> {/*TODO*/ }
                resources.getString(R.string.trigger_wifi) -> {/*TODO*/ }
                resources.getString(R.string.trigger_call) -> {/*TODO*/ }
                resources.getString(R.string.trigger_sms) -> {/*TODO*/ }
                resources.getString(R.string.trigger_sound) -> {/*TODO*/ }
                resources.getString(R.string.trigger_acceleration) -> {/*TODO*/ }
                resources.getString(R.string.trigger_light) -> {/*TODO*/ }
                resources.getString(R.string.trigger_magnetic) -> {/*TODO*/ }
            }
        }
    }

    private fun createWhatIfs(element: AbstractStep): Array<String>{
        if (element.whatIfs.isEmpty()) {
            val stakeholder1 = StringHelper.nvl(activePath?.stakeholder?.name,NOTHING)
            var stakeholder2 = "Stakeholder2"
            val list = ArrayList<String>()
            //Variable
            for (o in element.objects){
                list.add("${o.name} cannot participate during this action?")
                list.add("${o.name} is functioning incorrectly during this action?")
                list.add("${o.name} is not functioning during this action?")
                list.add("${o.name} is unavailable during this action?")
            }
            //Attributes
            //TODO Attributes & Config
            //Stakeholders
            val stakeholderCount = NumberHelper.nvl(projectContext?.stakeholders?.size, 0)
            if (stakeholderCount > 1){
                for (s in 0 until stakeholderCount){
                    stakeholder2 = projectContext!!.stakeholders[s].name
                    if (stakeholder2 != stakeholder1)
                    list.add("$stakeholder1 and $stakeholder2 are the same instance of an agent?")
                    list.add("$stakeholder1 and $stakeholder2 fulfil the same roles during this action?")
                    list.add("The roles that are fulfilled by $stakeholder1 and $stakeholder2 are fulfilled by the same agent?")
                }
            }
            list.add("$stakeholder1 is unavailable during this action?")
            //Fixed
            list.add("A different event occurs instead of this event in the scenario?")
            list.add("An unexpected intrusion occurs into the system during this event?")
            list.add("the end of this action is delayed?")
            list.add("The information manipulated in this action is incorrect in some way?")
            list.add("The information manipulated in this action is out-of-date?")
            list.add("The information manipulated in this action is too detailed for the task?")
            list.add("The information manipulated in this action is too general for the task?")
            list.add("The information manipulated in this action is unreliable in some way?")
            list.add("The information manipulated in this action represents a deliberate lie?")
            list.add("The information manipulated is inappropriate to this task?")
            list.add("There is insufficient information available to complete the action?")
            list.add("This action does not complete?")
            list.add("This action ends before it is planned to?")
            list.add("This event and the previous one are the same event?")
            list.add("This event and the previous one occur in the wrong order in the scenario?")
            list.add("This event does not occur in this scenario?")
            list.add("This event occurs earlier in time than expected in the scenario?")
            list.add("This event occurs later in time than expected in the scenario?")
            list.add("This event occurs less frequently than expected in the scenario?")
            list.add("This event occurs more than once in the scenario?")
            list.add("This event occurs more than once in this scenario?")
            list.add("This event repeats at a later time in this scenario?")
            return list.toTypedArray()
        }
        return element.whatIfs.toTypedArray()
    }

    /**
     * Prepares the Input and returns an Index
     */
    private fun adaptAttributes(vararg attributeNames: String): Int {
        for (i in elementAttributes.indices) {
            elementAttributes[i] = ""
        }
        for (name in attributeNames.indices) {
            if (name > elementAttributes.size - 1) {
                return elementAttributes.size - 1
            }
            elementAttributes[name] = attributeNames[name]
        }
        return 0
    }

    override fun createEntity() {
        if (activePath == null) {
            return
        }
        val whatIfs = ArrayList<String>()
        if (elementAttributes[0] == getString(R.string.literal_what_if) && inputMap[elementAttributes[0]] == null && multiInputMap[getString(R.string.literal_what_if)] != null){
            val rawWhatIfs = multiInputMap[getString(R.string.literal_what_if)]
            for (view in rawWhatIfs!!){
                whatIfs.add(view.text.toString())
            }
        }
        if (editUnit != null){
            DatabaseHelper.getInstance(applicationContext).delete(editUnit!!.getElementId(),IElement::class)
            activePath?.remove(editUnit!!)
        }
        if ((!whatIfs.isEmpty() && editUnit is AbstractStep ) || //Add new What-Ifs
                (whatIfs.isEmpty() && inputMap[elementAttributes[0]] == null)){ //Clear What-Ifs
            DatabaseHelper.getInstance(applicationContext).write(editUnit!!.getElementId(),(editUnit as AbstractStep).withWhatIfs(whatIfs))
            return
        }
        val endPoint = if (editUnit != null) editUnit?.getPreviousElementId() else activePath?.getEndPoint()?.getElementId()

        when (creationUnitClass) {
            //Steps
            StandardStep::class -> {
                val title = inputMap[elementAttributes[0]]!!.getStringValue()
                val objects = activeScenario?.getObjectsWithNames((inputMap[elementAttributes[1]]!! as SreMultiAutoCompleteTextView).getUsedObjectLabels())
                val text = inputMap[elementAttributes[1]]!!.getStringValue()
                addAndRenderElement(StandardStep(editUnit?.getElementId(), endPoint, activePath!!.id).withTitle(title).withText(text).withObjects(objects!!).withWhatIfs((editUnit as AbstractStep?)?.whatIfs))
            }
            InputStep::class -> {/*TODO*/}
            LoopbackStep::class -> {/*TODO*/}
            MergeStep::class -> {/*TODO*/}
            ResourceStep::class -> {/*TODO*/}
            //Triggers
            ButtonTrigger::class -> {
                val buttonLabel = inputMap[elementAttributes[0]]!!.getStringValue()
                addAndRenderElement(ButtonTrigger(editUnit?.getElementId(), endPoint, activePath!!.id).withButtonLabel(buttonLabel))
            }
            IfElseTrigger::class -> {
                if (inputMap[elementAttributes[0]] == null) {
                    //Removal
                    val selection = multiInputMap[elementAttributes[0]]
                    if (!selection.isNullOrEmpty()) {
                        for (editText in selection) {
                            val option = (editUnit as IfElseTrigger).getOptionFromIndexedString(editText.getStringValue())
                            val layer = (editUnit as IfElseTrigger).removePathOption(option)
                            val removedPath = activeScenario?.removePath(activePath!!.stakeholder, layer)
                            if (removedPath != null) {
                                DatabaseHelper.getInstance(applicationContext).delete(removedPath.id, Path::class)
                            }
                        }
                        addAndRenderElement(editUnit!!)
                    }
                } else {
                    //Add Edit
                    val text = inputMap[elementAttributes[0]]!!.getStringValue()
                    val defaultOption = inputMap[elementAttributes[1]]!!.getStringValue()
                    val option1 = inputMap[elementAttributes[2]]?.getStringValue()
                    val option2 = inputMap[elementAttributes[3]]?.getStringValue()
                    val option3 = inputMap[elementAttributes[4]]?.getStringValue()
                    val option4 = inputMap[elementAttributes[5]]?.getStringValue()
                    val option5 = inputMap[elementAttributes[6]]?.getStringValue()
                    val newOptionCount = countNonNull(defaultOption, option1, option2, option3, option4, option5)
                    val element = IfElseTrigger(editUnit?.getElementId(), endPoint, activePath!!.id, text, defaultOption)
                            .addPathOption(defaultOption, activePath!!.layer, 0)
                    if (newOptionCount > 1) {
                        val oldOptionCount = (editUnit as IfElseTrigger).getOptionCount()
                        if (oldOptionCount < newOptionCount) {
                            val newPath = activeScenario?.getPath(activePath!!.stakeholder, applicationContext)
                            element.addPathOption(inputMap[elementAttributes[newOptionCount]]?.getStringValue(), newPath!!.layer, newOptionCount - 1)
                        }
                        element.addPathOption(option1, (editUnit as IfElseTrigger).getLayerForOption(1), 1)
                                .addPathOption(option2, (editUnit as IfElseTrigger).getLayerForOption(2), 2)
                                .addPathOption(option3, (editUnit as IfElseTrigger).getLayerForOption(3), 3)
                                .addPathOption(option4, (editUnit as IfElseTrigger).getLayerForOption(4), 4)
                                .addPathOption(option5, (editUnit as IfElseTrigger).getLayerForOption(5), 5)
                    }
                    addAndRenderElement(element)
                }
            }
            StakeholderInteractionTrigger::class -> {
                val text = inputMap[elementAttributes[0]]!!.getStringValue()
                val textView = inputMap[elementAttributes[1]]
                if (textView is SreButton){
                    val stakeholder = textView.getStringValue()
                    val stakeholderPos = (textView.data  as Int)-1
                    val stakeholderId = projectContext!!.stakeholders[stakeholderPos].id
                    addAndRenderElement(StakeholderInteractionTrigger(editUnit?.getElementId(), endPoint, activePath!!.id).withText(text).withInteractedStakeholderId(stakeholderId))
                }
            }
            InputTrigger::class -> {
                val text = inputMap[elementAttributes[0]]!!.getStringValue()
                val input = inputMap[elementAttributes[1]]!!.getStringValue()
                addAndRenderElement(InputTrigger(editUnit?.getElementId(), endPoint, activePath!!.id).withText(text).withInput(input))
            }
            TimeTrigger::class -> {/*TODO*/}
            SoundTrigger::class -> {/*TODO*/}
            BluetoothTrigger::class -> {/*TODO*/}
            GpsTrigger::class -> {/*TODO*/}
            MobileNetworkTrigger::class -> {/*TODO*/}
            NfcTrigger::class -> {/*TODO*/}
            WifiTrigger::class -> {/*TODO*/}
            CallTrigger::class -> {/*TODO*/}
            SmsTrigger::class -> {/*TODO*/}
            AccelerationTrigger::class -> {/*TODO*/}
            GyroscopeTrigger::class -> {/*TODO*/}
            LightTrigger::class -> {/*TODO*/}
            MagnetometerTrigger::class -> {/*TODO*/}
        }
        editUnit = null
        creationUnitClass = null
    }

    private fun connectPreviousToNext() {
        for (v in 0 until getContentHolderLayout().childCount) {
            if (getContentHolderLayout().getChildAt(v) is Element) {
                (getContentHolderLayout().getChildAt(v) as Element).connectToNext()
            }
        }
    }

    private fun colorizeZebraPattern() {
        for (v in 0 until getContentHolderLayout().childCount) {
            if (getContentHolderLayout().getChildAt(v) is Element) {
                (getContentHolderLayout().getChildAt(v) as Element).setZebraPattern(v%2==0)
            }
        }
    }

    override fun resetEditMode() {
        editor_spinner_selection?.visibility = View.VISIBLE
        creationButton?.visibility = View.VISIBLE
        editUnit = null
        inputMap.clear()
        multiInputMap.clear()
        creationUnitClass = null
        refreshState()
        getInfoTitle().textSize = DipHelper.get(resources).dip2_5.toFloat()
    }

    private fun updateSpinner(arrayResource: Int) {
        val spinnerArrayAdapter = ArrayAdapter<String>(this, R.layout.sre_spinner_item, resources.getStringArray(arrayResource))
        spinnerArrayAdapter.setDropDownViewResource(R.layout.sre_spinner_item)
        editor_spinner_selection.adapter = spinnerArrayAdapter
        editor_spinner_selection.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                creationButton?.setButtonIcons(R.string.icon_backward, R.string.icon_forward, if (pathList.isEmpty()) R.string.icon_null else R.string.icon_undo, R.string.icon_plus, null)
                        ?.setButtonStates(true, true, !pathList.isEmpty(), activePath != null && !(editor_spinner_selection.selectedItem as String).contains("["))?.updateViews(false)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                //NOP
            }
        };
    }

    override fun execAdaptToOrientationChange() {
        if (resources.configuration.orientation == ORIENTATION_LANDSCAPE) {
            contentDefaultMaxLines = 2
        } else if (resources.configuration.orientation == ORIENTATION_PORTRAIT) {
            contentDefaultMaxLines = 4
        }
    }

    override fun onBackPressed() {
        if (isInputOpen()){
            onToolbarRightClicked()
        }else{
            super.onBackPressed()
        }
    }

    override fun onToolbarCenterRightClicked() {
        if (!isInputOpen()) {
            val intent = Intent(this, GlossaryActivity::class.java)
            intent.putExtra(Constants.BUNDLE_GLOSSARY_TOPIC, "Editor")
            intent.putExtra(Constants.BUNDLE_GLOSSARY_ADDITIONAL_TOPICS, arrayOf("Step","Trigger"))
            startActivity(intent)
        }
    }
}