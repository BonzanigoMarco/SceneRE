package uzh.scenere.views

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.widget.*
import uzh.scenere.R
import uzh.scenere.const.Constants.Companion.ANONYMOUS
import uzh.scenere.const.Constants.Companion.NOTHING
import uzh.scenere.const.Constants.Companion.USER_NAME
import uzh.scenere.datamodel.*
import uzh.scenere.datamodel.ContextObject.NullContextObject
import uzh.scenere.datamodel.Resource.NullResource
import uzh.scenere.datamodel.steps.AbstractStep
import uzh.scenere.datamodel.steps.StandardStep
import uzh.scenere.datamodel.trigger.AbstractTrigger
import uzh.scenere.datamodel.trigger.direct.ButtonTrigger
import uzh.scenere.datamodel.trigger.direct.IfElseTrigger
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
    private val paths: HashMap<Int, Path>? = scenario.getAllPaths(stakeholder)
    private var first = paths?.get(layer)?.getStartingPoint()
    private var second = paths?.get(layer)?.getNextElement(first)
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

        addView(stepLayout)
        addView(triggerLayout)

        if (first == null && second == null) {
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
            loadNextStep("Start Scenario")
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
                            loadNextStep("Button")
                        }
                        triggerLayout.addView(button)
                    }
                    is IfElseTrigger -> {
                        val title = "Question [" + (second as IfElseTrigger).getOptions().size + " Option(s)]:"
                        val questionText = generateText(title, (second as IfElseTrigger).text, ArrayList(), arrayListOf(title))
                        questionText.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE)
                        questionText.id = View.generateViewId()
                        triggerLayout.addView(questionText)
                        val scroll = SreScrollView(context,triggerLayout)
                        scroll.addRule(RelativeLayout.BELOW, questionText.id)
                        var optionId = 0
                        for (option in (second as IfElseTrigger).getOptions()){
                            val button = generateButton(option)
                            button.addRule(RelativeLayout.BELOW, id)
                            id = View.generateViewId()
                            button.id = id
                            val optionLayer = (second as IfElseTrigger).getLayerForOption(optionId++)
                            button.setOnClickListener {
                                layer = optionLayer
                                loadNextStep("Button: ${button.text}",optionLayer != 0)
                            }
                            scroll.addScrollElement(button)
                        }
                        triggerLayout.addView(scroll)
                    }
                    else -> {
                        //FALLBACK FOR MISSING TRIGGER AT THE END
                        val button = generateButton(context.getString(R.string.walkthrough_complete))
                        button.setOnClickListener {
                            loadNextStep("Automatic")
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


    private fun generateText(title: String?, content: String?): TextView {
        return generateText(title,content, ArrayList(), ArrayList())
    }

    private fun <T: Serializable> generateText(title: String?, content: String?, contextObjects: ArrayList<T>, boldWords: ArrayList<String>): SreTextView {
        val text = SreContextAwareTextView(context,stepLayout, boldWords,contextObjects)
        text.addRule(RelativeLayout.CENTER_IN_PARENT)
        text.text = StringHelper.fromHtml("$title<br>$content")
        text.setMargin(DipHelper.get(resources).dip10.toInt())
        text.setPadding(DipHelper.get(resources).dip5.toInt())
        return text
    }

    private fun generateButton(label: String?): SreButton {
        val button = SreButton(context,triggerLayout,label)
        button.addRule(RelativeLayout.CENTER_IN_PARENT)
        return button
    }

    private fun loadNextStep(info: String, pathSwitch: Boolean = false) {
        walkthrough.addTriggerInfo(getCurrentStep(),info,getCurrentTrigger())
        if (state != WalkthroughState.STARTED){
            walkthrough.addStep(getCurrentStep()?.withTime(getTime()))
            first = if (pathSwitch) paths?.get(layer)?.getStartingPoint() else paths?.get(layer)?.getNextElement(second)
            second = paths?.get(layer)?.getNextElement(first)
        }
        state = WalkthroughState.PLAYING
        prepareLayout()
    }

    private fun getCurrentTrigger(): AbstractTrigger? = if (first is AbstractTrigger) (first as AbstractTrigger) else if (second is AbstractTrigger) (second as AbstractTrigger) else null
    private fun getCurrentStep(): AbstractStep? = if (first is AbstractStep) (first as AbstractStep) else if (second is AbstractStep) (second as AbstractStep) else null

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

    fun getContextObjects(): ArrayList<AbstractObject> {
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
            if (obj !is NullContextObject && obj !is NullResource){
                list.add(obj.name)
            }
        }
        return list.toTypedArray()
    }
}