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
        SELECT_PROJECT, SELECT_SCENARIO, SELECT_WALKTHROUGH
    }

    private var mode: AnalyticsMode = AnalyticsMode.SELECT_PROJECT
    private var pointer: Int? = null
    private var projectPointer: Int? = null
    private var scenarioPointer: Int? = null

    private val loadedProjects = ArrayList<Project>()
    private val loadedScenarios = ArrayList<Scenario>()
    private val loadedWalkthroughs = ArrayList<Walkthrough>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getInfoTitle().text = StringHelper.styleString(getSpannedStringFromId(getConfiguredInfoString()), fontAwesome)
        loadData()
        creationButton = SwipeButton(this, if (loadedProjects.isEmpty() || loadedProjects[0] is Project.NullProject) "No Walkthroughs found" else createButtonLabel(loadedProjects, "Projects"))
                .setColors(Color.WHITE, Color.GRAY)
                .setButtonMode(SwipeButton.SwipeButtonMode.QUADRUPLE)
                .setButtonIcons(R.string.icon_backward, R.string.icon_forward, R.string.icon_undo, R.string.icon_check, null)
                .setButtonStates(!loadedProjects.isEmpty(), !loadedProjects.isEmpty(), false, false)
                .adaptMasterLayoutParams(true)
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
            if (scenario != null && scenario !is Scenario.NullScenario) {
                loadedScenarios.add(scenario)
            }
        }
        map.clear()
        for (scenario in loadedScenarios) {
            map[scenario.projectId] = 1
        }
        for (entry in map.entries) {
            val project = DatabaseHelper.getInstance(applicationContext).readFull(entry.key, Project::class)
            if (project != null && project !is Project.NullProject) {
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
            AnalyticsMode.SELECT_SCENARIO -> select(loadedScenarios, true)
            AnalyticsMode.SELECT_WALKTHROUGH -> select(loadedWalkthroughs, true)
            else -> return
        }
    }

    private fun execPrev() {
        when (mode) {
            AnalyticsMode.SELECT_PROJECT -> select(loadedProjects, false)
            AnalyticsMode.SELECT_SCENARIO -> select(loadedScenarios, false)
            AnalyticsMode.SELECT_WALKTHROUGH -> select(loadedWalkthroughs, false)
            else -> return
        }
    }


    private fun execSelect() {
        when (mode) {
            AnalyticsMode.SELECT_PROJECT -> {
                mode = AnalyticsMode.SELECT_SCENARIO
                projectPointer = pointer
                pointer = null
                creationButton?.setButtonStates(true, true, true, false)?.setText(createButtonLabel(loadedScenarios, "Scenarios"))?.updateViews(false)
            }
            AnalyticsMode.SELECT_SCENARIO ->  {
                mode = AnalyticsMode.SELECT_WALKTHROUGH
                scenarioPointer = pointer
                pointer = null
                creationButton?.setButtonStates(true, true, true, false)
                        ?.setButtonIcons(R.string.icon_backward, R.string.icon_forward, R.string.icon_undo, R.string.icon_search, null)
                        ?.setText(createButtonLabel(loadedWalkthroughs, "Walkthroughs"))
                        ?.updateViews(false)
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
                        ?.setText(createButtonLabel(loadedScenarios, getString(R.string.literal_scenarios)))
                        ?.updateViews(false)
                scroll_holder_linear_layout_holder.removeAllViews()
            }
            else -> return
        }
    }

    private fun showStatistics() {
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
            is Project -> creationButton?.setButtonStates(true, true, false, true)?.setText((selectedList[pointer!!] as Project).title)?.updateViews(false)
            is Scenario -> creationButton?.setButtonStates(true, true, true, true)?.setText((selectedList[pointer!!] as Scenario).title)?.updateViews(false)
            is Walkthrough -> {
                creationButton?.setButtonStates(true, true, true, true)?.setText((selectedList[pointer!!] as Walkthrough).owner)?.updateViews(false)
                Handler().postDelayed({
                    scroll_holder_linear_layout_holder.removeAllViews()
                    scroll_holder_linear_layout_holder.addView(WalkthroughAnalyticLayout(this, selectedList[pointer!!] as Walkthrough))
                },500)
            }
        }
    }


}