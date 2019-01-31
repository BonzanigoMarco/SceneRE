package uzh.scenere.activities

import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import kotlinx.android.synthetic.main.activity_editor.*
import kotlinx.android.synthetic.main.scroll_holder.*
import uzh.scenere.R
import uzh.scenere.const.Constants
import uzh.scenere.datamodel.*
import uzh.scenere.datamodel.steps.AbstractStep
import uzh.scenere.datamodel.steps.StandardStep
import uzh.scenere.datamodel.trigger.AbstractTrigger
import uzh.scenere.datamodel.trigger.direct.ButtonTrigger
import uzh.scenere.helpers.DatabaseHelper
import uzh.scenere.helpers.StringHelper
import uzh.scenere.helpers.getStringValue
import uzh.scenere.views.Element
import uzh.scenere.views.Element.ElementMode.STEP
import uzh.scenere.views.Element.ElementMode.TRIGGER
import uzh.scenere.views.SwipeButton
import java.util.*
import kotlin.collections.HashMap
import kotlin.reflect.KClass


class EditorActivity : AbstractManagementActivity() {
    override fun isInEditMode(): Boolean {
        return editorState == EditorState.ADD
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

    override fun isSpacingEnabled(): Boolean {
        return false
    }

    private val explanationMap: HashMap<Int,Map.Entry<Int,Int>> = HashMap<Int,Map.Entry<Int,Int>>()
    enum class EditorState{
        STEP, TRIGGER, INIT, ADD
    }
    private var editorState: EditorState = EditorState.INIT
    private val elementAttributes: Array<String> = arrayOf("","","","","","","","","","")
    private var creationUnitClass: KClass<out IElement>? = null
    private var creationUnit: IElement? = null
    //Context
    private var activeScenario: Scenario? = null
    private var projectContext: Project? = null
    private var activePath: Path? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activeScenario = intent.getSerializableExtra(Constants.BUNDLE_SCENARIO) as Scenario?
        if (activeScenario != null){
            projectContext = DatabaseHelper.getInstance(applicationContext).readFull(activeScenario!!.projectId,Project::class)
            activeScenario = DatabaseHelper.getInstance(applicationContext).readFull(activeScenario!!.id,Scenario::class)
        }else{
            //DEV
            projectContext = DatabaseHelper.getInstance(applicationContext).readFull("ce482ccd-ece3-4184-9a3a-c47470f5ed35",Project::class  )
            activeScenario = DatabaseHelper.getInstance(applicationContext).readFull("bdeb277a-7a63-4390-8bac-7df589dc1e53",Scenario::class)
        }
        //TODO> FOR NOW, LOAD ALL STAKEHOLDERS OF THE PROJECT, LATER, LET SELECT
        var stakeholder: Stakeholder? = null
        if (projectContext != null && !projectContext!!.stakeholders.isNullOrEmpty()){
            stakeholder = projectContext?.getNextStakeholder()
            activePath = activeScenario?.getPath(stakeholder!!, applicationContext)
        }else{
            //TODO Warn that there
        }

        scroll_holder_scroll.setBackgroundColor(Color.WHITE)
        populateExplanationMap()
        execAdaptToOrientationChange()
        //TODO Toolbar

        refreshState()

        creationButton = SwipeButton(this, stakeholder?.name ?: "Warning: No Stakeholders in this Project. Path cannot be built.")
                .setColors(Color.WHITE, Color.GRAY)
                .setButtonMode(SwipeButton.SwipeButtonMode.QUADRUPLE)
                .setButtonIcons(R.string.icon_backward, R.string.icon_forward, R.string.icon_null, R.string.icon_plus, null)
                .setButtonStates(true, true, false, false)
                .adaptMasterLayoutParams(true)
                .updateViews(true)
        creationButton?.setExecutable(createControlExecutable())
        editor_linear_layout_control.addView(creationButton)

        getInfoTitle().text = StringHelper.styleString(getSpannedStringFromId(getConfiguredInfoString()), fontAwesome)
        customizeToolbarText(resources.getText(R.string.icon_back).toString(), null, null, null, null)
        visualizeActivePath()
        refreshState()
    }

    private fun visualizeActivePath() {
        if (activePath != null){
            scroll_holder_linear_layout_holder.removeAllViews()
            var element = activePath?.getStartingPoint()
            while (element!= null){
                renderElement(element)
                element = activePath?.getNextElement(element.getElementId())
            }
        }
    }

