package uzh.scenere.views

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.os.Handler
import android.support.v4.content.ContextCompat
import android.view.View
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import uzh.scenere.R
import uzh.scenere.const.Constants.Companion.ANONYMOUS
import uzh.scenere.const.Constants.Companion.DASH
import uzh.scenere.const.Constants.Companion.FIVE_MIN_MS
import uzh.scenere.const.Constants.Companion.NOTHING
import uzh.scenere.const.Constants.Companion.TEN_SEC_MS
import uzh.scenere.const.Constants.Companion.USER_NAME
import uzh.scenere.datamodel.*
import uzh.scenere.datamodel.ContextObject.NullContextObject
import uzh.scenere.datamodel.Resource.NullResource
import uzh.scenere.datamodel.steps.AbstractStep
import uzh.scenere.datamodel.steps.StandardStep
import uzh.scenere.datamodel.trigger.AbstractTrigger
import uzh.scenere.datamodel.trigger.communication.BluetoothTrigger
import uzh.scenere.datamodel.trigger.communication.GpsTrigger
import uzh.scenere.datamodel.trigger.communication.NfcTrigger
import uzh.scenere.datamodel.trigger.communication.WifiTrigger
import uzh.scenere.datamodel.trigger.direct.*
import uzh.scenere.datamodel.trigger.indirect.CallTrigger
import uzh.scenere.datamodel.trigger.indirect.SmsTrigger
import uzh.scenere.datamodel.trigger.indirect.SoundTrigger
import uzh.scenere.helpers.CommunicationHelper
import uzh.scenere.helpers.DatabaseHelper
import uzh.scenere.helpers.DipHelper
import uzh.scenere.helpers.StringHelper
import java.io.Serializable

@SuppressLint("ViewConstructor")
class WalkthroughPlayLayout(context: Context, private val scenario: Scenario, private val stakeholder: Stakeholder, private val nextStepFunction: () -> Unit, private val stopFunction: () -> Unit,  private val notify: ((String) -> Unit)) : LinearLayout(context) {

    private val stepLayout: RelativeLayout = RelativeLayout(context)
    private val triggerLayout: RelativeLayout = RelativeLayout(context)

    enum class WalkthroughPlayMode {
        STEP_INDUCED, TRIGGER_INDUCED
    }

    enum class WalkthroughState {
        STARTED, PLAYING, INFO, WHAT_IF, INPUT, FINISHED
    }

