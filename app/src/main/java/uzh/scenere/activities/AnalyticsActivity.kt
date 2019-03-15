package uzh.scenere.activities

import android.os.Bundle
import android.os.Handler
import android.support.v4.content.ContextCompat
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.LinearLayout.VERTICAL
import kotlinx.android.synthetic.main.activity_analytics.*
import kotlinx.android.synthetic.main.activity_walkthrough.*
import uzh.scenere.R
import uzh.scenere.const.Constants
import uzh.scenere.const.Constants.Companion.NONE
import uzh.scenere.const.Constants.Companion.NOTHING
import uzh.scenere.const.Constants.Companion.SPACE
import uzh.scenere.datamodel.Project
import uzh.scenere.datamodel.Scenario
import uzh.scenere.datamodel.Walkthrough
import uzh.scenere.helpers.*
import uzh.scenere.views.*
import java.io.Serializable

class AnalyticsActivity : AbstractManagementActivity() {

    override fun getConfiguredRootLayout(): ViewGroup? {
        return analytics_root
    }

    override fun isInEditMode(): Boolean {
        return false
    }

    override fun isInAddMode(): Boolean {
        return false
    }

    override fun isInViewMode(): Boolean {
        return true
    }

    override fun resetEditMode() {
        //NOP
    }

    override fun createEntity() {
        //NOP
    }

    override fun getConfiguredInfoString(): Int {
        return R.string.icon_explain_analytics
    }

    override fun getConfiguredLayout(): Int {
        return R.layout.activity_analytics
    }

    enum class AnalyticsMode {
        SELECT_PROJECT, SELECT_SCENARIO, SELECT_WALKTHROUGH, SELECT_STATISTICS, SELECT_COMMENTS
    }

    private var mode: AnalyticsMode = AnalyticsMode.SELECT_PROJECT
    private var pointer: Int? = null
    private var projectPointer: Int? = null
    private var scenarioPointer: Int? = null