    private fun addAndRenderElement(element: IElement){
        activePath?.add(element)
        DatabaseHelper.getInstance(applicationContext).write(element.getElementId(),element)
        renderElement(element)
    }

    private fun renderElement(iElement: IElement){
        var title: String? = null
        if (iElement is StandardStep) title = iElement.title
        if (iElement is ButtonTrigger) title = "Button-Trigger"
        val previousAvailable = scroll_holder_linear_layout_holder.childCount != 0
        if (previousAvailable) {
            connectPreviousToNext()
        }
        if (iElement is AbstractTrigger){
            scroll_holder_linear_layout_holder.addView(TRIGGER.create(title, previousAvailable, false, false, false, applicationContext))
        }else if (iElement is AbstractStep){
            scroll_holder_linear_layout_holder.addView(STEP.create(title, previousAvailable, false, false, false, applicationContext))
        }
    }

    private fun refreshState() {
        for (v in 0 until scroll_holder_linear_layout_holder.childCount){
            if (scroll_holder_linear_layout_holder.getChildAt(v) is Element){
                editorState = if ((scroll_holder_linear_layout_holder.getChildAt(v) as Element).getElementMode() == STEP) EditorState.TRIGGER else EditorState.STEP
            }
        }
        when (editorState){
            EditorState.INIT -> {
                updateSpinner(R.array.spinner_steps)
                editorState = EditorState.STEP}
            EditorState.STEP -> updateSpinner(R.array.spinner_steps)
            EditorState.TRIGGER -> updateSpinner(R.array.spinner_triggers)
            else -> return
        }
    }

    private fun populateExplanationMap() {
        explanationMap[R.array.spinner_steps] = AbstractMap.SimpleEntry(R.string.explanation_steps_title,R.string.explanation_steps_content)
        explanationMap[R.string.step_standard] = AbstractMap.SimpleEntry(R.string.step_standard,R.string.explanation_step_standard)
    }

    private fun createControlExecutable(): SwipeButton.SwipeButtonExecution {
        return object: SwipeButton.SwipeButtonExecution{
            override fun execLeft() {
                val stakeholder: Stakeholder? = projectContext?.getPreviousStakeholder(activePath?.stakeholder)
                if (stakeholder != null){
                    activePath = activeScenario?.getPath(stakeholder, applicationContext)
                    creationButton?.setText(stakeholder.name)
                    Handler().postDelayed({ creationButton?.collapse() }, 250)
                    visualizeActivePath()
                }
            }
            override fun execRight() {
                val stakeholder: Stakeholder? = projectContext?.getNextStakeholder(activePath?.stakeholder)
                if (stakeholder != null){
                    activePath = activeScenario?.getPath(stakeholder, applicationContext)
                    creationButton?.setText(stakeholder.name)
                    Handler().postDelayed({ creationButton?.collapse() }, 250)
                    visualizeActivePath()
                }
            }
            override fun execDown() {
                super.execDown()
                openInput()
            }
        }
    }

    private fun openInput() {
        editorState = EditorState.ADD
        editor_spinner_selection?.visibility = View.GONE
        creationButton?.visibility = View.GONE
        when ((editor_spinner_selection.selectedItem as String)) {
            resources.getString(R.string.step_standard) -> {
                creationUnitClass = StandardStep::class
                cleanInfoHolder("Add " + editor_spinner_selection.selectedItem)
                adaptAttributes("Title", "Object","Text")
                scroll_holder_text_info_content_wrap.addView(createLine(elementAttributes[0], LineInputType.SINGLE_LINE_EDIT, null))
                scroll_holder_text_info_content_wrap.addView(createLine(elementAttributes[1], LineInputType.LOOKUP, null, activeScenario?.getObjectNames("")/*arrayOf("","Object A","Object B","Object C")*/))
                scroll_holder_text_info_content_wrap.addView(createLine(elementAttributes[2], LineInputType.MULTI_LINE_EDIT, null))
                execMorphInfoBar(InfoState.MAXIMIZED)
            }
            resources.getString(R.string.trigger_button) -> {
                creationUnitClass = ButtonTrigger::class
                cleanInfoHolder("Add " + editor_spinner_selection.selectedItem)
                adaptAttributes("Button-Label")
                scroll_holder_text_info_content_wrap.addView(createLine(elementAttributes[0], LineInputType.SINGLE_LINE_EDIT, null))
                execMorphInfoBar(InfoState.MAXIMIZED)
            }
        }
    }

