package uzh.scenere.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import uzh.scenere.R
import uzh.scenere.const.Constants.Companion.ANONYMOUS
import uzh.scenere.const.Constants.Companion.NOTHING
import uzh.scenere.const.Constants.Companion.USER_NAME
import uzh.scenere.datamodel.Object
import uzh.scenere.datamodel.Path
import uzh.scenere.datamodel.Scenario
import uzh.scenere.datamodel.Stakeholder
import uzh.scenere.datamodel.Walkthrough
import uzh.scenere.datamodel.steps.AbstractStep
import uzh.scenere.datamodel.steps.StandardStep
import uzh.scenere.datamodel.trigger.direct.ButtonTrigger
import uzh.scenere.helpers.DatabaseHelper
import uzh.scenere.helpers.DipHelper
import uzh.scenere.helpers.StringHelper
import java.io.Serializable

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
    private val walkthrough: Walkthrough = Walkthrough.WalkthroughBuilder(DatabaseHelper.getInstance(context).read(USER_NAME,String::class, ANONYMOUS), scenario.id, stakeholder.id).build()
    fun getWalkthrough(): Walkthrough{
        return walkthrough
    }
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

//        stepLayout.setBackgroundColor(Color.BLUE)
//        triggerLayout.setBackgroundColor(Color.YELLOW)

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
        val text = generateText("Intro", scenario.intro, ArrayList(),arrayListOf("Intro"))
        val button = generateButton("Start Scenario")
        button.setOnClickListener {
            Walkthrough.WalkthroughProperty.INTRO_TIME.set(getTime())
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
                        val title = StringHelper.nvl((first as StandardStep).title, NOTHING)
                        val text = generateText(title,(first as StandardStep).text, (first as StandardStep).objects,arrayListOf(title))
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
                    else -> {
                        //FALLBACK FOR MISSING TRIGGER AT THE END
                        val button = generateButton(context.getString(R.string.walkthrough_complete))
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
        val content = scenario.outro + "<br>" + walkthrough.printStatistics()
        val cutHtml = StringHelper.cutHtmlAfter(content, 10, context.getString(R.string.walkthrough_see_more))
        val text = generateText("Outro", cutHtml, ArrayList(), arrayListOf("Outro","Statistics"))
        val button = generateButton("Finish Scenario")
        button.setOnClickListener {
            saveAndLoadNew()
        }
        stepLayout.addView(text)
        triggerLayout.addView(button)
    }

    private fun <T: Serializable>generateText(title: String?, content: String?, contextObjects: ArrayList<T>, boldWords: ArrayList<String>): TextView {
        val text = SreContextAwareTextView(context,stepLayout, boldWords,contextObjects)
        text.addRule(RelativeLayout.CENTER_IN_PARENT)
        text.text = StringHelper.fromHtml("$title<br>$content")
        return text
    }

    private fun generateButton(label: String?): Button {
        val button = SreButton(context,triggerLayout,label)
        button.addRule(RelativeLayout.CENTER_IN_PARENT)
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
        walkthrough.toXml(context)
        stopFunction()
    }

    fun setInfoActive(active: Boolean) {
        if (active){
            infoTime = System.currentTimeMillis()
            backupState = state
            state = WalkthroughState.INFO
        }else{
            Walkthrough.WalkthroughProperty.INFO_TIME.set(Walkthrough.WalkthroughProperty.INFO_TIME.get(Long::class)+System.currentTimeMillis()-infoTime)
            infoTime = 0
            state = backupState

        }
    }

    fun getContextObjects(): ArrayList<Object> {
        if (first is AbstractStep){
            return (first as AbstractStep).objects
        }else if (second is AbstractStep){
            return (second as AbstractStep).objects
        }
        return ArrayList()
    }

    fun getObjectNames(vararg additionalName: String): Array<String>{
        val list = ArrayList<String>()
        list.addAll(additionalName)
        for (obj in getContextObjects()){
            list.add(obj.name)
        }
        return list.toTypedArray()
    }
}