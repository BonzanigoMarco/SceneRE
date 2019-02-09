package uzh.scenere.activities

import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import kotlinx.android.synthetic.main.activity_editor.*
import kotlinx.android.synthetic.main.scroll_holder.*
import uzh.scenere.R
import uzh.scenere.activities.EditorActivity.EditorState.ADD
import uzh.scenere.activities.EditorActivity.EditorState.EDIT
import uzh.scenere.const.Constants
import uzh.scenere.datamodel.*
import uzh.scenere.datamodel.steps.StandardStep
import uzh.scenere.datamodel.trigger.AbstractTrigger
import uzh.scenere.datamodel.trigger.direct.ButtonTrigger
import uzh.scenere.helpers.DatabaseHelper
import uzh.scenere.helpers.StringHelper
import uzh.scenere.helpers.getStringValue
import uzh.scenere.helpers.readableClassName
import uzh.scenere.views.Element
import uzh.scenere.views.SreMultiAutoCompleteTextView
import uzh.scenere.views.SwipeButton
import uzh.scenere.views.SwipeButtonScrollView
import java.util.*
import kotlin.collections.HashMap
import kotlin.reflect.KClass


class EditorActivity : AbstractManagementActivity() {
    override fun isInEditMode(): Boolean {
        return (editorState == ADD || editorState == EDIT)
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activeScenario = intent.getSerializableExtra(Constants.BUNDLE_SCENARIO) as Scenario?
        if (activeScenario != null) {
            projectContext = DatabaseHelper.getInstance(applicationContext).readFull(activeScenario!!.projectId, Project::class)
            activeScenario = DatabaseHelper.getInstance(applicationContext).readFull(activeScenario!!.id, Scenario::class)
        } else {
            //DEV
            projectContext = DatabaseHelper.getInstance(applicationContext).readFull("97a35810-27b2-4917-9346-196f9fb18f7a", Project::class)
            activeScenario = DatabaseHelper.getInstance(applicationContext).readFull("558856ba-3074-4725-9ffc-b03677df77d0", Scenario::class)
        }
        var stakeholder: Stakeholder? = null
        if (projectContext != null && !projectContext!!.stakeholders.isNullOrEmpty()) {
            stakeholder = projectContext?.getNextStakeholder()
            activePath = activeScenario?.getPath(stakeholder!!, applicationContext)
        } else {
            //TODO Warn that there
        }

        scroll_holder_scroll.setBackgroundColor(Color.WHITE)
        populateExplanationMap()
        execAdaptToOrientationChange()
        //TODO Toolbar

        refreshState()

        creationButton = SwipeButton(this, stakeholder?.name
                ?: "No Stakeholders, Path cannot be built.")
                .setColors(Color.WHITE, Color.GRAY)
                .setButtonMode(SwipeButton.SwipeButtonMode.QUADRUPLE)
                .setButtonIcons(R.string.icon_backward, R.string.icon_forward, R.string.icon_null, R.string.icon_plus, null)
                .setButtonStates(true, true, false, false)
                .adaptMasterLayoutParams(true)
                .setAutoCollapse(true)
                .updateViews(true)
        creationButton?.setExecutable(createControlExecutable())
        editor_linear_layout_control.addView(creationButton)

        getInfoTitle().text = StringHelper.styleString(getSpannedStringFromId(getConfiguredInfoString()), fontAwesome)
        customizeToolbarText(resources.getText(R.string.icon_back).toString(), null, null, null, null)
        visualizeActivePath()
        scroll_holder_linear_layout_holder.setOnHierarchyChangeListener(object : ViewGroup.OnHierarchyChangeListener {
            override fun onChildViewRemoved(parent: View?, child: View?) {
                refreshState(child)
            }

            override fun onChildViewAdded(parent: View?, child: View?) {
                //NOP
            }

        })
    }

    private fun visualizeActivePath() {
        if (activePath != null) {
            scroll_holder_linear_layout_holder.removeAllViews()
            var element = activePath?.getStartingPoint()
            while (element != null) {
                renderElement(element)
                element = activePath?.getNextElement(element.getElementId())
            }
        }
        refreshState()
    }

    private fun addAndRenderElement(element: IElement, edit: Boolean = (editorState==EDIT)) {
        activePath?.add(element)
        DatabaseHelper.getInstance(applicationContext).write(element.getElementId(), element)
        if (edit){
            updateRenderedElement(element)
        }else{
            renderElement(element)
        }
    }

