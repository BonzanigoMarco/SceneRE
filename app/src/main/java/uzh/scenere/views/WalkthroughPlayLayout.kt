package uzh.scenere.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import uzh.scenere.const.Constants.Companion.ANONYMOUS
import uzh.scenere.const.Constants.Companion.USER_NAME
import uzh.scenere.datamodel.Path
import uzh.scenere.datamodel.Scenario
import uzh.scenere.datamodel.Stakeholder
import uzh.scenere.datamodel.Walkthrough
import uzh.scenere.datamodel.steps.AbstractStep
import uzh.scenere.datamodel.steps.StandardStep
import uzh.scenere.datamodel.trigger.direct.ButtonTrigger
import uzh.scenere.helpers.DatabaseHelper
import uzh.scenere.helpers.DipHelper

@SuppressLint("ViewConstructor")
class WalkthroughPlayLayout(context: Context, private val scenario: Scenario, private val stakeholder: Stakeholder, private val stopFunction: () -> Unit) : LinearLayout(context) {

    private val stepLayout: RelativeLayout = RelativeLayout(context)
    private val triggerLayout: RelativeLayout = RelativeLayout(context)

    enum class WalkthroughPlayMode {
        STEP_INDUCED, TRIGGER_INDUCED
    }

    enum class WalkthroughState {
        STARTED, PLAYING, INFO, FINISHED
    }

    //Statistics
    val walkthrough: Walkthrough = Walkthrough.WalkthroughBuilder(DatabaseHelper.getInstance(context).read(USER_NAME,String::class, ANONYMOUS), scenario.id, stakeholder.id).build()
    private var startingTime: Long = System.currentTimeMillis()
    private fun getTime(): Long{
        val time = System.currentTimeMillis() - startingTime
        startingTime = System.currentTimeMillis()
        return time
    }
    private var infoTime: Long = 0
    //Play
    private var layer: Int = 0
    private val path: HashMap<Int, Path>? = scenario.getAllPaths(stakeholder)
    private var first = path?.get(layer)?.getStartingPoint()
    private var second = path?.get(layer)?.getNextElement(first)
    //State
    private val mode: WalkthroughPlayMode = if (first is AbstractStep) WalkthroughPlayMode.STEP_INDUCED else WalkthroughPlayMode.TRIGGER_INDUCED
    private var state: WalkthroughState = WalkthroughState.STARTED
    private var backupState: WalkthroughState = WalkthroughState.STARTED

    init {
        prepareLayout()
    }

    private fun prepareLayout() {
        removeAllViews()
        stepLayout.removeAllViews()
        triggerLayout.removeAllViews()
        orientation = HORIZONTAL
        weightSum = 10f
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)

        stepLayout.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, 3f)
        triggerLayout.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, 7f)

        stepLayout.setBackgroundColor(Color.BLUE)
        triggerLayout.setBackgroundColor(Color.YELLOW)

        addView(stepLayout)
        addView(triggerLayout)

        if (first == null && second == null) { //TODO Evaluate if end trigger is missing
            state = WalkthroughState.FINISHED
        }
        when (state) {
            WalkthroughState.STARTED -> resolveIntro()
            WalkthroughState.PLAYING -> resolveStepAndTrigger()
            WalkthroughState.FINISHED -> resolveOutro()
            else -> {}
        }
    }

    private fun resolveIntro() {
        val text = generateText("Intro:\n"+scenario.intro)
        val button = generateButton("Start Scenario")
        button.setOnClickListener {
            walkthrough.setIntroTime(getTime())
            loadNextStep()
        }
        stepLayout.addView(text)
        triggerLayout.addView(button)
    }


    private fun resolveStepAndTrigger() {
        when (mode) {
            WalkthroughPlayMode.STEP_INDUCED -> {
                when (first) {
                    is StandardStep -> {
                        val text = generateText((first as StandardStep).text)
                        stepLayout.addView(text)
                    }
                }
                when (second) {
                    is ButtonTrigger -> {
                        val button = generateButton((second as ButtonTrigger).buttonLabel)
                        button.setOnClickListener {
                            loadNextStep()
                        }
                        triggerLayout.addView(button)
                    }
                }
            }
            WalkthroughPlayMode.TRIGGER_INDUCED -> {

            }
        }
    }

    private fun resolveOutro() {
        val text = generateText("Outro:\n"+scenario.intro+"\n"+walkthrough.printStatistics())
        val button = generateButton("Finish Scenario")
        button.setOnClickListener {
            saveAndLoadNew()
        }
        stepLayout.addView(text)
        triggerLayout.addView(button)
    }

    private fun generateText(label: String?): TextView {
        val text = TextView(context)
        val layoutParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
        layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT)
        text.layoutParams = layoutParams
        text.setTextColor(Color.WHITE)
        text.gravity = Gravity.CENTER
        text.text = label
        return text
    }

    private fun generateButton(label: String?): Button {
        val button = Button(context)
        val layoutParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
        layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT)
        DipHelper.get(resources).setMargin(layoutParams, 10f)
        button.layoutParams = layoutParams
        button.gravity = Gravity.CENTER
        button.text = label
        return button
    }

    private fun loadNextStep() {
        if (state != WalkthroughState.STARTED){
            walkthrough.addStep(if (first is AbstractStep) ((first as AbstractStep).withTime(getTime())) else if (second is AbstractStep) ((second as AbstractStep).withTime(getTime())) else null)
            first = path?.get(0)?.getNextElement(second)
            second = path?.get(0)?.getNextElement(first)
        }
        state = WalkthroughState.PLAYING
        prepareLayout()
    }

    private fun saveAndLoadNew() {
        //TODO: Save
        stopFunction()
    }

    fun setInfoActive(active: Boolean) {
        if (active){
            infoTime = System.currentTimeMillis()
            backupState = state
            state = WalkthroughState.INFO
        }else{
            walkthrough.setInfoTime(System.currentTimeMillis()-infoTime)
            infoTime = 0
            state = backupState

        }
    }

}