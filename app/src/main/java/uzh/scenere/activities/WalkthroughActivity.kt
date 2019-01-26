package uzh.scenere.activities

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_walkthrough.*
import kotlinx.android.synthetic.main.holder.*
import uzh.scenere.R
import uzh.scenere.datamodel.Project
import uzh.scenere.datamodel.Scenario
import uzh.scenere.datamodel.Stakeholder
import uzh.scenere.helpers.DatabaseHelper
import uzh.scenere.helpers.StringHelper
import uzh.scenere.views.SwipeButton
import uzh.scenere.views.WalkthroughPlayLayout
import java.io.Serializable

class WalkthroughActivity : AbstractManagementActivity() {
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
        SELECT_PROJECT, SELECT_SCENARIO, SELECT_STAKEHOLDER, PLAY
    }

    private var mode: WalkthroughMode = WalkthroughMode.SELECT_PROJECT
    private val loadedProjects: ArrayList<Project> = ArrayList<Project>()
    private val loadedScenarios: ArrayList<Scenario> = ArrayList<Scenario>()
    private val loadedStakeholders: ArrayList<Stakeholder> = ArrayList<Stakeholder>()
    private var pointer: Int? = null
    private var projectPointer: Int? = null
    private var scenarioPointer: Int? = null
    private var scenarioContext: Scenario? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getInfoTitle().text = StringHelper.styleString(getSpannedStringFromId(getConfiguredInfoString()), fontAwesome)
        loadedProjects.clear()
        loadedProjects.addAll(DatabaseHelper.getInstance(applicationContext).readBulk(Project::class, null))
        creationButton = SwipeButton(this, createButtonLabel(loadedProjects, "Projects"))
                .setColors(Color.WHITE, Color.GRAY)
                .setButtonMode(SwipeButton.SwipeButtonMode.QUADRUPLE)
                .setButtonIcons(R.string.icon_backward, R.string.icon_forward, R.string.icon_undo, R.string.icon_check, null)
                .setButtonStates(!loadedProjects.isEmpty(), !loadedProjects.isEmpty(), false, false)
                .adaptMasterLayoutParams(true)
                .updateViews(true)
        creationButton?.setExecutable(createControlExecutable())
        walkthrough_layout_selection_content.addView(creationButton)
    }

    private fun <T : Serializable> createButtonLabel(selectedList: ArrayList<T>, label: String): String {
        if (selectedList.isEmpty()) {
            return resources.getString(R.string.walkthrough_button_label_failure, label)
        }
        return resources.getString(R.string.walkthrough_button_label, selectedList.size, label)
    }

    private fun createControlExecutable(): SwipeButton.SwipeButtonExecution {
        return object : SwipeButton.SwipeButtonExecution {
            override fun execLeft() {
                execPrev()
                Handler().postDelayed({ creationButton?.collapse() }, 250)
            }

            override fun execRight() {
                execNext()
                Handler().postDelayed({ creationButton?.collapse() }, 250)
            }

            override fun execDown() {
                execSelect()
                Handler().postDelayed({ creationButton?.collapse() }, 250)
            }

            override fun execUp() {
                execBack()
                Handler().postDelayed({ creationButton?.collapse() }, 250)
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
                loadedStakeholders.addAll(DatabaseHelper.getInstance(applicationContext).readBulk(Stakeholder::class, loadedProjects[projectPointer!!])) //TODO: Reduce to Scenario-Stakeholders
                scenarioContext = DatabaseHelper.getInstance(applicationContext).readFull(loadedScenarios[pointer!!].id,Scenario::class)
                scenarioPointer = pointer
                pointer = null
                walkthrough_text_selected_scenario.text = loadedScenarios[scenarioPointer!!].title
                creationButton?.setButtonStates(!loadedStakeholders.isEmpty(), !loadedStakeholders.isEmpty(), true, false)
                        ?.setButtonIcons(R.string.icon_backward, R.string.icon_forward, R.string.icon_undo, R.string.icon_play, null)
                        ?.setText(createButtonLabel(loadedStakeholders, "Stakeholders"))
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
                creationButton?.setButtonStates(!loadedProjects.isEmpty(), !loadedProjects.isEmpty(), false, false)?.setText(createButtonLabel(loadedProjects, "Projects"))?.updateViews(false)
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
                        ?.setText(createButtonLabel(loadedScenarios, "Scenarios"))
                        ?.updateViews(false)
            }
            else -> return
        }
    }

    private fun play() {
        mode = WalkthroughMode.PLAY
        walkthrough_layout_selection.visibility = GONE
        walkthrough_holder.visibility = VISIBLE
        val path = scenarioContext?.getAllPaths(loadedStakeholders[pointer!!])
        getContentHolderLayout().addView(WalkthroughPlayLayout(applicationContext,path))
    }
}