    private fun renderElement(iElement: IElement) {
        var title: String? = null
        if (iElement is StandardStep) title = "<b>" + iElement.readableClassName() + "</b><br>" + iElement.title
        if (iElement is AbstractTrigger) title = "<b>" + iElement.readableClassName() + "</b>"
        val previousAvailable = scroll_holder_linear_layout_holder.childCount != 0
        if (previousAvailable) {
            connectPreviousToNext()
        }
        val element = Element(applicationContext, iElement, previousAvailable, false, false, false).withLabel(StringHelper.fromHtml(title))
        element.setEditExecutable { openInput(iElement) }
        element.setDeleteExecutable {
            DatabaseHelper.getInstance(applicationContext).delete(iElement.getElementId(),IElement::class)
            val prevPosition = scroll_holder_linear_layout_holder.indexOfChild(element)-1
            if (prevPosition >= 0){
                (scroll_holder_linear_layout_holder.getChildAt(prevPosition) as Element).disconnectFromNext()
            }
            scroll_holder_linear_layout_holder.removeView(element)
            activePath?.remove(iElement)
        }
        scroll_holder_linear_layout_holder.addView(element)
    }

    private fun updateRenderedElement(iElement: IElement) {
        var title: String? = null
        if (iElement is StandardStep) title = "<b>" + iElement.readableClassName() + "</b><br>" + iElement.title
        if (iElement is AbstractTrigger) title = "<b>" + iElement.readableClassName() + "</b>"
        for (v in 0 until scroll_holder_linear_layout_holder.childCount){
            if (scroll_holder_linear_layout_holder.getChildAt(v) is Element && (scroll_holder_linear_layout_holder.getChildAt(v) as Element).containsElement(iElement)){
                (scroll_holder_linear_layout_holder.getChildAt(v) as Element).withLabel(StringHelper.fromHtml(title)).updateElement(iElement).setEditExecutable { openInput(iElement) }
            }
        }
    }

    private fun refreshState(view: View? = null) {
        if (view != null && view is Element){
            editorState = if ((view as Element).isStep()) EditorState.STEP else EditorState.TRIGGER
        }else{
            for (v in 0 until scroll_holder_linear_layout_holder.childCount) {
                if (scroll_holder_linear_layout_holder.getChildAt(v) is Element) {
                    editorState = if ((scroll_holder_linear_layout_holder.getChildAt(v) as Element).isStep()) EditorState.TRIGGER else EditorState.STEP
                }
            }
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
                    activePath = activeScenario?.getPath(stakeholder, applicationContext)
                    creationButton?.setText(stakeholder.name)
                    visualizeActivePath()
                }
            }

            override fun execRight() {
                val stakeholder: Stakeholder? = projectContext?.getNextStakeholder(activePath?.stakeholder)
                if (stakeholder != null) {
                    activePath = activeScenario?.getPath(stakeholder, applicationContext)
                    creationButton?.setText(stakeholder.name)
                    visualizeActivePath()
                }
            }