    //Statistics
    private val walkthrough: Walkthrough = Walkthrough.WalkthroughBuilder(DatabaseHelper.getInstance(context).read(USER_NAME,String::class, ANONYMOUS), scenario.id, stakeholder.id).build()
    fun getWalkthrough(): Walkthrough{
        return walkthrough
    }
    private var startingTime: Long = System.currentTimeMillis()
    private fun getTime(): Long{
        val time = (System.currentTimeMillis() - startingTime)/1000
        startingTime = System.currentTimeMillis()
        return time
    }
    private var infoTime: Long = 0
    private var whatIfTime: Long = 0
    private var inputTime: Long = 0
    //Play
    private var layer: Int = 0
    private val paths: HashMap<Int, Path>? = scenario.getAllPaths(stakeholder)
    private var first = paths?.get(layer)?.getStartingPoint()
    private var second = paths?.get(layer)?.getNextElement(first)
    //Input
    private val comments = ArrayList<String>()
    //State
    private val mode: WalkthroughPlayMode = if (first is AbstractStep) WalkthroughPlayMode.STEP_INDUCED else WalkthroughPlayMode.TRIGGER_INDUCED
    private var backupState: WalkthroughState = WalkthroughState.STARTED
    var state: WalkthroughState = WalkthroughState.STARTED
    //Update
    private var refresh = false

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
        val text = generateText(context.getString(R.string.walkthrough_intro), scenario.intro, ArrayList(),arrayListOf("Intro"))
        val button = generateButton(context.getString(R.string.walkthrough_start_scenario))
        button.setExecutable {
            Walkthrough.WalkthroughProperty.INTRO_TIME.set(getTime())
            loadNextStep(context.getString(R.string.walkthrough_start_scenario))
        }
        stepLayout.addView(text)
        triggerLayout.addView(button)
        nextStepFunction()
    }


    fun resolveStepAndTrigger() {
        triggerLayout.removeAllViews()
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
                        button.setExecutable {
                            loadNextStep(context.getString(R.string.walkthrough_transition_button))
                        }
                        triggerLayout.addView(button)
                    }
                    is IfElseTrigger -> {
                        val title = context.getString(R.string.walkthrough_question_option,(second as IfElseTrigger).getOptions().size)
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
                            button.setExecutable {
                                layer = optionLayer
                                loadNextStep( context.getString(R.string.walkthrough_transition_button_x,button.text),optionLayer != 0)
                            }
                            scroll.addScrollElement(button)
                        }
                        triggerLayout.addView(scroll)
                    }
                    is StakeholderInteractionTrigger -> {
                        val interactedStakeholder = DatabaseHelper.getInstance(context).read((second as StakeholderInteractionTrigger).interactedStakeholderId!!, Stakeholder::class)
                        if (interactedStakeholder !is Stakeholder.NullStakeholder) {
                            val text = (second as StakeholderInteractionTrigger).text!!
                            val name = interactedStakeholder.name
                            val code = generateCode(stakeholder)
                            val title = context.getString(R.string.walkthrough_interaction_text,text,name,code)
                            val titleText = generateText(null, title, ArrayList(), arrayListOf(context.getString(R.string.walkthrough_interaction_code),text, name, code))
                            val codeInput = generateEditText(context.getString(R.string.walkthrough_input_code))
                            codeInput.setMargin(DipHelper.get(resources).dip10)
                            val button = generateButton(context.getString(R.string.walkthrough_check_code))
                            refresh = true
                            refresh({
                                val t = (second as StakeholderInteractionTrigger).text!!
                                val n = interactedStakeholder.name
                                val c = generateCode(stakeholder)
                                val txt = context.getString(R.string.walkthrough_interaction_text, t, n, c)
                                titleText.setTextWithNewBoldWords(txt,c)
                            })
                            button.setExecutable {
                                if (codeInput.text.toString() == generateCode(interactedStakeholder)){
                                    loadNextStep(context.getString(R.string.walkthrough_transition_interaction))
                                    refresh = false
                                    notify.invoke(context.getString(R.string.walkthrough_correct_code))
                                }else{
                                    notify.invoke(context.getString(R.string.walkthrough_incorrect_code))
                                    codeInput.text = null
                                }
                            }
                            val scroll = SreScrollView(context,triggerLayout)
                            scroll.addScrollElement(titleText)
                            scroll.addScrollElement(codeInput)
                            scroll.addScrollElement(button)
                            triggerLayout.addView(scroll)
                        }
                    }
                    is InputTrigger -> {
                        val text = (second as InputTrigger).text!!
                        val input = (second as InputTrigger).input!!
                        val titleText = generateText(null, context.getString(R.string.walkthrough_input_text,text), ArrayList(), arrayListOf(text))
                        val inputField = generateEditText(context.getString(R.string.walkthrough_input_here))
                        inputField.setMargin(DipHelper.get(resources).dip10)
                        val button = generateButton(context.getString(R.string.walkthrough_check_input))
                        button.setExecutable {
                            if (inputField.text.toString() == input){
                                loadNextStep(context.getString(R.string.walkthrough_transition_input))
                                refresh = false
                                notify.invoke(context.getString(R.string.walkthrough_input_correct))
                            }else{
                                notify.invoke(context.getString(R.string.walkthrough_input_incorrect))
                                inputField.text = null
                            }
                        }
                        val scroll = SreScrollView(context,triggerLayout)
                        scroll.addScrollElement(titleText)
                        scroll.addScrollElement(inputField)
                        scroll.addScrollElement(button)
                        triggerLayout.addView(scroll)
                    }
                    is NfcTrigger -> {
                        val text = (second as NfcTrigger).text!!
                        val titleText = generateText(null, context.getString(R.string.walkthrough_nfc_text,text), ArrayList(), arrayListOf(text))
                        val nfcSupported = CommunicationHelper.supports(context, CommunicationHelper.Companion.Communications.NFC)
                        val nfcOn = CommunicationHelper.check(context, CommunicationHelper.Companion.Communications.NFC)
                        val scroll = SreScrollView(context,triggerLayout)
                        scroll.addScrollElement(titleText)
                        if (!nfcSupported || !nfcOn){
                            val state = if (!nfcSupported) context.getString(R.string.x_not_supported) else context.getString(R.string.x_disabled)
                            val alertText = generateText(null, "NFC $state!",arrayListOf(state),ArrayList())
                            val button = generateButton(context.getString(if (!nfcSupported) R.string.walkthrough_end_scenario else R.string.walkthrough_enable_nfc))
                            button.setExecutable {
                                if (!nfcSupported){
                                    saveAndLoadNew(true,context.getString(R.string.walkthrough_final_state_cancelled_nfc, StringHelper.numberToPositionString(Walkthrough.WalkthroughProperty.STEP_ID_LIST.getAll(String::class).size+1),getCurrentStep()?.title))
                                }else{
                                    CommunicationHelper.toggle(context, CommunicationHelper.Companion.Communications.NFC)
                                }
                            }
                            scroll.addScrollElement(alertText)
                            scroll.addScrollElement(button)
                        }
                        triggerLayout.addView(scroll)
                    }
                    is TimeTrigger -> {/*TODO*/
                    }
                    is SoundTrigger -> {/*TODO*/
                    }
                    is BluetoothTrigger -> {/*TODO*/
                    }
                    is WifiTrigger -> {/*TODO*/
                    }
                    is GpsTrigger -> {/*TODO*/
                    }
                    is CallTrigger -> {/*TODO*/
                    }
                    is SmsTrigger -> {/*TODO*/
                    }
                    else -> {
                        //FALLBACK
                        if (first != null){
                            val button = generateButton(context.getString(R.string.walkthrough_complete))
                            button.setExecutable {
                                loadNextStep(context.getString(R.string.walkthrough_transition_automatic))
                            }
                            triggerLayout.addView(button)
                        }else{
                            resolveOutro()
                        }
                    }
                }
            }
            WalkthroughPlayMode.TRIGGER_INDUCED -> {

            }
        }
    }

    private fun generateCode(stakeholder: Stakeholder): String {
        val ms = System.currentTimeMillis().div(FIVE_MIN_MS).toString()
        val minutePart = ms.substring(ms.length-2).toInt() // Changes all 5 Minutes, Max Val = 99
        val offset = minutePart/4
        val base = scenario.id.replace(DASH,NOTHING).substring(offset,offset+2).plus(stakeholder.id.replace(DASH,NOTHING).substring(offset, offset+2)) //ID Length = 32
        return base.plus(minutePart)
    }

    private fun refresh(function: () -> Unit, refreshRate: Long = TEN_SEC_MS) {
        Handler().postDelayed({
            if (refresh) {
                function()
                Handler().postDelayed({refresh(function)},refreshRate)
            }
        }, refreshRate)
    }

    private fun resolveOutro() {
        val content = context.getString(R.string.walkthrough_concat_br,scenario.outro,walkthrough.printStatistics())
        val cutHtml = StringHelper.cutHtmlAfter(content, 10, context.getString(R.string.walkthrough_see_more))
        val text = generateText(context.getString(R.string.walkthrough_outro), cutHtml, ArrayList(), arrayListOf(context.getString(R.string.walkthrough_outro),context.getString(R.string.walkthrough_statistics)))
        val button = generateButton(context.getString(R.string.walkthrough_finish_scenario))
        button.setExecutable {
            saveAndLoadNew()
        }
        stepLayout.addView(text)
        triggerLayout.addView(button)
    }

    private fun generateText(title: String?, content: String?): TextView {
        return generateText(title,content, ArrayList(), ArrayList())
    }

    private fun <T: Serializable> generateText(title: String?, content: String?, contextObjects: ArrayList<T>, boldWords: ArrayList<String>): SreContextAwareTextView {
        val text = SreContextAwareTextView(context,stepLayout, boldWords,contextObjects)
        text.addRule(RelativeLayout.CENTER_IN_PARENT)
        if (title == null){
            text.text = StringHelper.fromHtml(content)
        }else if (content == null){
            text.text = StringHelper.fromHtml(title)
        }else{
            text.text = StringHelper.fromHtml(context.getString(R.string.walkthrough_concat_br,title,content))
        }
        text.setMargin(DipHelper.get(resources).dip10)
        text.setPadding(DipHelper.get(resources).dip5)
        return text
    }

    private fun generateEditText(title: String?): SreEditText {
        val text = SreEditText(context,stepLayout,null,title)
        text.addRule(RelativeLayout.CENTER_IN_PARENT)
        text.setTextColor(ColorStateList.valueOf(ContextCompat.getColor(context,R.color.srePrimaryPastel)))
        text.setPadding(DipHelper.get(resources).dip5)
        text.setSingleLine(true)
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
            walkthrough.addStep(getCurrentStep()?.withTime(getTime())?.withComments(comments))
            comments.clear()
            first = if (pathSwitch) paths?.get(layer)?.getStartingPoint() else paths?.get(layer)?.getNextElement(second)
            second = paths?.get(layer)?.getNextElement(first)
        }
        state = WalkthroughState.PLAYING
        prepareLayout()
        nextStepFunction()
    }

    private fun getCurrentTrigger(): AbstractTrigger? = if (first is AbstractTrigger) (first as AbstractTrigger) else if (second is AbstractTrigger) (second as AbstractTrigger) else null
    private fun getCurrentStep(): AbstractStep? = if (first is AbstractStep) (first as AbstractStep) else if (second is AbstractStep) (second as AbstractStep) else null

    fun execUseNfcData(data: String): String?{
        if (data == getCurrentTrigger()?.id){
            val message = (getCurrentTrigger() as NfcTrigger).message
            loadNextStep(context.getString(R.string.walkthrough_transition_nfc_scanned))
            return message
        }
        return context.getString(R.string.walkthrough_nfc_tag_wrong)
    }


    fun saveAndLoadNew(interrupted: Boolean = false, reason: String? = null) {
        if (interrupted){
            walkthrough.addStep(getCurrentStep()?.withTime(getTime())?.withComments(comments))
            Walkthrough.WalkthroughProperty.FINAL_STATE.set(reason?: context.getString(R.string.walkthrough_final_state_cancelled, StringHelper.numberToPositionString(Walkthrough.WalkthroughProperty.STEP_ID_LIST.getAll(String::class).size+1),getCurrentStep()?.title))
        }else{
            Walkthrough.WalkthroughProperty.FINAL_STATE.set(context.getString(R.string.walkthrough_final_state_complete))
        }
        walkthrough.toXml(context)
        stopFunction()
    }

    fun resetActiveness() {
        when (state){
            WalkthroughState.INFO -> {setInfoActive(false)}
            WalkthroughState.WHAT_IF -> {setWhatIfActive(false)}
            WalkthroughState.INPUT -> {setInputActive(false)}
            else -> {
            }
        }
    }

    fun setInfoActive(active: Boolean) {
        if (active){
            infoTime = System.currentTimeMillis()
            backupState = state
            state = WalkthroughState.INFO
        }else{
            Walkthrough.WalkthroughProperty.INFO_TIME.set(Walkthrough.WalkthroughProperty.INFO_TIME.get(Long::class)+(System.currentTimeMillis()-infoTime)/1000)
            infoTime = 0
            state = backupState
        }
    }

    fun setWhatIfActive(active: Boolean) {
        if (active){
            whatIfTime = System.currentTimeMillis()
            backupState = state
            state = WalkthroughState.WHAT_IF
        }else{
            Walkthrough.WalkthroughProperty.WHAT_IF_TIME.set(Walkthrough.WalkthroughProperty.WHAT_IF_TIME.get(Long::class)+(System.currentTimeMillis()-whatIfTime)/1000)
            whatIfTime = 0
            state = backupState
        }
    }

    fun setInputActive(active: Boolean) {
        if (active){
            inputTime = System.currentTimeMillis()
            backupState = state
            state = WalkthroughState.INPUT
        }else{
            Walkthrough.WalkthroughProperty.INPUT_TIME.set(Walkthrough.WalkthroughProperty.INPUT_TIME.get(Long::class)+(System.currentTimeMillis()-inputTime)/1000)
            inputTime = 0
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

    fun getActiveWhatIfs(): ArrayList<String> {
        if (first is AbstractStep){
            return (first as AbstractStep).whatIfs
        }
        if (second is AbstractStep){
            return (second as AbstractStep).whatIfs
        }
        return ArrayList()
    }

    fun removeComment(comment: String) {
        comments.remove(comment)
    }

    fun addComment(comment: String) {
        comments.add(comment)
    }

    fun getComments(): Array<String>{
        return comments.toTypedArray()
    }
}