    private val loadedProjects = ArrayList<Project>()
    private val loadedScenarios = ArrayList<Scenario>()
    private val activeScenarios = ArrayList<Scenario>()
    private val activeWalkthroughs = ArrayList<Walkthrough>()
    private var scenarioAnalytics: ScenarioAnalyticLayout? = null
    private var stepAnalytics: StepAnalyticsLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getInfoTitle().text = StringHelper.styleString(getSpannedStringFromId(getConfiguredInfoString()), fontAwesome)
        loadData()
        creationButton = SwipeButton(this, if (loadedProjects.isEmpty()) getString(R.string.analytics_no_walkthrough) else if (loadedProjects[0] is Project.NullProject) getString(R.string.project_anonymous) else createButtonLabel(loadedProjects, getString(R.string.literal_projects)))
                .setColors(getColorWithStyle(applicationContext,R.color.srePrimaryPastel), getColorWithStyle(applicationContext,R.color.srePrimaryDisabled))
                .setButtonMode(SwipeButton.SwipeButtonMode.QUADRUPLE)
                .setButtonIcons(R.string.icon_backward, R.string.icon_forward, R.string.icon_undo, R.string.icon_check, null)
                .setButtonStates(!loadedProjects.isEmpty(), !loadedProjects.isEmpty(), false, false)
                .adaptMasterLayoutParams(true)
                .setFirstPosition()
                .setAutoCollapse(true)
                .updateViews(true)
        creationButton?.setExecutable(createControlExecutable())
        analytics_layout_button_holder.addView(creationButton)
        customizeToolbarId(R.string.icon_back, null, null, null, null)
        getInfoTitle().textSize = DipHelper.get(resources).dip2_5.toFloat()
        tutorialOpen = SreTutorialLayoutDialog(this,screenWidth,"info_analytics","info_analytics_type").addEndExecutable { tutorialOpen = false }.show(tutorialOpen)
        createOverviewLayout()
    }


    private var projectLabel: SreContextAwareTextView? =  null
    private var scenarioLabel: SreContextAwareTextView? = null
    private var analyticsLabel: SreContextAwareTextView? = null
    private var labelWrapper: LinearLayout? = null

    private fun createOverviewLayout(){
        labelWrapper = LinearLayout(applicationContext)
        labelWrapper?.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
        labelWrapper?.orientation = VERTICAL
        labelWrapper?.weightSum = 6f
        projectLabel = SreContextAwareTextView(applicationContext, walkthrough_layout_selection_orientation, arrayListOf(getString(R.string.walkthrough_selected_project)), ArrayList())
        scenarioLabel = SreContextAwareTextView(applicationContext, walkthrough_layout_selection_orientation, arrayListOf(getString(R.string.walkthrough_selected_scenario)), ArrayList())
        analyticsLabel = SreContextAwareTextView(applicationContext, walkthrough_layout_selection_orientation, arrayListOf(getString(R.string.walkthrough_selected_analytics)), ArrayList())
        val weightedParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0)
        weightedParams.weight = 2f
        projectLabel?.setWeight(weightedParams)
        scenarioLabel?.setWeight(weightedParams)
        analyticsLabel?.setWeight(weightedParams)
        projectLabel?.text = getString(R.string.walkthrough_selected_project).plus(Constants.NONE)
        scenarioLabel?.text = getString(R.string.walkthrough_selected_scenario).plus(Constants.NONE)
        analyticsLabel?.text = getString(R.string.walkthrough_selected_analytics).plus(Constants.NONE)
        labelWrapper?.addView(projectLabel)
        labelWrapper?.addView(scenarioLabel)
        labelWrapper?.addView(analyticsLabel)
        analyticsLabel?.visibility = View.INVISIBLE
        getContentHolderLayout().addView(labelWrapper)
    }

    private fun updateLabelWrapper(project: String? = null, scenario: String? = null, analytics: String? = null){
        if (labelWrapper!= null && labelWrapper?.parent == null){
            getContentHolderLayout().addView(labelWrapper)
        }
        if (StringHelper.hasText(project)){
            projectLabel?.text = getString(R.string.walkthrough_selected_project).plus(SPACE).plus(project)
        }
        if (StringHelper.hasText(scenario)){
            scenarioLabel?.text = getString(R.string.walkthrough_selected_scenario).plus(SPACE).plus(scenario)
        }
        if (StringHelper.hasText(analytics)){
            analyticsLabel?.visibility = View.VISIBLE
            analyticsLabel?.text = getString(R.string.walkthrough_selected_analytics).plus(SPACE).plus(analytics)
        }else{
            analyticsLabel?.visibility = View.INVISIBLE
        }
    }

    private fun loadData() {
        //Load Data Bottom-Up
        val map = HashMap<String, Int>()
        loadedWalkthroughs.addAll(DatabaseHelper.getInstance(applicationContext).readBulk(Walkthrough::class, null))
        for (walkthrough in loadedWalkthroughs) {
            map[walkthrough.scenarioId] = 1
        }
        for (entry in map.entries) {
            val scenario = DatabaseHelper.getInstance(applicationContext).readFull(entry.key, Scenario::class)
            if (scenario != null) {
                loadedScenarios.add(scenario)
            }
        }
        map.clear()
        for (scenario in loadedScenarios) {
            map[scenario.projectId] = 1
        }
        for (entry in map.entries) {
            val project = DatabaseHelper.getInstance(applicationContext).readFull(entry.key, Project::class)
            if (project != null) {
                loadedProjects.add(project)
            }
        }
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
            AnalyticsMode.SELECT_PROJECT -> select(loadedProjects, true)
            AnalyticsMode.SELECT_SCENARIO -> select(activeScenarios, true)
            AnalyticsMode.SELECT_WALKTHROUGH -> select(activeWalkthroughs, true)
            AnalyticsMode.SELECT_STATISTICS -> {
                getContentHolderLayout().removeView(labelWrapper)
                scenarioAnalytics?.addTo(getContentHolderLayout())
                scenarioAnalytics?.nextStakeholder()
                val text = ObjectHelper.nvl(scenarioAnalytics?.getActiveStakeholder()?.name, NOTHING)
                creationButton?.setText(text)?.updateViews(false)
            }
            AnalyticsMode.SELECT_COMMENTS -> {
                getContentHolderLayout().removeView(labelWrapper)
                stepAnalytics?.addTo(getContentHolderLayout())
                stepAnalytics?.nextStep()
                val text = ObjectHelper.nvl(stepAnalytics?.getActiveStepName(), NOTHING)
                creationButton?.setText(text)?.updateViews(false)
            }
        }
    }

    private fun execPrev() {
        when (mode) {
            AnalyticsMode.SELECT_PROJECT -> select(loadedProjects, false)
            AnalyticsMode.SELECT_SCENARIO -> select(activeScenarios, false)
            AnalyticsMode.SELECT_WALKTHROUGH -> select(activeWalkthroughs, false)
            AnalyticsMode.SELECT_STATISTICS -> {
                getContentHolderLayout().removeView(labelWrapper)
                scenarioAnalytics?.addTo(getContentHolderLayout())
                scenarioAnalytics?.previousStakeholder()
                val text = ObjectHelper.nvl(scenarioAnalytics?.getActiveStakeholder()?.name, NOTHING)
                creationButton?.setText(text)?.updateViews(false)
            }
            AnalyticsMode.SELECT_COMMENTS -> {
                getContentHolderLayout().removeView(labelWrapper)
                stepAnalytics?.addTo(getContentHolderLayout())
                stepAnalytics?.previousStep()
                val text = ObjectHelper.nvl(stepAnalytics?.getActiveStepName(), NOTHING)
                creationButton?.setText(text)?.updateViews(false)
            }
        }
    }

    private fun execSelect() {
        when (mode) {
            AnalyticsMode.SELECT_PROJECT -> {
                mode = AnalyticsMode.SELECT_SCENARIO
                projectPointer = pointer
                pointer = null
                activeScenarios.clear()
                val activeProject = loadedProjects[projectPointer!!]
                var label = getString(R.string.scenario_anonymous)
                if (activeProject is Project.NullProject) {
                    pointer = 0
                    activeScenarios.add(NullHelper.get(Scenario::class))
                } else {
                    for (scenario in loadedScenarios) {
                        if (scenario.projectId == activeProject.id) {
                            activeScenarios.add(scenario)
                        }
                    }
                    label = createButtonLabel(activeScenarios, getString(R.string.literal_scenarios))
                    updateLabelWrapper(activeProject.title,NONE)
                }
                creationButton?.setButtonStates(true, true, true, pointer==0)?.setText(label)
                        ?.setButtonIcons(R.string.icon_backward, R.string.icon_forward, R.string.icon_undo, R.string.icon_walkthrough, null)
                        ?.updateViews(false)

            }
            AnalyticsMode.SELECT_SCENARIO -> {
                mode = AnalyticsMode.SELECT_WALKTHROUGH
                scenarioPointer = pointer
                pointer = null
                activeWalkthroughs.clear()
                val activeScenario = activeScenarios[scenarioPointer!!]
                if (activeScenario is Scenario.NullScenario) {
                    for (walkthrough in loadedWalkthroughs) {
                        var found = false
                        for (scenario in loadedScenarios) {
                            if (walkthrough.scenarioId == scenario.id) {
                                found = true
                            }
                        }
                        if (!found){
                            activeWalkthroughs.add(walkthrough)
                        }
                    }
                } else {
                    for (walkthrough in loadedWalkthroughs) {
                        if (walkthrough.scenarioId == activeScenario.id) {
                            activeWalkthroughs.add(walkthrough)
                        }
                    }
                    updateLabelWrapper(null,activeScenario.title,getString(R.string.analytics_walkthroughs))
                }
                creationButton?.setButtonStates(true, true, true, true)
                        ?.setButtonIcons(R.string.icon_backward, R.string.icon_forward, R.string.icon_undo, R.string.icon_chart_bar, null)
                        ?.setText(createButtonLabel(activeWalkthroughs, getString(R.string.literal_walkthroughs)))
                        ?.updateViews(false)
            }
            AnalyticsMode.SELECT_WALKTHROUGH-> {
                mode = AnalyticsMode.SELECT_STATISTICS
                createScenarioStatistics()
                updateLabelWrapper(null,null,getString(R.string.analytics_statistics))
                creationButton?.setButtonStates(true, true, true, true)
                        ?.setButtonIcons(R.string.icon_backward, R.string.icon_forward, R.string.icon_undo, R.string.icon_input, null)
                        ?.setText(createButtonLabel(NumberHelper.nvl(scenarioAnalytics?.getStakeholderCount(),0), getString(R.string.literal_stakeholders)))
                        ?.updateViews(false)
            }
            AnalyticsMode.SELECT_STATISTICS-> {
                mode = AnalyticsMode.SELECT_COMMENTS
                createStepStatistics()
                updateLabelWrapper(null,null,getString(R.string.analytics_comments))
                val commentCount = NumberHelper.nvl(stepAnalytics?.getStepCount(), 0)
                creationButton?.setButtonStates(commentCount > 0, commentCount > 0, true, false)
                        ?.setButtonIcons(R.string.icon_backward, R.string.icon_forward, R.string.icon_undo, R.string.icon_null, null)
                        ?.setText(createButtonLabel(commentCount, getString(R.string.literal_steps_with_comments)))
                        ?.updateViews(false)
            }
            else -> return
        }
    }


    private fun execBack() {
        when (mode) {
            AnalyticsMode.SELECT_SCENARIO -> {
                mode = AnalyticsMode.SELECT_PROJECT
                pointer = null
                projectPointer = null
                creationButton?.setButtonStates(!loadedProjects.isEmpty(), !loadedProjects.isEmpty(), false, false)?.setText(createButtonLabel(loadedProjects, getString(R.string.literal_projects)))?.updateViews(false)
                getContentHolderLayout().removeAllViews()
                updateLabelWrapper(NONE,NONE)
            }
            AnalyticsMode.SELECT_WALKTHROUGH -> {
                mode = AnalyticsMode.SELECT_SCENARIO
                pointer = null
                scenarioPointer = null
                creationButton?.setButtonStates(!loadedScenarios.isEmpty(), !loadedScenarios.isEmpty(), true, false)
                        ?.setButtonIcons(R.string.icon_backward, R.string.icon_forward, R.string.icon_undo, R.string.icon_walkthrough, null)
                        ?.setText(createButtonLabel(activeScenarios, getString(R.string.literal_scenarios)))
                        ?.updateViews(false)
                getContentHolderLayout().removeAllViews()
                updateLabelWrapper(null,NONE)
            }
            AnalyticsMode.SELECT_STATISTICS -> {
                mode = AnalyticsMode.SELECT_WALKTHROUGH
                pointer = null
                creationButton?.setButtonStates(true, true, true, true)
                        ?.setButtonIcons(R.string.icon_backward, R.string.icon_forward, R.string.icon_undo, R.string.icon_chart_bar, null)
                        ?.setText(createButtonLabel(activeWalkthroughs, getString(R.string.literal_walkthroughs)))
                        ?.updateViews(false)
                getContentHolderLayout().removeAllViews()
                updateLabelWrapper(null,null,getString(R.string.analytics_walkthroughs))
            }
            AnalyticsMode.SELECT_COMMENTS -> {
                mode = AnalyticsMode.SELECT_STATISTICS
                pointer = null
                creationButton?.setButtonStates(true, true, true, true)
                        ?.setButtonIcons(R.string.icon_backward, R.string.icon_forward, R.string.icon_undo, R.string.icon_input, null)
                        ?.setText(createButtonLabel(activeWalkthroughs, getString(R.string.literal_walkthroughs)))
                        ?.updateViews(false)
                getContentHolderLayout().removeAllViews()
                updateLabelWrapper(null,null,getString(R.string.analytics_statistics))
            }
            else -> return
        }
    }

    private fun createScenarioStatistics() {
        getContentHolderLayout().removeAllViews()
        scenarioAnalytics = ScenarioAnalyticLayout(applicationContext, *activeWalkthroughs.toTypedArray())
    }

    private fun createStepStatistics() {
        getContentHolderLayout().removeAllViews()
        stepAnalytics = StepAnalyticsLayout(applicationContext, *activeWalkthroughs.toTypedArray())
    }

    private val loadedWalkthroughs = ArrayList<Walkthrough>()

    private fun <T : Serializable> createButtonLabel(selectedList: ArrayList<T>, label: String): String {
        if (selectedList.isEmpty()) {
            return getString(R.string.walkthrough_button_label_failure, label)
        }
        return getString(R.string.walkthrough_button_label, selectedList.size, label)
    }

    private fun createButtonLabel(selectedListSize: Int, label: String): String {
        if (selectedListSize == 0) {
            return getString(R.string.walkthrough_button_label_failure, label)
        }
        return getString(R.string.walkthrough_button_label, selectedListSize, label)
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
            is Project -> {
                val text = StringHelper.nvl((selectedList[pointer!!] as Project).title,getString(R.string.project_anonymous))
                creationButton?.setButtonStates(true, true, false, true)?.setText(text)?.updateViews(false)
            }
            is Scenario -> {
                val text = StringHelper.nvl((selectedList[pointer!!] as Scenario).title,getString(R.string.scenario_anonymous))
                creationButton?.setButtonStates(true, true, true, true)?.setText(text)?.updateViews(false)
            }
            is Walkthrough -> {
                creationButton?.setButtonStates(true, true, true, true)?.setText(getString(R.string.analytics_walkthrough_x,(pointer!!+1)))?.updateViews(false)
                Handler().postDelayed({
                    getContentHolderLayout().removeAllViews()
                    getContentHolderLayout().addView(WalkthroughAnalyticLayout(applicationContext, selectedList[pointer!!] as Walkthrough, true) {
                        DatabaseHelper.getInstance(applicationContext).delete((selectedList[pointer!!] as Walkthrough).id,Walkthrough::class)
                        loadedWalkthroughs.remove(selectedList[pointer!!] as Walkthrough)
                        selectedList.removeAt(pointer!!)
                        if  (selectedList.isEmpty()){
                            getContentHolderLayout().removeAllViews()
                        }else{
                            execNext()
                        }
                        notify(getString(R.string.deleted))
                    })
                }, 500)
            }
        }
    }
}