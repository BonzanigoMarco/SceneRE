package uzh.scenere.activities

import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_walkthrough.*
import kotlinx.android.synthetic.main.holder.*
import uzh.scenere.R
import uzh.scenere.datamodel.*
import uzh.scenere.helpers.DatabaseHelper
import uzh.scenere.helpers.StringHelper
import uzh.scenere.views.SreTutorialLayoutDialog
import uzh.scenere.views.SwipeButton
import uzh.scenere.views.WalkthroughPlayLayout
import java.io.Serializable

class WalkthroughActivity : AbstractManagementActivity() {

    override fun isInEditMode(): Boolean {
        return mode == WalkthroughMode.INFO
    }

    override fun isInAddMode(): Boolean {
        return mode == WalkthroughMode.INFO
    }

    override fun isInViewMode(): Boolean {
        return mode != WalkthroughMode.INFO
    }

    override fun resetEditMode() {
        mode = WalkthroughMode.PLAY
        getInfoContentWrap().removeView(objectInfoSpinnerLayout)
        getInfoContentWrap().removeView(attributeInfoSpinnerLayout)
        getInfoContentWrap().removeView(selectedAttributeInfoLayout)
        objectInfoSpinnerLayout = null
        attributeInfoSpinnerLayout = null
        selectedAttributeInfoLayout = null
        selectedObject = null
        selectedAttribute = null
        getInfoContent().visibility = VISIBLE
        customizeToolbarId(null,null,R.string.icon_info,null,null)
        activeWalkthrough?.setInfoActive(false)
    }

    override fun createEntity() {
        //NOP
    }

    override fun getConfiguredInfoString(): Int {
        return R.string.icon_explain_walkthrough
    }

    override fun getConfiguredLayout(): Int {
        return R.layout.activity_walkthrough
    }

    override fun getContentHolderLayout(): ViewGroup {
        return holder_linear_layout_holder
    }

    override fun getContentWrapperLayout(): ViewGroup {
        return holder_linear_layout_holder
    }

    override fun getInfoWrapper(): LinearLayout {
        return holder_layout_info
    }

    override fun getInfoTitle(): TextView {
        return if (mode == WalkthroughMode.PLAY) holder_text_info_title else walkthrough_text_selection_info
    }

    override fun getInfoContentWrap(): LinearLayout {
        return holder_text_info_content_wrap
    }

    override fun getInfoContent(): TextView {
        return holder_text_info_content
    }

    enum class WalkthroughMode {
        SELECT_PROJECT, SELECT_SCENARIO, SELECT_STAKEHOLDER, PLAY, INFO
    }

