package uzh.scenere.views

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.res.ColorStateList
import android.net.wifi.ScanResult
import android.os.Handler
import android.support.v4.content.ContextCompat
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import uzh.scenere.R
import uzh.scenere.activities.WalkthroughActivity
import uzh.scenere.const.Constants.Companion.ANONYMOUS
import uzh.scenere.const.Constants.Companion.DASH
import uzh.scenere.const.Constants.Companion.FIVE_MIN_MS
import uzh.scenere.const.Constants.Companion.FIVE_SEC_MS
import uzh.scenere.const.Constants.Companion.NOTHING
import uzh.scenere.const.Constants.Companion.ONE_SEC_MS
import uzh.scenere.const.Constants.Companion.TEN_SEC_MS
import uzh.scenere.const.Constants.Companion.TWO_SEC_MS
import uzh.scenere.const.Constants.Companion.USER_NAME
import uzh.scenere.datamodel.*
import uzh.scenere.datamodel.ContextObject.NullContextObject
import uzh.scenere.datamodel.Resource.NullResource
import uzh.scenere.datamodel.steps.*
import uzh.scenere.datamodel.trigger.AbstractTrigger
import uzh.scenere.datamodel.trigger.communication.BluetoothTrigger
import uzh.scenere.datamodel.trigger.communication.GpsTrigger
import uzh.scenere.datamodel.trigger.communication.NfcTrigger
import uzh.scenere.datamodel.trigger.communication.WifiTrigger
import uzh.scenere.datamodel.trigger.direct.*
import uzh.scenere.datamodel.trigger.indirect.CallTrigger
import uzh.scenere.datamodel.trigger.indirect.SmsTrigger
import uzh.scenere.datamodel.trigger.indirect.SoundTrigger
import uzh.scenere.datastructures.PlayState
import uzh.scenere.helpers.*
import uzh.scenere.listener.SreSoundChangeListener
import java.io.Serializable
import android.os.VibrationEffect
import android.os.Build
import android.os.Vibrator
import uzh.scenere.const.Constants.Companion.HALF_SEC_MS
import android.media.ToneGenerator
import android.media.AudioManager

@SuppressLint("ViewConstructor")
class WalkthroughPlayLayout(context: Context, private var scenario: Scenario, private var stakeholder: Stakeholder, private var nextStepFunction: () -> Unit, private var stopFunction: () -> Unit,  private var notify: ((String) -> Unit)) : LinearLayout(context), Serializable {


    companion object {
        fun saveState(play: WalkthroughPlayLayout): PlayState {
            return PlayState(
                    play.scenario,
                    play.stakeholder,
                    play.walkthrough,
                    play.startingTime,
                    play.infoTime,
                    play.whatIfTime,
                    play.inputTime,
                    play.layer,
                    play.paths,
                    play.first,
                    play.second,
                    play.comments,
                    play.mode,
                    play.backupState,
                    play.state,
                    play.refresh,
                    play.wifiDiscovered)
        }

        fun loadState(play: WalkthroughPlayLayout, playState: PlayState){
            play.scenario = playState.scenario
            play.stakeholder = playState.stakeholder
            play.walkthrough = playState.walkthrough
            play.startingTime = playState.startingTime
            play.infoTime = playState.infoTime
            play.whatIfTime = playState.whatIfTime
            play.inputTime = playState.inputTime
            play.layer = playState.layer
            play.paths = playState.paths
            play.first = playState.first
            play.second = playState.second
            play.comments = playState.comments
            play.mode = playState.mode
            play.backupState = playState.backupState
            play.state = playState.state
            play.refresh = playState.refresh
            play.wifiDiscovered = playState.wifiDiscovered
        }
    }

    private val stepLayout: RelativeLayout = SreRelativeLayout(context)
    private val triggerLayout: RelativeLayout = SreRelativeLayout(context)

    enum class WalkthroughPlayMode {
        STEP_INDUCED, TRIGGER_INDUCED
    }

    enum class WalkthroughState {
        STARTED, PLAYING, INFO, WHAT_IF, INPUT, FINISHED
    }

