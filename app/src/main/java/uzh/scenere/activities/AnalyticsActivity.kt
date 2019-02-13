package uzh.scenere.activities

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import kotlinx.android.synthetic.main.activity_analytics.*
import kotlinx.android.synthetic.main.scroll_holder.*
import uzh.scenere.R
import uzh.scenere.datamodel.Project
import uzh.scenere.datamodel.Scenario
import uzh.scenere.datamodel.Walkthrough
import uzh.scenere.helpers.DatabaseHelper
import uzh.scenere.helpers.NullHelper
import uzh.scenere.helpers.StringHelper
import uzh.scenere.views.SwipeButton
import uzh.scenere.views.WalkthroughAnalyticLayout
import java.io.Serializable

class AnalyticsActivity : AbstractManagementActivity() {
    override fun isInEditMode(): Boolean {
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
        SELECT_PROJECT, SELECT_SCENARIO, SELECT_WALKTHROUGH, SELECT_STEP
    }

    private var mode: AnalyticsMode = AnalyticsMode.SELECT_PROJECT
    private var pointer: Int? = null
    private var projectPointer: Int? = null
    private var scenarioPointer: Int? = null

    private val loadedProjects = ArrayList<Project>()
    private val loadedScenarios = ArrayList<Scenario>()
    private val activeScenarios = ArrayList<Scenario>()
    private val loadedWalkthroughs = ArrayList<Walkthrough>()
    private val activeWalkthroughs = ArrayList<Walkthrough>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getInfoTitle().text = StringHelper.styleString(getSpannedStringFromId(getConfiguredInfoString()), fontAwesome)
        loadData()
        creationButton = SwipeButton(this, if (loadedProjects.isEmpty()) "No Walkthroughs found" else if (loadedProjects[0] is Project.NullProject) getString(R.string.project_anonymous) else createButtonLabel(loadedProjects, getString(R.string.projects)))
                .setColors(Color.WHITE, Color.GRAY)
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
            else -> return
        }
    }

    private fun execPrev() {
        when (mode) {
            AnalyticsMode.SELECT_PROJECT -> select(loadedProjects, false)
            AnalyticsMode.SELECT_SCENARIO -> select(activeScenarios, false)
            AnalyticsMode.SELECT_WALKTHROUGH -> select(activeWalkthroughs, false)
            else -> return
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
                    label = createButtonLabel(activeScenarios, getString(R.string.scenarios))
                }
                creationButton?.setButtonStates(true, true, true, pointer==0)?.setText(label)?.updateViews(false)

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
                }
                creationButton?.setButtonStates(true, true, true, false)?.setText(createButtonLabel(activeWalkthroughs, getString(R.string.walkthroughs)))?.updateViews(false)
            }
            AnalyticsMode.SELECT_WALKTHROUGH -> showStatistics()
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
            }
            AnalyticsMode.SELECT_WALKTHROUGH -> {
                mode = AnalyticsMode.SELECT_SCENARIO
                pointer = null
                scenarioPointer = null
                creationButton?.setButtonStates(!loadedScenarios.isEmpty(), !loadedScenarios.isEmpty(), true, false)
                        ?.setButtonIcons(R.string.icon_backward, R.string.icon_forward, R.string.icon_undo, R.string.icon_check, null)
                        ?.setText(createButtonLabel(activeScenarios, getString(R.string.literal_scenarios)))
                        ?.updateViews(false)
                scroll_holder_linear_layout_holder.removeAllViews()
            }
            else -> return
        }
    }

    private fun showStatistics() {
        //TODO
    }

    private fun <T : Serializable> createButtonLabel(selectedList: ArrayList<T>, label: String): String {
        if (selectedList.isEmpty()) {
            return getString(R.string.walkthrough_button_label_failure, label)
        }
        return getString(R.string.walkthrough_button_label, selectedList.size, label)
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
                creationButton?.setButtonStates(true, true, true, true)?.setText("Walkthrough number "+(pointer!!+1))?.updateViews(false)
                Handler().postDelayed({
                    scroll_holder_linear_layout_holder.removeAllViews()
                    scroll_holder_linear_layout_holder.addView(WalkthroughAnalyticLayout(this, selectedList[pointer!!] as Walkthrough, true))
                }, 500)
            }
        }
    }
}