    private var mode: WalkthroughMode = WalkthroughMode.SELECT_PROJECT
    private val loadedProjects = ArrayList<Project>()
    private val loadedScenarios = ArrayList<Scenario>()
    private val loadedStakeholders = ArrayList<Stakeholder>()
    private var pointer: Int? = null
    private var projectPointer: Int? = null
    private var scenarioPointer: Int? = null
    private var scenarioContext: Scenario? = null
    //Play
    private var activeWalkthrough: WalkthroughPlayLayout? = null
    //Info
    private var objectInfoSpinnerLayout: View? = null
    private var attributeInfoSpinnerLayout: View? = null
    private var selectedAttributeInfoLayout: View? = null
    private var selectedObject: AbstractObject? = null
    private var selectedAttribute: Attribute? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
        getInfoTitle().text = StringHelper.styleString(getSpannedStringFromId(getConfiguredInfoString()), fontAwesome)
        loadedProjects.clear()
        loadedProjects.addAll(DatabaseHelper.getInstance(applicationContext).readBulk(Project::class, null))
        creationButton = SwipeButton(this, createButtonLabel(loadedProjects, getString(R.string.literal_projects)))
                .setColors(Color.WHITE, Color.GRAY)
                .setButtonMode(SwipeButton.SwipeButtonMode.QUADRUPLE)
                .setButtonIcons(R.string.icon_backward, R.string.icon_forward, R.string.icon_undo, R.string.icon_check, null)
                .setButtonStates(!loadedProjects.isEmpty(), !loadedProjects.isEmpty(), false, false)
                .adaptMasterLayoutParams(true)
                .setFirstPosition()
                .setAutoCollapse(true)
                .updateViews(true)
        creationButton?.setExecutable(createControlExecutable())
        walkthrough_layout_selection_content.addView(creationButton)
        customizeToolbarId(R.string.icon_back,null,null,null,null)
        tutorialOpen = SreTutorialLayoutDialog(this,screenWidth,"info_walkthrough").addEndExecutable { tutorialOpen = false }.show(tutorialOpen)
    }

    private fun <T : Serializable> createButtonLabel(selectedList: ArrayList<T>, label: String): String {
        if (selectedList.isEmpty()) {
            return getString(R.string.walkthrough_button_label_failure, label)
        }
        return getString(R.string.walkthrough_button_label, selectedList.size, label)
    }

    private fun createControlExecutable(): SwipeButton.SwipeButtonExecution {
        return object : SwipeButton.SwipeButtonExecution {
            override fun execLeft() {
                execPrev()
            }

            override fun execRight() {
                execNext()
            }

            override fun execDown() {
                execSelect()
            }

            override fun execUp() {
                execBack()
            }
        }
    }

    private fun execNext() {
        when (mode) {
            WalkthroughMode.SELECT_PROJECT -> select(loadedProjects, true)
            WalkthroughMode.SELECT_SCENARIO -> select(loadedScenarios, true)
            WalkthroughMode.SELECT_STAKEHOLDER -> select(loadedStakeholders, true)
            else -> return
        }
    }

    private fun execPrev() {
        when (mode) {
            WalkthroughMode.SELECT_PROJECT -> select(loadedProjects, false)
            WalkthroughMode.SELECT_SCENARIO -> select(loadedScenarios, false)
            WalkthroughMode.SELECT_STAKEHOLDER -> select(loadedStakeholders, false)
            else -> return
        }
    }

    private fun execSelect() {
        when (mode) {
            WalkthroughMode.SELECT_PROJECT -> {
                mode = WalkthroughMode.SELECT_SCENARIO
                loadedScenarios.clear()
                loadedScenarios.addAll(DatabaseHelper.getInstance(applicationContext).readBulk(Scenario::class, loadedProjects[pointer!!]))
                projectPointer = pointer
                pointer = null
                walkthrough_text_selected_project.text = loadedProjects[projectPointer!!].title
                creationButton?.setButtonStates(!loadedScenarios.isEmpty(), !loadedScenarios.isEmpty(), true, false)?.setText(createButtonLabel(loadedScenarios, "Scenarios"))?.updateViews(false)
            }
            WalkthroughMode.SELECT_SCENARIO ->  {
                mode = WalkthroughMode.SELECT_STAKEHOLDER
                loadedStakeholders.clear()
                scenarioContext = DatabaseHelper.getInstance(applicationContext).readFull(loadedScenarios[pointer!!].id,Scenario::class)
                scenarioPointer = pointer
                pointer = null
                val stakeholders = DatabaseHelper.getInstance(applicationContext).readBulk(Stakeholder::class, loadedProjects[projectPointer!!])
                for (stakeholder in stakeholders){
                    if (scenarioContext!!.hasStakeholderPath(stakeholder)){
                        loadedStakeholders.add(stakeholder)
                    }
                }
                walkthrough_text_selected_scenario.text = loadedScenarios[scenarioPointer!!].title
                creationButton?.setButtonStates(!loadedStakeholders.isEmpty(), !loadedStakeholders.isEmpty(), true, false)
                        ?.setButtonIcons(R.string.icon_backward, R.string.icon_forward, R.string.icon_undo, R.string.icon_play, null)
                        ?.setText(createButtonLabel(loadedStakeholders, getString(R.string.literal_stakeholders)))
                        ?.updateViews(false)
            }
            WalkthroughMode.SELECT_STAKEHOLDER -> play()
            else -> return
        }
    }

    /**
     * next = false --> previous
     */
    private fun <T : Serializable> select(selectedList: ArrayList<T>, next: Boolean) {
        if (selectedList.isEmpty()) {
            return
        }
        if (next) {
            if (pointer != null && selectedList.size > (pointer!! + 1)) {
                pointer = pointer!! + 1
            } else {
                pointer = 0
            }
        } else {
            if (pointer != null && 0 <= (pointer!! - 1)) {
                pointer = pointer!! - 1
            } else {
                pointer = (selectedList.size - 1)
            }
        }

        when (selectedList[pointer!!]) {
            is Project -> creationButton?.setButtonStates(true, true, false, true)?.setText((selectedList[pointer!!] as Project).title)?.updateViews(false)
            is Scenario -> creationButton?.setButtonStates(true, true, true, true)?.setText((selectedList[pointer!!] as Scenario).title)?.updateViews(false)
            is Stakeholder -> {
                val hasPath = scenarioContext!!.hasStakeholderPath((selectedList[pointer!!] as Stakeholder))
                creationButton?.setButtonStates(true, true, true, hasPath)?.setText((selectedList[pointer!!] as Stakeholder).name)?.updateViews(false)
            }
        }
    }

    private fun execBack() {
        when (mode) {
            WalkthroughMode.SELECT_SCENARIO -> {
                mode = WalkthroughMode.SELECT_PROJECT
                loadedProjects.clear()
                loadedProjects.addAll(DatabaseHelper.getInstance(applicationContext).readBulk(Project::class, null))
                pointer = null
                projectPointer = null
                walkthrough_text_selected_project.text = null
                creationButton?.setButtonStates(!loadedProjects.isEmpty(), !loadedProjects.isEmpty(), false, false)?.setText(createButtonLabel(loadedProjects, getString(R.string.literal_projects)))?.updateViews(false)
            }
            WalkthroughMode.SELECT_STAKEHOLDER -> {
                mode = WalkthroughMode.SELECT_SCENARIO
                loadedScenarios.clear()
                loadedScenarios.addAll(DatabaseHelper.getInstance(applicationContext).readBulk(Scenario::class, loadedProjects[projectPointer!!]))
                scenarioContext = null
                pointer = null
                scenarioPointer = null
                walkthrough_text_selected_scenario.text = null
                creationButton?.setButtonStates(!loadedScenarios.isEmpty(), !loadedScenarios.isEmpty(), true, false)
                        ?.setButtonIcons(R.string.icon_backward, R.string.icon_forward, R.string.icon_undo, R.string.icon_check, null)
                        ?.setText(createButtonLabel(loadedScenarios, getString(R.string.literal_scenarios)))
                        ?.updateViews(false)
            }
            else -> return
        }
    }

    private fun play() {
        mode = WalkthroughMode.PLAY
        customizeToolbarId(null,null,R.string.icon_info,null,null)
        walkthrough_layout_selection_content.visibility = GONE
        walkthrough_layout_selection.visibility = GONE
        walkthrough_holder.visibility = VISIBLE
        activeWalkthrough = WalkthroughPlayLayout(applicationContext, scenarioContext!!, loadedStakeholders[pointer!!]) {stop()}
        getContentHolderLayout().addView(activeWalkthrough)
    }

    private fun stop(){
        val walkthroughStatistics = activeWalkthrough?.getWalkthrough()
        if (walkthroughStatistics != null){
            walkthroughStatistics.toXml(applicationContext)
            DatabaseHelper.getInstance(applicationContext).write(walkthroughStatistics.id, walkthroughStatistics)
        }
        objectInfoSpinnerLayout = null
        mode = WalkthroughMode.SELECT_STAKEHOLDER
        walkthrough_layout_selection_content.visibility = VISIBLE
        walkthrough_layout_selection.visibility = VISIBLE
        walkthrough_holder.visibility = GONE
        activeWalkthrough = null
        getContentHolderLayout().removeAllViews()
        customizeToolbarId(R.string.icon_back,null,null,null,null)
    }

    override fun onToolbarCenterClicked() {
        if (mode == WalkthroughMode.PLAY){
            getInfoContent().visibility = GONE
            val objects = activeWalkthrough!!.getObjectNames("")
            val contextInfoAvailable = objects.size > 1
            getInfoTitle().text = getString(if (contextInfoAvailable) R.string.walkthrough_selection_info else R.string.walkthrough_selection_no_info)
            customizeToolbarId(null,null,null,null,R.string.icon_cross)
            execMorphInfoBar(InfoState.MAXIMIZED)
            if (contextInfoAvailable){
                objectInfoSpinnerLayout = createLine(getString(R.string.literal_object), LineInputType.LOOKUP, null, objects) { objectInfoSelected() }
                getInfoContentWrap().addView(objectInfoSpinnerLayout,0)
            }
            activeWalkthrough?.setInfoActive(true)
            mode = WalkthroughMode.INFO
        }
    }

    private fun objectInfoSelected(){
        if (objectInfoSpinnerLayout != null){
            val spinner = searchForLayout(objectInfoSpinnerLayout!!, Spinner::class)
            val objectName = spinner?.selectedItem.toString()
            val obj = scenarioContext?.getObjectByName(objectName)
            //Cleanup
            getInfoContentWrap().removeView(attributeInfoSpinnerLayout)
            getInfoContentWrap().removeView(selectedAttributeInfoLayout)
            selectedObject = null
            selectedAttribute = null
            if (obj != null && obj != selectedObject ){
                Walkthrough.WalkthroughProperty.INFO_OBJECT.set(objectName,String::class)
                selectedObject = obj
                attributeInfoSpinnerLayout = createLine(getString(R.string.literal_attribute), LineInputType.LOOKUP, null, obj.getAttributeNames("")) { attributeInfoSelected() }
                getInfoContentWrap().addView(attributeInfoSpinnerLayout,1)
            }
        }
    }

    private fun attributeInfoSelected(){
        if (attributeInfoSpinnerLayout != null){
            val spinner = searchForLayout(attributeInfoSpinnerLayout!!, Spinner::class)
            val attributeName = spinner?.selectedItem.toString()
            val attr = selectedObject?.getAttributeByName(attributeName)
            //Cleanup
            getInfoContentWrap().removeView(selectedAttributeInfoLayout)
            selectedAttribute = null
            if (attr != null && attr != selectedAttribute){
                Walkthrough.WalkthroughProperty.INFO_ATTRIBUTE.set(attributeName,String::class)
                selectedAttribute = attr
                selectedAttributeInfoLayout = createLine(getString(R.string.literal_value), LineInputType.MULTI_LINE_TEXT, attr.value)
                getInfoContentWrap().addView(selectedAttributeInfoLayout,2)
            }
        }
    }

    override fun onToolbarLeftClicked() {
        if (mode != WalkthroughMode.PLAY){
            super.onToolbarLeftClicked()
        }
    }

    private var awaitingBackConfirmation = false
    override fun onBackPressed() {
        if (mode != WalkthroughMode.PLAY || awaitingBackConfirmation){
            super.onBackPressed()
        }else{
            notify("Press the Back Button again to cancel the Walkthrough")
            awaitingBackConfirmation = true
            Handler().postDelayed({awaitingBackConfirmation=false},2000)
        }
    }
}