    //Statistics
    private var walkthrough: Walkthrough = Walkthrough.WalkthroughBuilder(DatabaseHelper.getInstance(context).read(USER_NAME,String::class, ANONYMOUS), scenario.id, stakeholder).build()
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
    private var paths: HashMap<Int, Path>? = scenario.getAllPaths(stakeholder)
    private var first = paths?.get(layer)?.getStartingPoint()
    private var second = paths?.get(layer)?.getNextElement(first)
    //Input
    private var comments = ArrayList<String>()
    //State
    private var mode: WalkthroughPlayMode = if (first is AbstractStep) WalkthroughPlayMode.STEP_INDUCED else WalkthroughPlayMode.TRIGGER_INDUCED
    private var backupState: WalkthroughState = WalkthroughState.STARTED
    var state: WalkthroughState = WalkthroughState.STARTED
    //Update
    private var refresh = false
    private var wifiDiscovered = false
    private var bluetoothDiscovered = false
    //Link
    private lateinit var activityLink: WalkthroughActivity
    //Progress
    private var loadingBar: SreLoadingBar? = null

    fun prepareLayout(): WalkthroughPlayLayout {
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
        return this
    }

    private fun resolveIntro() {
        val text = generateText(context.getString(R.string.walkthrough_initiation),context.getString(R.string.walkthrough_intro_text,stakeholder.name,stakeholder.description,scenario.intro),
                ArrayList(),arrayListOf(stakeholder.name,context.getString(R.string.walkthrough_initiation),context.getString(R.string.walkthrough_description),context.getString(R.string.walkthrough_introduction)))
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
                    is JumpStep -> {
                        val title = StringHelper.nvl((first as JumpStep).title, NOTHING)
                        val text = generateText(title,(first as JumpStep).text, (first as JumpStep).objects,arrayListOf(title))
                        stepLayout.addView(text)
                    }
                    is SoundStep -> {
                        val title = StringHelper.nvl((first as SoundStep).title, NOTHING)
                        val text = generateText(title,(first as SoundStep).text, (first as SoundStep).objects,arrayListOf(title))
                        stepLayout.addView(text)
                        val toneGen1 = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
                        toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP, 150)
                    }
                    is VibrationStep -> {
                        val title = StringHelper.nvl((first as VibrationStep).title, NOTHING)
                        val text = generateText(title,(first as VibrationStep).text, (first as VibrationStep).objects,arrayListOf(title))
                        stepLayout.addView(text)
                        val v = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            v.vibrate(VibrationEffect.createOneShot(HALF_SEC_MS, VibrationEffect.DEFAULT_AMPLITUDE))
                        } else {
                            v.vibrate(HALF_SEC_MS)
                        }
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
                        val nfcOn = CommunicationHelper.check(activityLink, CommunicationHelper.Companion.Communications.NFC)
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
                                    CommunicationHelper.toggle(activityLink, CommunicationHelper.Companion.Communications.NFC)
                                }
                            }
                            scroll.addScrollElement(alertText)
                            scroll.addScrollElement(button)
                        }
                        triggerLayout.addView(scroll)
                    }
                    is TimeTrigger -> {
                        val text = (second as TimeTrigger).text!!
                        val timeMs = (second as TimeTrigger).timeMs!!
                        val endTime = System.currentTimeMillis()+timeMs
                        val msToFormattedString = StringHelper.msToFormattedString(timeMs)
                        val titleText = generateText(null, context.getString(R.string.walkthrough_timer_text,text,msToFormattedString), ArrayList(), arrayListOf(text,msToFormattedString))
                        val scroll = SreScrollView(context,triggerLayout)
                        scroll.addScrollElement(titleText)
                        refresh = true
                        var timerUp = false
                        refresh({
                            if (!timerUp && System.currentTimeMillis() > endTime){
                                notify(context.getString(R.string.walkthrough_timer_expired))
                                loadNextStep(context.getString(R.string.walkthrough_transition_timer))
                                timerUp = true
                            }else{
                                val remainingTime = StringHelper.msToFormattedString(endTime-System.currentTimeMillis())
                                titleText.setTextWithNewBoldWords(context.getString(R.string.walkthrough_timer_text,text,remainingTime), text, remainingTime)
                            }
                        },ONE_SEC_MS)
                        triggerLayout.addView(scroll)
                        activityLink.registerSmsListener()
                    }
                    is SoundTrigger -> {/*TODO*/
                        val text = (second as SoundTrigger).text!!
                        val threshold = (second as SoundTrigger).dB!!
                        val calibrating = context.getString(R.string.walkthrough_calibrating)
                        val titleText = generateText(null, context.getString(R.string.walkthrough_sound_text,text,threshold.toString(), calibrating), ArrayList(), arrayListOf(text,threshold.toString(), calibrating))
                        val scroll = SreScrollView(context,triggerLayout)
                        scroll.addScrollElement(titleText)
                        //Audio
                        val recorder = SreSoundChangeListener(activityLink)
                        if (!recorder.audioAllowed) {
                            val state = if (recorder.audioPossible) context.getString(R.string.x_not_granted) else context.getString(R.string.x_not_supported)
                            val alertText = generateText(null, "Audio $state!", arrayListOf(state), ArrayList())
                            val button = generateButton(context.getString(R.string.walkthrough_grant_permission))
                            button.setExecutable {
                                if (recorder.audioPossible){
                                    PermissionHelper.request(activityLink, PermissionHelper.Companion.PermissionGroups.AUDIO)
                                }else{
                                    saveAndLoadNew(true,context.getString(R.string.walkthrough_final_state_cancelled_audio, StringHelper.numberToPositionString(Walkthrough.WalkthroughProperty.STEP_ID_LIST.getAll(String::class).size+1),getCurrentStep()?.title))
                                }
                            }
                            scroll.addScrollElement(alertText)
                            scroll.addScrollElement(button)
                        }
                        val calibrationButton = generateButton(context.getString(R.string.walkthrough_calibrate_sound))
                        calibrationButton.setExecutable {
                            refresh = false
                            titleText.setTextWithNewBoldWords(context.getString(R.string.walkthrough_sound_text,text,threshold.toString(),calibrating),text,threshold.toString(),calibrating)
                            recorder.calibrate()
                            refresh = true
                        }
                        scroll.addScrollElement(calibrationButton)
                        refresh = true
                        var init = true
                        refresh({
                            if (init) {
                                recorder.start()
                                init = false
                            }
                            val amplitude = recorder.getAmplitude()
                            if (threshold < 0 && amplitude < threshold || threshold > 0 && amplitude > threshold) {
                                recorder.stop()
                                notify(context.getString(R.string.walkthrough_sound_threshold_reached))
                                loadNextStep(context.getString(R.string.walkthrough_transition_sound))
                            } else if (amplitude == 0.0) {
                                titleText.setTextWithNewBoldWords(context.getString(R.string.walkthrough_sound_text, text, threshold.toString(), calibrating), text, threshold.toString(), calibrating)
                            } else {
                                titleText.setTextWithNewBoldWords(context.getString(R.string.walkthrough_sound_text, text, threshold.toString(), "$amplitude dB"), text, threshold.toString(), amplitude.toString())
                            }
                        },300)
                        triggerLayout.addView(scroll)
                    }
                    is BluetoothTrigger -> {/*TODO*/
                        val text = (second as BluetoothTrigger).text!!
                        val deviceId = (second as BluetoothTrigger).deviceId!!
                        val titleText = generateText(null, context.getString(R.string.walkthrough_bluetooth_text,text,deviceId), ArrayList(), arrayListOf(text,deviceId))
                        val scroll = SreScrollView(context,triggerLayout)
                        scroll.addScrollElement(titleText)
                        val bluetoothAllowed = PermissionHelper.check(activityLink,PermissionHelper.Companion.PermissionGroups.BLUETOOTH)
                        if (!bluetoothAllowed){
                            val state = context.getString(R.string.x_not_granted)
                            val alertText = generateText(null, "Bluetooth $state!",arrayListOf(state),ArrayList())
                            val button = generateButton(context.getString(R.string.walkthrough_grant_permission))
                            button.setExecutable {
                                PermissionHelper.request(activityLink,PermissionHelper.Companion.PermissionGroups.BLUETOOTH)
                            }
                            scroll.addScrollElement(alertText)
                            scroll.addScrollElement(button)
                        }else{
                            bluetoothDiscovered = false
                            activityLink.registerBluetoothListener()
                        }
                        scroll.addScrollElement(ProgressBar(context))
                        triggerLayout.addView(scroll)
                    }
                    is WifiTrigger -> {/*TODO*/
                        val text = (second as WifiTrigger).text!!
                        val ssid = (second as WifiTrigger).ssidAndStrength!!
                        val titleText = generateText(null, context.getString(R.string.walkthrough_wifi_text,text), ArrayList(), arrayListOf(text,ssid))
                        val wifiOn = CommunicationHelper.check(activityLink, CommunicationHelper.Companion.Communications.WIFI)
                        val scroll = SreScrollView(context,triggerLayout)
                        scroll.addScrollElement(titleText)
                        if (!wifiOn){
                            val state = context.getString(R.string.x_disabled)
                            val alertText = generateText(null, "Wi-Fi $state!",arrayListOf(state),ArrayList())
                            val button = generateButton(context.getString(R.string.walkthrough_enable_wifi))
                            button.setExecutable {
                                CommunicationHelper.toggle(activityLink, CommunicationHelper.Companion.Communications.WIFI)
                            }
                            scroll.addScrollElement(alertText)
                            scroll.addScrollElement(button)
                        }
                        loadingBar = SreLoadingBar(context,scroll)
                        scroll.addScrollElement(loadingBar!!)
                        triggerLayout.addView(scroll)
                        wifiDiscovered = false
                        activityLink.startWifiScan()
                    }
                    is GpsTrigger -> {/*TODO*/
                        val text = (second as GpsTrigger).text!!
                        var state = context.getString(R.string.walkthrough_gps_evaluation)
                        val range = (second as GpsTrigger).getRadius()
                        val latitude = (second as GpsTrigger).getLatitudeDouble()
                        val longitude = (second as GpsTrigger).getLongitudeDouble()
                        var dots = "."
                        val titleText = generateText(null, context.getString(R.string.walkthrough_gps_text,text,range,state,dots), ArrayList(), arrayListOf(text,"$range m",state))
                        val gpsOn = CommunicationHelper.registerGpsListener(activityLink)
                        val gpsAllowed = PermissionHelper.check(context,PermissionHelper.Companion.PermissionGroups.GPS)
                        val scroll = SreScrollView(context,triggerLayout)
                        refresh = true
                        refresh({
                            val latitudeLongitude = CommunicationHelper.Companion.SreLocationListener.get().getLatitudeLongitude()
                            val newText = (second as GpsTrigger).text!!
                            val boldWords = arrayListOf(newText, "$range m")
                            dots = if (dots.length == 3) "." else "$dots."
                            if (latitudeLongitude.first != null){
                                val distanceInMeter = NumberHelper.getDistanceInMeter(latitude, longitude, latitudeLongitude.first!!, latitudeLongitude.second!!)
                                state = context.getString(R.string.walkthrough_gps_distance,distanceInMeter.toString())
                                if (distanceInMeter <= range){
                                    CommunicationHelper.unregisterGpsListener(activityLink)
                                    notify(context.getString(R.string.walkthrough_gps_destination_reached))
                                    loadNextStep(context.getString(R.string.walkthrough_gps_destination_reached))
                                }
                            }
                            boldWords.add(state)
                            titleText.setTextWithNewBoldWords(context.getString(R.string.walkthrough_gps_text,newText,range,state,dots),*boldWords.toTypedArray())
                        },TWO_SEC_MS)
                        scroll.addScrollElement(titleText)
                        if (!gpsOn){
                            val gpsState = context.getString(R.string.x_disabled)
                            val alertText = generateText(null, "GPS $gpsState!",arrayListOf(gpsState),ArrayList())
                            val button = generateButton(context.getString(R.string.walkthrough_enable_gps))
                            button.setExecutable {
                                if (gpsAllowed) {
                                    val toggle = CommunicationHelper.toggle(activityLink, CommunicationHelper.Companion.Communications.GPS)
                                    if (toggle) {
                                        scroll.removeScrollElement(button)
                                    }
                                }else{
                                    PermissionHelper.request(activityLink, PermissionHelper.Companion.PermissionGroups.GPS)
                                }
                            }
                            scroll.addScrollElement(alertText)
                            scroll.addScrollElement(button)
                        }
                        val openButton = generateButton(context.getString(R.string.walkthrough_open_location))
                        openButton.setExecutable {
                            context.startActivity(CommunicationHelper.getMapIntent(latitude,longitude))
                        }
                        scroll.addScrollElement(openButton)
                        triggerLayout.addView(scroll)
                    }
                    is CallTrigger -> {/*TODO*/
                        val text = (second as CallTrigger).text!!
                        val telephoneNr = (second as CallTrigger).telephoneNr!!
                        val titleText = generateText(null, context.getString(R.string.walkthrough_telephone_text,text,telephoneNr), ArrayList(), arrayListOf(text,telephoneNr))
                        val telephonyPossible = PermissionHelper.check(activityLink, PermissionHelper.Companion.PermissionGroups.TELEPHONY)
                        val scroll = SreScrollView(context,triggerLayout)
                        scroll.addScrollElement(titleText)
                        if (!telephonyPossible){
                            val state = context.getString(R.string.x_not_granted)
                            val alertText = generateText(null, "Telephony-Permissions $state!",arrayListOf(state),ArrayList())
                            val button = generateButton(context.getString(R.string.walkthrough_grant_permission))
                            button.setExecutable {
                                PermissionHelper.request(activityLink, PermissionHelper.Companion.PermissionGroups.TELEPHONY)
                            }
                            scroll.addScrollElement(alertText)
                            scroll.addScrollElement(button)
                        }
                        triggerLayout.addView(scroll)
                        activityLink.registerPhoneCallListener()
                    }
                    is SmsTrigger -> {/*TODO*/
                        val text = (second as SmsTrigger).text!!
                        val telephoneNr = (second as SmsTrigger).telephoneNr!!
                        val titleText = generateText(null, context.getString(R.string.walkthrough_sms_text,text,telephoneNr), ArrayList(), arrayListOf(text,telephoneNr))
                        val smsPossible = PermissionHelper.check(activityLink, PermissionHelper.Companion.PermissionGroups.SMS)
                        val scroll = SreScrollView(context,triggerLayout)
                        scroll.addScrollElement(titleText)
                        if (!smsPossible){
                            val state = context.getString(R.string.x_not_granted)
                            val alertText = generateText(null, "SMS-Permissions $state!",arrayListOf(state),ArrayList())
                            val button = generateButton(context.getString(R.string.walkthrough_grant_permission))
                            button.setExecutable {
                                PermissionHelper.request(activityLink, PermissionHelper.Companion.PermissionGroups.SMS)
                            }
                            scroll.addScrollElement(alertText)
                            scroll.addScrollElement(button)
                        }
                        triggerLayout.addView(scroll)
                        activityLink.registerSmsListener()
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
            if (refresh) {
                function()
                Handler().postDelayed({refresh(function,refreshRate)},refreshRate)
            }
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

    private fun generateText(title: String?, content: String?): SreTextView {
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
        text.setTextColor(ColorStateList.valueOf(getColorWithStyle(context,R.color.srePrimaryPastel)))
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
        onPause()
        val currentStep = getCurrentStep()
        walkthrough.addTriggerInfo(currentStep,info,getCurrentTrigger())
        if (state != WalkthroughState.STARTED){
            walkthrough.addStep(currentStep?.withTime(getTime())?.withComments(comments))
            comments.clear()
            if (currentStep is JumpStep){
                val (s,p) = scenario.getPathAndStepToStepId(stakeholder, currentStep.targetStepId!!)
                first = s!!
                layer = p!!.layer
            }else{
                first = if (pathSwitch) paths?.get(layer)?.getStartingPoint() else paths?.get(layer)?.getNextElement(second)
            }
            second = paths?.get(layer)?.getNextElement(first)
        }
        state = WalkthroughState.PLAYING
        prepareLayout()
        nextStepFunction()
    }

    private fun getCurrentTrigger(): AbstractTrigger? = if (first is AbstractTrigger) (first as AbstractTrigger) else if (second is AbstractTrigger) (second as AbstractTrigger) else null
    private fun getCurrentStep(): AbstractStep? = if (first is AbstractStep) (first as AbstractStep) else if (second is AbstractStep) (second as AbstractStep) else null

    fun execUseNfcData(data: String): String?{
        if (getCurrentTrigger() is NfcTrigger){
            if (data == getCurrentTrigger()?.id){
                val message = (getCurrentTrigger() as NfcTrigger).message
                loadNextStep(context.getString(R.string.walkthrough_transition_nfc_scanned))
                return message
            }
            return context.getString(R.string.walkthrough_nfc_tag_wrong)
        }
        return NOTHING
    }

    fun execUseWifiData(data: ScanResult){
        val step = getCurrentTrigger()
        if (step is WifiTrigger){
            val ssid = StringHelper.nvl(step.getSsid(),NOTHING)
            if (!wifiDiscovered && (data.SSID == ssid || data.SSID.matches(ssid.toRegex()))){
                val wifiStrength = CommunicationHelper.getWifiStrength(data.level).strength
                val targetStrength = CommunicationHelper.Companion.WiFiStrength.valueOf(step.getStrength()).strength
                loadingBar?.setProgress(100f/ targetStrength *wifiStrength)
                if (wifiStrength >= targetStrength) {
                    wifiDiscovered = true
                    notify("${data.SSID} discovered!")
                    loadingBar = null
                    activityLink.stopWifiScan()
                    loadNextStep(context.getString(R.string.walkthrough_transition_wifi_discovered))
                }
            }
        }else{
            activityLink.stopWifiScan()
        }
    }

    fun handlePhoneNumber(phoneNumber: String): Boolean {
        if (getCurrentTrigger() is CallTrigger){
            val telephoneNr = StringHelper.nvl((getCurrentTrigger() as CallTrigger).telephoneNr,NOTHING)
            if (StringHelper.hasText(telephoneNr) && (phoneNumber == telephoneNr || phoneNumber.matches(telephoneNr.toRegex()))){
                notify("Call from $phoneNumber received!")
                activityLink.unregisterPhoneCallListener()
                loadNextStep(context.getString(R.string.walkthrough_transition_call))
                return true
            }
        }
        return false
    }

    fun handleSmsData(phoneNumber: String, message: String): Boolean {
        if (getCurrentTrigger() is SmsTrigger){
            val telephoneNr = StringHelper.nvl((getCurrentTrigger() as SmsTrigger).telephoneNr,NOTHING)
            if (StringHelper.hasText(telephoneNr) && (phoneNumber == telephoneNr || phoneNumber.matches(telephoneNr.toRegex()))){
                notify("SMS from $phoneNumber received: $message")
                activityLink.unregisterSmsListener()
                loadNextStep(context.getString(R.string.walkthrough_transition_sms))
                return true
            }
        }
        return false
    }

    fun handleBluetoothData(devices: List<BluetoothDevice>): Boolean {
        if (!bluetoothDiscovered && getCurrentTrigger() is BluetoothTrigger) {
            for (device in devices) {
                val deviceId = StringHelper.nvl((getCurrentTrigger() as BluetoothTrigger).deviceId, NOTHING)
                val name = device.name
                if (StringHelper.hasText(deviceId) && StringHelper.hasText(name) && (name == deviceId || name.matches(deviceId.toRegex()))) {
                    bluetoothDiscovered = true
                    notify("In proximity to $name")
                    activityLink.unregisterBluetoothListener()
                    loadNextStep(context.getString(R.string.walkthrough_transition_bluetooth))
                    return true
                }
            }
            Handler().postDelayed({if (!bluetoothDiscovered && getCurrentTrigger() is BluetoothTrigger){activityLink.execStartBluetoothDiscovery()}},FIVE_SEC_MS)
        }
        return false
    }

    fun saveAndLoadNew(interrupted: Boolean = false, reason: String? = null) {
        onPause()
        if (interrupted){
            walkthrough.addStep(getCurrentStep()?.withTime(getTime())?.withComments(comments))
            Walkthrough.WalkthroughProperty.FINAL_STATE.set(reason?: context.getString(R.string.walkthrough_final_state_cancelled, StringHelper.numberToPositionString(Walkthrough.WalkthroughProperty.STEP_ID_LIST.getAll(String::class).size+1),getCurrentStep()?.title))
        }else{
            Walkthrough.WalkthroughProperty.FINAL_STATE.set(context.getString(R.string.walkthrough_final_state_complete))
        }
        CommunicationHelper.unregisterGpsListener(activityLink)
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

    fun connectActivity(walkthroughActivity: WalkthroughActivity) {
        this.activityLink = walkthroughActivity
    }

    fun onPause() {
        wifiDiscovered = false
        bluetoothDiscovered = false
        refresh = false
        activityLink.stopWifiScan()
        CommunicationHelper.unregisterGpsListener(activityLink)
    }
}