            override fun execDown() {
                super.execDown()
                openInput()
            }
        }
    }

    private fun openInput(element: IElement? = null) {
        editorState = if (element == null) ADD else EDIT
        editor_spinner_selection?.visibility = View.GONE
        creationButton?.visibility = View.GONE
        if (element != null) {//LOAD
            cleanInfoHolder("Edit " + element.readableClassName())
            editUnit = element
            when (element) {
                is StandardStep -> {
                    creationUnitClass = StandardStep::class
                    adaptAttributes("Title", "Text")
                    scroll_holder_text_info_content_wrap.addView(createLine(elementAttributes[0], LineInputType.SINGLE_LINE_EDIT, element.title))
                    scroll_holder_text_info_content_wrap.addView(createLine(elementAttributes[1], LineInputType.MULTI_LINE_CONTEXT_EDIT, element.text))
                    (inputMap[elementAttributes[1]] as SreMultiAutoCompleteTextView).setObjects(activeScenario?.objects!!)
                    execMorphInfoBar(InfoState.MAXIMIZED)
                }
                is ButtonTrigger -> {
                    creationUnitClass = ButtonTrigger::class
                    adaptAttributes("Button-Label")
                    scroll_holder_text_info_content_wrap.addView(createLine(elementAttributes[0], LineInputType.SINGLE_LINE_EDIT, element.buttonLabel))
                    execMorphInfoBar(InfoState.MAXIMIZED)
                }
            }
        } else {//CREATE
            when ((editor_spinner_selection.selectedItem as String)) {
                resources.getString(R.string.step_standard) -> {
                    creationUnitClass = StandardStep::class
                    cleanInfoHolder("Add " + editor_spinner_selection.selectedItem)
                    adaptAttributes("Title", "Text")
                    scroll_holder_text_info_content_wrap.addView(createLine(elementAttributes[0], LineInputType.SINGLE_LINE_EDIT, null))
                    scroll_holder_text_info_content_wrap.addView(createLine(elementAttributes[1], LineInputType.MULTI_LINE_CONTEXT_EDIT, null))
                    (inputMap[elementAttributes[1]] as SreMultiAutoCompleteTextView).setObjects(activeScenario?.objects!!)
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
    }

    private fun adaptAttributes(vararg attributeNames: String) {
        for (i in elementAttributes.indices) {
            elementAttributes[i] = ""
        }
        for (name in attributeNames.indices) {
            if (name > elementAttributes.size - 1) {
                return
            }
            elementAttributes[name] = attributeNames[name]
        }
    }

    override fun createEntity() {
        if (activePath == null) {
            return
        }
        if (editUnit != null){
            DatabaseHelper.getInstance(applicationContext).delete(editUnit!!.getElementId(),IElement::class)
            activePath?.remove(editUnit!!)
        }
        val endPoint = if (editUnit != null) editUnit?.getPreviousElementId() else activePath?.getEndPoint()?.getElementId()
        when (creationUnitClass) {
            //Steps
            StandardStep::class -> {
                val title = inputMap[elementAttributes[0]]!!.getStringValue()
                val objects = activeScenario?.getObjectsWithNames((inputMap[elementAttributes[1]]!! as SreMultiAutoCompleteTextView).getUsedObjectLabels())
                val text = inputMap[elementAttributes[1]]!!.getStringValue()
                addAndRenderElement(StandardStep(editUnit?.getElementId(), endPoint, activePath!!.id).withTitle(title).withText(text).withObjects(objects!!))
            }
            //Triggers
            ButtonTrigger::class -> {
                val buttonLabel = inputMap[elementAttributes[0]]!!.getStringValue()
                addAndRenderElement(ButtonTrigger(editUnit?.getElementId(), endPoint, activePath!!.id).withButtonLabel(buttonLabel))
            }
        }
        editUnit = null
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
        editUnit = null
        inputMap.clear()
        multiInputMap.clear()
        creationUnitClass = null
        refreshState()
    }

    fun onToolSelectionClicked(v: View) {
        when (v.id) {
//            R.id.editor_button_steps -> {updateSpinner(R.array.spinner_steps)}
//            R.id.editor_button_triggers_communication -> {updateSpinner(R.array.spinner_triggers_communication)}
//            R.id.editor_button_triggers_direct -> {updateSpinner(R.array.spinner_triggers_direct)}
//            R.id.editor_button_triggers_indirect -> {updateSpinner(R.array.spinner_triggers_indirect)}
//            R.id.editor_button_triggers_sensor -> {updateSpinner(R.array.spinner_triggers_sensor)}
            else -> {
            }
        }
    }

    private fun updateSpinner(arrayResource: Int) {
//        showInformation(explanationMap[arrayResource]?.key,explanationMap[arrayResource]?.value)
        val spinnerArrayAdapter = ArrayAdapter<String>(this, R.layout.sre_spinner_item, resources.getStringArray(arrayResource))
        spinnerArrayAdapter.setDropDownViewResource(R.layout.sre_spinner_item)
        editor_spinner_selection.adapter = spinnerArrayAdapter
        editor_spinner_selection.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                creationButton?.setButtonStates(true, true, false, activePath != null && !(editor_spinner_selection.selectedItem as String).contains("["))?.updateViews(false)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                //NOP
            }
        };
    }

    private var handlerId = 0L
    private fun showInformation(titleId: Int?, contentId: Int?) {
        if (titleId == null || contentId == null) {
            return
        }
        execMorphInfoBar(InfoState.NORMAL)
        scroll_holder_text_info_title.text = resources.getString(titleId)
        scroll_holder_text_info_content.text = resources.getString(contentId)
        val localHandlerId = Random().nextLong()
        handlerId = localHandlerId
        Handler().postDelayed({
            if (localHandlerId == handlerId) {
                scroll_holder_text_info_title.text = null
                scroll_holder_text_info_content.text = null
                execMorphInfoBar(InfoState.MINIMIZED)
            }
        }, 5000)
    }

    override fun execAdaptToOrientationChange() {
        if (resources.configuration.orientation == ORIENTATION_LANDSCAPE) {
            contentDefaultMaxLines = 2
        } else if (resources.configuration.orientation == ORIENTATION_PORTRAIT) {
            contentDefaultMaxLines = 4
        }
    }

    override fun resetToolbar() {
        customizeToolbarText(resources.getText(R.string.icon_back).toString(), null, null, null, null)
    }

    override fun execFullScroll(){
        if (editorState == ADD){ //Avoid Scrolls on Edit
            (getContentWrapperLayout() as SwipeButtonScrollView).fullScroll(View.FOCUS_DOWN)
        }
    }
}