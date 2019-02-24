package uzh.scenere.activities

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import kotlinx.android.synthetic.main.scroll_holder.*
import uzh.scenere.R
import uzh.scenere.const.Constants
import uzh.scenere.datamodel.AbstractObject
import uzh.scenere.datamodel.Project
import uzh.scenere.datamodel.Scenario
import uzh.scenere.helpers.DatabaseHelper
import uzh.scenere.helpers.StringHelper
import uzh.scenere.helpers.getStringValue
import uzh.scenere.views.SwipeButton

class ScenariosActivity : AbstractManagementActivity() {

    override fun getConfiguredInfoString(): Int {
        return R.string.icon_explain_scenarios
    }
    override fun getConfiguredLayout(): Int {
        return R.layout.activity_scenarios
    }

    enum class ScenarioMode {
        VIEW, EDIT_CREATE, OBJECTS, EDITOR
    }
    private var scenariosMode: ScenarioMode = ScenarioMode.VIEW
    override fun isInViewMode(): Boolean {
        return scenariosMode == ScenarioMode.VIEW
    }

    override fun isInEditMode(): Boolean {
        return scenariosMode == ScenarioMode.EDIT_CREATE
    }

    override fun isInAddMode(): Boolean {
        return scenariosMode == ScenarioMode.EDIT_CREATE
    }

    override fun resetEditMode() {
        activeScenario = null
        scenariosMode = ScenarioMode.VIEW
    }

    private val inputLabelTitle = "Scenario Name"
    private val inputLabelIntro = "Scenario Intro"
    private val inputLabelOutro = "Scenario Outro"
    private var activeProject: Project? = null
    private var activeScenario: Scenario? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activeProject = intent.getSerializableExtra(Constants.BUNDLE_PROJECT) as Project
        creationButton =
                SwipeButton(this, "Create New Scenario")
                        .setButtonMode(SwipeButton.SwipeButtonMode.DOUBLE)
                        .setColors(Color.WHITE, Color.GRAY)
                        .setButtonStates(false, true, false, false)
                        .setButtonIcons(R.string.icon_null, R.string.icon_edit, null, null, R.string.icon_scenario)
                        .setFirstPosition()
                        .updateViews(true)
        creationButton!!.setExecutable(generateCreationExecutable(creationButton!!))
        scroll_holder_linear_layout_holder.addView(creationButton)
        createTitle("", scroll_holder_linear_layout_holder)
        for (scenario in DatabaseHelper.getInstance(applicationContext).readBulk(Scenario::class, activeProject)) {
            addScenarioToList(scenario)
        }
        scroll_holder_text_info_title.text = StringHelper.styleString(getSpannedStringFromId(R.string.icon_explain_scenarios), fontAwesome)
        customizeToolbarText(resources.getText(R.string.icon_back).toString(), null, getLockIcon(), null, null)
    }

    private fun addScenarioToList(scenario: Scenario) {
        val swipeButton = SwipeButton(this, scenario.title)
                .setColors(Color.WHITE, Color.GRAY)
                .setButtonMode(SwipeButton.SwipeButtonMode.QUADRUPLE)
                .setButtonIcons(R.string.icon_delete, R.string.icon_edit, R.string.icon_object, R.string.icon_path_editor, null)
                .setButtonStates(lockState == LockState.UNLOCKED, true, true, true)
                .updateViews(true)
        swipeButton.dataObject = scenario
        swipeButton.setCounter(DatabaseHelper.getInstance(applicationContext).readBulk(AbstractObject::class,scenario).size,null)
        swipeButton.setExecutable(generateScenarioExecutable(swipeButton, scenario))
        scroll_holder_linear_layout_holder.addView(swipeButton)
    }

    private fun generateCreationExecutable(button: SwipeButton, scenario: Scenario? = null): SwipeButton.SwipeButtonExecution {
        return object : SwipeButton.SwipeButtonExecution {
            override fun execRight() {
                activeButton = button
                openInput(ScenarioMode.EDIT_CREATE)
            }
        }
    }

    private fun generateScenarioExecutable(button: SwipeButton, scenario: Scenario? = null): SwipeButton.SwipeButtonExecution {
        return object : SwipeButton.SwipeButtonExecution {
            override fun execLeft() {
                if (scenario != null) {
                    removeScenario(scenario,true)
                    showDeletionConfirmation(scenario.title)
                }
            }
            override fun execRight() {
                activeButton = button
                openInput(ScenarioMode.EDIT_CREATE, scenario)
            }
            override fun execUp() {
                activeButton = button
                openInput(ScenarioMode.OBJECTS, scenario)
            }
            override fun execDown() {
                activeButton = button
                openInput(ScenarioMode.EDITOR, scenario)
            }
            override fun execReset() {
                resetEditMode()
            }
        }
    }

    override fun createEntity() {
        val title = inputMap[inputLabelTitle]!!.getStringValue()
        val intro = inputMap[inputLabelIntro]!!.getStringValue()
        val outro = inputMap[inputLabelIntro]!!.getStringValue()
        val scenarioBuilder = Scenario.ScenarioBuilder(activeProject!!, title, intro, outro)
        if (activeScenario != null) {
            removeScenario(activeScenario!!)
            scenarioBuilder.copyId(activeScenario!!)
        }
        val scenario = scenarioBuilder.build()
        DatabaseHelper.getInstance(applicationContext).write(scenario.id, scenario)
        addScenarioToList(scenario)
    }

    private fun openInput(scenariosMode: ScenarioMode, scenario: Scenario? = null) {
        activeScenario = scenario
        this.scenariosMode = scenariosMode
        when (scenariosMode) {
            ScenarioMode.VIEW -> {}//NOP
            ScenarioMode.EDIT_CREATE -> {
                cleanInfoHolder(if (activeScenario == null) getString(R.string.scenarios_create) else getString(R.string.scenarios_edit))
                scroll_holder_text_info_content_wrap.addView(createLine(inputLabelTitle, LineInputType.SINGLE_LINE_EDIT, scenario?.title))
                scroll_holder_text_info_content_wrap.addView(createLine(inputLabelIntro, LineInputType.MULTI_LINE_EDIT, scenario?.intro))
                scroll_holder_text_info_content_wrap.addView(createLine(inputLabelOutro, LineInputType.MULTI_LINE_EDIT, scenario?.outro))
            }
            ScenarioMode.OBJECTS -> {
                val intent = Intent(this, ObjectsActivity::class.java)
                intent.putExtra(Constants.BUNDLE_SCENARIO, activeScenario)
                startActivity(intent)
                return
            }
            ScenarioMode.EDITOR -> {
                val intent = Intent(this, EditorActivity::class.java)
                intent.putExtra(Constants.BUNDLE_SCENARIO, activeScenario)
                Handler().postDelayed({ // Delay loading since it can take a while TODO> Delay all?
                startActivity(intent)},350)
                return
            }
        }

        execMorphInfoBar(InfoState.MAXIMIZED)
    }

    private fun removeScenario(scenario: Scenario, dbRemoval: Boolean = false) {
        for (viewPointer in 0 until scroll_holder_linear_layout_holder.childCount) {
            if (scroll_holder_linear_layout_holder.getChildAt(viewPointer) is SwipeButton &&
                    (scroll_holder_linear_layout_holder.getChildAt(viewPointer) as SwipeButton).dataObject == scenario) {
                scroll_holder_linear_layout_holder.removeViewAt(viewPointer)
                if (dbRemoval){
                    DatabaseHelper.getInstance(applicationContext).delete(scenario.id, Scenario::class)
                }
                return
            }
        }
    }
}