    private fun adaptAttributes(vararg attributeNames: String){
        for (i in elementAttributes.indices){
            elementAttributes[i] = ""
        }
        for (name in attributeNames.indices){
            if (name > elementAttributes.size-1){
                return
            }
            elementAttributes[name] = attributeNames[name]
        }
    }

    override fun createEntity() {
        if (activePath == null){
            return
        }
        when (creationUnitClass){
            //Steps
            StandardStep::class -> {
                val title = inputMap[elementAttributes[0]]!!.getStringValue()
                val objects = projectContext?.getObjectsWithNames(getTextsFromLookupChoice(elementAttributes[1]))
                val text = inputMap[elementAttributes[2]]!!.getStringValue()
                val endPoint = activePath?.getEndPoint()
                addAndRenderElement(StandardStep(null,endPoint?.getElementId(),activePath!!.id).withTitle(title).withText(text).withObjects(objects!!))
            }
            //Triggers
            ButtonTrigger::class -> {
                val buttonLabel = inputMap[elementAttributes[0]]!!.getStringValue()
                val endPoint = activePath?.getEndPoint()
                addAndRenderElement(ButtonTrigger(null,endPoint?.getElementId(),activePath!!.id).withButtonLabel(buttonLabel))
            }
        }
        creationUnitClass = null
    }

    private fun connectPreviousToNext() {
        for (v in 0 until scroll_holder_linear_layout_holder.childCount) {
            if (scroll_holder_linear_layout_holder.getChildAt(v) is Element) {
                (scroll_holder_linear_layout_holder.getChildAt(v) as Element).connectToNext()
            }
        }
    }

    override fun resetEditMode() {
        editor_spinner_selection?.visibility = View.VISIBLE
        creationButton?.visibility = View.VISIBLE
        Handler().postDelayed({ creationButton?.collapse() }, 250)
        refreshState()
    }

    fun onToolSelectionClicked(v: View) {
        when(v.id){
//            R.id.editor_button_steps -> {updateSpinner(R.array.spinner_steps)}
//            R.id.editor_button_triggers_communication -> {updateSpinner(R.array.spinner_triggers_communication)}
//            R.id.editor_button_triggers_direct -> {updateSpinner(R.array.spinner_triggers_direct)}
//            R.id.editor_button_triggers_indirect -> {updateSpinner(R.array.spinner_triggers_indirect)}
//            R.id.editor_button_triggers_sensor -> {updateSpinner(R.array.spinner_triggers_sensor)}
            else -> {}
        }
    }

    fun onAddButtonClicked(v: View){
        //Do magic
    }

    private fun updateSpinner(arrayResource: Int) {
//        showInformation(explanationMap[arrayResource]?.key,explanationMap[arrayResource]?.value)
        val spinnerArrayAdapter = ArrayAdapter<String>(this, R.layout.sre_spinner_item, resources.getStringArray(arrayResource))
        spinnerArrayAdapter.setDropDownViewResource(R.layout.sre_spinner_item)
        editor_spinner_selection.adapter = spinnerArrayAdapter
        editor_spinner_selection.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                creationButton?.setButtonStates(true,true,false, !(editor_spinner_selection.selectedItem as String).contains("["))?.updateViews(false)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                //NOP
            }
        };
    }

    private var handlerId = 0L
    private fun showInformation(titleId: Int?, contentId: Int?){
        if (titleId == null || contentId == null){
            return
        }
        execMorphInfoBar(InfoState.NORMAL)
        scroll_holder_text_info_title.text = resources.getString(titleId)
        scroll_holder_text_info_content.text = resources.getString(contentId)
        val localHandlerId = Random().nextLong()
        handlerId = localHandlerId
        Handler().postDelayed({
            if (localHandlerId == handlerId){
                scroll_holder_text_info_title.text = null
                scroll_holder_text_info_content.text = null
                execMorphInfoBar(InfoState.MINIMIZED)
            }
        }, 5000)
    }

    override fun execAdaptToOrientationChange() {
        if (resources.configuration.orientation == ORIENTATION_LANDSCAPE){
            contentDefaultMaxLines = 2
        }else if (resources.configuration.orientation == ORIENTATION_PORTRAIT){
            contentDefaultMaxLines = 4
        }
    }
}