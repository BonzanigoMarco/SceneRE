package uzh.scenere.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Typeface
import android.net.Uri
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.nfc.*
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.support.v7.app.AppCompatActivity
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.text.SpannedString
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import kotlinx.android.synthetic.main.sre_toolbar.*
import uzh.scenere.R
import uzh.scenere.const.Constants
import uzh.scenere.const.Constants.Companion.FOLDER_TEMP
import uzh.scenere.const.Constants.Companion.MILLION
import uzh.scenere.const.Constants.Companion.NOTHING
import uzh.scenere.const.Constants.Companion.PHONE_STATE
import uzh.scenere.const.Constants.Companion.SMS_RECEIVED
import uzh.scenere.const.Constants.Companion.ZERO
import uzh.scenere.helpers.*
import uzh.scenere.listener.SreBluetoothReceiver
import uzh.scenere.listener.SreBluetoothScanCallback
import uzh.scenere.listener.SrePhoneReceiver
import uzh.scenere.listener.SreSmsReceiver
import java.io.File
import java.io.IOException
import kotlin.random.Random
import kotlin.reflect.KClass


abstract class AbstractBaseActivity : AppCompatActivity() {
    protected var marginSmall: Int? = null
    protected var marginHuge: Int? = null
    protected var textSize: Float? = null
    protected var fontAwesome: Typeface? = null
    protected var fontNormal: Typeface = Typeface.DEFAULT
    protected var screenWidth = 0
    protected var screenHeight = 0
    protected var tutorialOpen = false
    //NFC
    private var nfcAdapter: NfcAdapter? = null
    private var pendingIntent: PendingIntent? = null
    private var nfcReady = false
    //WiFi
    private var wifiManager: WifiManager? = null
    //Bluetooth
    private var sreBluetoothReceiver: SreBluetoothReceiver? = null
    private var sreBluetoothCallback: SreBluetoothScanCallback? = null
    //Telephone
    private var srePhoneReceiver: SrePhoneReceiver? = null
    //SMS
    private var sreSmsReceiver: SreSmsReceiver? = null
    //Async
    private var asyncTask: SreAsyncTask? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkAndCreateFolderStructure()
        setContentView(getConfiguredLayout())
        readVariables()
        initNfc()
        initWifi()
    }

    private fun checkAndCreateFolderStructure(){
        if (PermissionHelper.check(applicationContext,PermissionHelper.Companion.PermissionGroups.STORAGE)){
            FileHelper.checkAndCreateFolderStructure()
        }
    }

    private fun readVariables() {
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        screenHeight = displayMetrics.heightPixels
        screenWidth = displayMetrics.widthPixels
        marginSmall = DipHelper.get(resources).dip5
        marginHuge = DipHelper.get(resources).dip50
        textSize = DipHelper.get(resources).dip3_5.toFloat()
        fontAwesome = Typeface.createFromAsset(applicationContext.assets, "FontAwesome900.otf")
    }

    //NFC

    open fun isUsingNfc():Boolean{
        return false
    }

    protected fun isNfcReady(): Boolean{
        return nfcReady
    }

    private fun initNfc() {
        if (isUsingNfc()) {
            nfcAdapter = NfcAdapter.getDefaultAdapter(this)
            if (nfcAdapter != null) {
                pendingIntent = PendingIntent.getActivity(this, 0, Intent(this,
                        javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0)

            }
            nfcReady = true
        }
    }

    override fun onResume() {
        super.onResume()
        if (nfcAdapter != null && pendingIntent != null){
            nfcAdapter!!.enableForegroundDispatch(this, pendingIntent, null, null)
        }
        resumePhoneCallListener()
        resumeSmsListener()
        resumeBluetoothListener()
    }

    override fun onPause() {
        if (nfcAdapter != null) {
            nfcAdapter!!.disableForegroundDispatch(this)
        }
        pausePhoneCallListener()
        pauseSmsListener()
        pauseBluetoothListener()
        cancelAsyncTask()
        super.onPause()
    }

    override fun onDestroy() {
        unregisterPhoneCallListener()
        unregisterSmsListener()
        unregisterBluetoothListener()
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (nfcReady && intent != null){
            getNfcTagInfo(intent)
        }
    }

    private var nfcDataWrite: String? = null
    private var nfcDataRead: String? = null
    private var beamBinaryWrite: ByteArray? = null
    private var beamBinaryRead: ByteArray? = null

    fun setDataToWrite(data: String?){
        nfcDataWrite = data
    }

    fun getDataToRead(): String?{
        return nfcDataRead
    }

    fun setBinaryDataToBeam(data: ByteArray?){
        beamBinaryWrite = data
    }

    fun getBinaryDataFromBeam(): ByteArray?{
        return beamBinaryRead
    }

    open fun execUseNfcData(data: String){
        //NOP
    }

    open fun execNoDataRead(){
        //NOP
    }

    open fun onDataWriteExecuted(returnValues: Pair<Boolean, String>){
        notify(getString(R.string.nfc),returnValues.second)
    }

    private fun getNfcTagInfo(intent: Intent) {
        val tag: Tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        if (nfcDataWrite != null){
            onDataWriteExecuted(writeDataToTag(tag,nfcDataWrite!!))
            nfcDataWrite = null
        }else{
            nfcDataRead = getDataFromTag(tag,intent)
            if (nfcDataRead != null){
                execUseNfcData(nfcDataRead!!)
            }else{
                execNoDataRead()
            }
        }
    }

    private fun getDataFromTag(tag: Tag, intent: Intent): String? {
        val ndef = Ndef.get(tag)
        try {
            ndef.connect()
            val messages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)

            if (messages != null) {
                val ndefMessages = ArrayList<NdefMessage>()
                for (i in 0 until messages.size) {
                    ndefMessages.add(messages[i] as NdefMessage)
                }
                val record = ndefMessages[0].records[0]

                val payload = record.payload
                val text = String(payload)
                ndef.close()
                return text
            }
        } catch (e: Exception) {
            //NOP
        }
        return null
    }

    private fun writeDataToTag(tag: Tag, message: String): Pair<Boolean,String>{
        val nDefTag = Ndef.get(tag)
        val ndefMessage = dataToNdefMessage(message)
        try {

            nDefTag?.let {
                it.connect()
                if (it.maxSize < ndefMessage.toByteArray().size) {
                    return Pair(false,getString(R.string.nfc_too_large))
                }
                return if (it.isWritable) {
                    it.writeNdefMessage(ndefMessage)
                    it.close()
                    Pair(true,getString(R.string.nfc_success))
                } else {
                    Pair(false,getString(R.string.nfc_write_not_supported))
                }
            }
        } catch (e: Exception) {
            //Possible unformatted Tag, try to format
            try{
                val nDefFormatableTag = NdefFormatable.get(tag)

                nDefFormatableTag?.let {
                    return try {
                        it.connect()
                        it.format(ndefMessage)
                        it.close()
                        Pair(true,getString(R.string.nfc_success))
                    } catch (e: IOException) {
                        Pair(false,getString(R.string.nfc_no_init))
                    }
                }
                return Pair(false,getString(R.string.nfc_write_not_supported))
            }catch(e: Exception){
                //NOP
            }
        }
        return Pair(false,getString(R.string.nfc_write_not_supported))
    }

    private fun dataToNdefMessage(data: String): NdefMessage{
        val pathPrefix = Constants.APPLICATION_ID
        val nfcRecord = NdefRecord(NdefRecord.TNF_EXTERNAL_TYPE, pathPrefix.toByteArray(), ByteArray(0), data.toByteArray())
        return NdefMessage(arrayOf(nfcRecord))
    }

    @SuppressLint("SetWorldReadable")
    fun sendDataOverBeam(){
        if (beamBinaryWrite != null && isUsingNfc() && CommunicationHelper.check(this,CommunicationHelper.Companion.Communications.NFC)&&
                CommunicationHelper.supports(this,CommunicationHelper.Companion.Communications.NFC)&&
                CommunicationHelper.requestBeamActivation(this,true)){
            val fileName = getString(R.string.share_export_file_prefix) + DateHelper.getCurrentTimestamp() + Constants.SRE_FILE
            val filePath = FileHelper.writeFile(applicationContext, beamBinaryWrite!!,fileName,FOLDER_TEMP)
            val fileToTransfer = File(filePath)
            fileToTransfer.setReadable(true, false)
            fileToTransfer.deleteOnExit()
            nfcAdapter!!.setBeamPushUris(arrayOf(Uri.fromFile(fileToTransfer)), this)
            nfcAdapter!!.invokeBeam(this)
            beamBinaryWrite = null
        }
    }

    // WiFi

    open fun isUsingWifi(): Boolean{
        return false
    }

    private fun initWifi(){
        if (isUsingWifi()) {
            wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val wifiReceiver = WifiBroadcastReceiver(wifiManager, onWifiDiscoveredExecutable)
            registerReceiver(wifiReceiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
        }
    }

    var scanningWifi: Boolean = false

    fun startWifiScan(){
        scanningWifi = true
        wifiManager?.startScan()
    }

    fun stopWifiScan(){
        scanningWifi = false
    }

    private val onWifiDiscoveredExecutable: (List<ScanResult>?) -> Unit = {
        if (!it.isNullOrEmpty()) {
            for (scanResult in it){
                execUseWifiScanResult(scanResult)
            }
        }
        if (scanningWifi){
            Handler().postDelayed({
                wifiManager?.startScan()
            },2000)
        }
    }

    open fun execUseWifiScanResult(scanResult: ScanResult) {
        //NOP
    }

    private class WifiBroadcastReceiver(val wifiManager: WifiManager?,val onWifiDiscoveredExecutable: (List<ScanResult>?) -> Unit) : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {
                onWifiDiscoveredExecutable.invoke(wifiManager?.scanResults)
            }
        }
    }

    abstract fun getConfiguredLayout(): Int
    abstract fun getConfiguredRootLayout(): ViewGroup?

    open fun onNavigationButtonClicked(view: View) {
        when (view.id) {
            R.id.startup_button_continue -> startActivity(Intent(this, MainMenuActivity::class.java))
            R.id.projects_button_scenario_management -> startActivity(Intent(this, ScenariosActivity::class.java))
        }
    }

    open fun onToolbarClicked(view: View) {
        when (view.id) {
            R.id.toolbar_action_left -> onToolbarLeftClicked()
            R.id.toolbar_action_center_left -> onToolbarCenterLeftClicked()
            R.id.toolbar_action_center -> onToolbarCenterClicked()
            R.id.toolbar_action_center_right -> onToolbarCenterRightClicked()
            R.id.toolbar_action_right -> onToolbarRightClicked()
        }
    }

    open fun onButtonClicked(view: View) {
        //NOP
    }

    open fun onToolbarLeftClicked() {
        //NOP
    }

    open fun onToolbarCenterLeftClicked() {
        //NOP
    }

    open fun onToolbarCenterClicked() {
        //NOP
    }

    open fun onToolbarCenterRightClicked() {
        //NOP
    }

    open fun onToolbarRightClicked() {
        //NOP
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        onLayoutRendered()
    }

    open fun onLayoutRendered(){
        reStyleText(applicationContext,getConfiguredRootLayout())
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        execAdaptToOrientationChange()
    }

    //************
    //* CREATION *
    //************
    protected fun createLayoutParams(weight: Float, textView: TextView? = null, crop: Int = 0): LinearLayout.LayoutParams {
        val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT,
                weight
        )
        if (textView != null) {
            val margin = NumberHelper.nvl(this.resources?.getDimension(R.dimen.dpi5), 0).toInt()
            textView.setPadding(0, 0, 0, 0)
            layoutParams.setMargins(margin, margin, margin, margin)
            when (crop) {
                0 -> {
                    textView.setPadding(0, -margin / 2, 0, 0)
                }
                1 -> layoutParams.setMargins(margin, margin, margin, margin / 2)
                2 -> layoutParams.setMargins(margin, margin / 2, margin, margin)
            }
            textView.gravity = Gravity.CENTER
            textView.textAlignment = View.TEXT_ALIGNMENT_CENTER
            textView.layoutParams = layoutParams
        }
        return layoutParams
    }

    protected fun createTitle(title: String, holder: ViewGroup) {
        val titleText = TextView(this)
        val titleParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        titleText.layoutParams = titleParams
        titleText.textAlignment = View.TEXT_ALIGNMENT_CENTER
        titleText.gravity = Gravity.CENTER
        titleText.text = title
        titleText.setTextColor(getColorWithStyle(applicationContext,R.color.srePrimaryDark))
        holder.addView(titleText)
    }

    fun toast(toast: String) {
        Toast.makeText(this, toast, Toast.LENGTH_SHORT).show()
    }

    private val notificationQueue = ArrayList<Pair<String?,String?>>()
    fun notify(title: String? = null, content: String? = null, clearNotificationId: Int? = null){
        val notificationManager = NotificationManagerCompat.from(this)
        if (clearNotificationId == null){
            notificationQueue.add(Pair(title,content))
            if (notificationQueue.size > 1){
                return // let the notifications in the queue call themselves
            }
        }else{
            notificationQueue.removeFirst()
            notificationManager.cancel(clearNotificationId)
        }
        if (notificationQueue.isEmpty()){
            return
        }
        val notification = NotificationCompat.Builder(applicationContext, Constants.APPLICATION_ID)
                .setSmallIcon(android.R.drawable.btn_star)
                .setContentTitle(notificationQueue.first().first)
                .setColor(getColorWithStyle(this,R.color.srePrimary))
                .setColorized(true)
                .setDefaults(Notification.DEFAULT_ALL)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            notification.priority = NotificationManager.IMPORTANCE_HIGH
        }else{
            notification.priority = Notification.PRIORITY_MAX
        }
        if (notificationQueue.first().second != null){
            notification.setContentText(notificationQueue.first().second)
        }
        val text = StringHelper.nvl(notificationQueue.first().first,NOTHING).plus(StringHelper.nvl(notificationQueue.first().second,NOTHING))
        val id = Random(System.currentTimeMillis()).nextInt(ZERO, MILLION)
        Handler().postDelayed( {
            notificationManager.notify(id, notification.build())
        },100)
        Handler().postDelayed( {
            notify(null,null,id)
        },2000L+(50*text.length))
    }

    protected fun execMinimizeKeyboard(){
        val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        val focusView = currentFocus ?: View(this)
        inputMethodManager.hideSoftInputFromWindow(focusView.windowToken, 0)
        focusView.clearFocus()
    }

    open fun execAdaptToOrientationChange() {
        //NOP
    }

    //*******
    //* GUI *
    //*******
    protected fun customizeToolbarId(l: Int?, cl: Int?, c: Int?, cr: Int?, r: Int?) {
        toolbar_action_left.text = StringHelper.lookupOrEmpty(l, applicationContext)
        toolbar_action_center_left.text = StringHelper.lookupOrEmpty(cl, applicationContext)
        toolbar_action_center.text = StringHelper.lookupOrEmpty(c, applicationContext)
        toolbar_action_center_right.text = StringHelper.lookupOrEmpty(cr, applicationContext)
        toolbar_action_right.text = StringHelper.lookupOrEmpty(r, applicationContext)
    }

    protected fun adaptToolbarId(l: Int?, cl: Int?, c: Int?, cr: Int?, r: Int?) {
        toolbar_action_left.text = if (l != null) StringHelper.lookupOrEmpty(l, applicationContext) else toolbar_action_left.text
        toolbar_action_center_left.text = if (cl != null)  StringHelper.lookupOrEmpty(cl, applicationContext) else toolbar_action_center_left.text
        toolbar_action_center.text = if (c != null)  StringHelper.lookupOrEmpty(c, applicationContext) else toolbar_action_center.text
        toolbar_action_center_right.text = if (cr != null)  StringHelper.lookupOrEmpty(cr, applicationContext) else toolbar_action_center_right.text
        toolbar_action_right.text = if (r != null)  StringHelper.lookupOrEmpty(r, applicationContext) else toolbar_action_right.text
    }

    protected fun customizeToolbarText(l: String?, cl: String?, c: String?, cr: String?, r: String?) {
        toolbar_action_left.text = l
        toolbar_action_center_left.text = cl
        toolbar_action_center.text = c
        toolbar_action_center_right.text = cr
        toolbar_action_right.text = r
    }

    protected fun adaptToolbarText(l: String?, cl: String?, c: String?, cr: String?, r: String?) {
        toolbar_action_left.text = l ?: toolbar_action_left.text
        toolbar_action_center_left.text = cl ?: toolbar_action_center_left.text
        toolbar_action_center.text = c ?: toolbar_action_center.text
        toolbar_action_center_right.text = cr ?: toolbar_action_center_right.text
        toolbar_action_right.text = r ?: toolbar_action_right.text
    }

    protected fun getSpannedStringFromId(id: Int): SpannedString{
        return getText(id) as SpannedString
    }

    protected fun executeAsyncTask(asyncFunction: () -> Unit, postExecuteFunction: () -> Unit){
        if (asyncTask == null){
            asyncTask = SreAsyncTask(asyncFunction,postExecuteFunction,{cancelAsyncTask()})
            asyncTask?.execute()
        }
    }

    protected fun cancelAsyncTask(){
        if (asyncTask != null){
            try {
                asyncTask?.cancel(true)
            }catch (e: Exception){
                //NOP
            }
            asyncTask = null
        }
    }

    protected fun isAsyncTaskRunning(): Boolean{
        if (asyncTask != null){
            return !asyncTask!!.isCancelled
        }
        return false
    }

    class SreAsyncTask(private val asyncFunction: () -> Unit, private val postExecuteFunction: () -> Any?, val cleanupFunction: () -> Unit) : AsyncTask<Void, Void, Void?>() {
        override fun doInBackground(vararg params: Void?): Void? {
            try{
                asyncFunction.invoke()
            }catch(e: Exception){
                //NOP
            }
            return null
        }

        override fun onPreExecute() {
            //NOP
        }

        override fun onPostExecute(result: Void?) {
            try{
                postExecuteFunction.invoke()
            }catch(e: Exception){
                //NOP
            }finally {
                cleanupFunction.invoke()
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    protected fun <T: View> searchForLayout(view: View, clazz: KClass<T>): T? {
        if (view is ViewGroup){
            for (v in 0 until view.childCount){
                if (view.getChildAt(v)::class == clazz){
                    return view.getChildAt(v) as T
                }
                val v0 = searchForLayout(view.getChildAt(v),clazz)
                if (v0 != null){
                    return v0
                }
            }
        }
        return null
    }

    protected fun <T: View?> removeExcept(holder: ViewGroup, exception: T) {
        if (exception == null){
            return
        }
        if (holder.childCount == 0)
            return
        if (holder.childCount == 1 && holder.getChildAt(0) == exception)
            return
        if (holder.getChildAt(0) != exception) {
            holder.removeViewAt(0)
        }else{
            holder.removeViewAt(holder.childCount-1)
        }
        removeExcept(holder,exception)
    }

    fun registerPhoneCallListener(){
        srePhoneReceiver = SrePhoneReceiver(handlePhoneCall)
        resumePhoneCallListener()
    }

    fun unregisterPhoneCallListener(){
        pausePhoneCallListener()
        srePhoneReceiver = null
    }

    private fun resumePhoneCallListener(){
        if (srePhoneReceiver != null){
            srePhoneReceiver!!.registerListener(applicationContext)
            val phoneCallFilter = IntentFilter()
            phoneCallFilter.addAction(PHONE_STATE)
            registerReceiver(srePhoneReceiver,phoneCallFilter)
        }
    }

    private fun pausePhoneCallListener(){
        if (srePhoneReceiver != null){
            try{
                val telephonyManager = applicationContext.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                telephonyManager.listen(null,PhoneStateListener.LISTEN_NONE)
                unregisterReceiver(srePhoneReceiver)
            }catch(e: Exception){
                //NOP
            }
        }
    }

    open fun handlePhoneNumber(phoneNumber: String): Boolean {
        return false
    }

    private val handlePhoneCall: (Context?, String) -> Boolean = { context: Context?, phoneNumber: String ->
        var hangUp = false
        if (handlePhoneNumber(phoneNumber)) {
            try {
                val telephonyManager = context?.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

                val methodGetITelephony = telephonyManager.javaClass.getDeclaredMethod("getITelephony")

                methodGetITelephony.isAccessible = true

                val telephonyInterface = methodGetITelephony.invoke(telephonyManager)

                val methodEndCall = telephonyInterface.javaClass.getDeclaredMethod("endCall")

                methodEndCall.invoke(telephonyInterface)
                hangUp = true

            } catch (e: java.lang.Exception) {
                //NOP
            }
        }
        hangUp
    }

    fun registerSmsListener(){
        sreSmsReceiver = SreSmsReceiver(handleSms)
        resumeSmsListener()
    }

    fun unregisterSmsListener(){
        pauseSmsListener()
        sreSmsReceiver = null
    }

    private fun resumeSmsListener(){
        if (sreSmsReceiver != null){
            val smsFilter = IntentFilter()
            smsFilter.addAction(SMS_RECEIVED)
            registerReceiver(sreSmsReceiver,smsFilter)
        }
    }

    private fun pauseSmsListener(){
        if (sreSmsReceiver != null){
            try{
                unregisterReceiver(sreSmsReceiver)
            }catch(e: Exception){
                //NOP
            }
        }
    }

    open fun handleSmsData(phoneNumber: String, message: String): Boolean {
        return false
    }

    private val handleSms: (String, String) -> Boolean = { phoneNumber: String, message: String ->
        if (handleSmsData(phoneNumber,message)) {
            //NOP
        }
        true
    }

    fun registerBluetoothListener(){
        if (CommunicationHelper.check(this,CommunicationHelper.Companion.Communications.BLUETOOTH)){
            // Turn off Bluetooth to let devices reconnect
            CommunicationHelper.disable(this,CommunicationHelper.Companion.Communications.BLUETOOTH)
        }
        //Turn on Bluetooth again
        CommunicationHelper.enable(this,CommunicationHelper.Companion.Communications.BLUETOOTH)
        registerBluetoothListenerDelayed()
    }

    private fun registerBluetoothListenerDelayed() {
        Handler().postDelayed({
            if (CommunicationHelper.check(this, CommunicationHelper.Companion.Communications.BLUETOOTH)) {
                sreBluetoothReceiver = SreBluetoothReceiver(handleBluetooth)
                resumeBluetoothListener()
            }else{
                CommunicationHelper.enable(this,CommunicationHelper.Companion.Communications.BLUETOOTH)
                registerBluetoothListenerDelayed()
            }
        }, 300
        )
    }

    fun unregisterBluetoothListener(){
        pauseBluetoothListener()
        sreBluetoothReceiver = null
    }

    private fun resumeBluetoothListener(){
        if (sreBluetoothReceiver != null){
            val bluetoothFilter = IntentFilter(BluetoothDevice.ACTION_FOUND)
            bluetoothFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            bluetoothFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            registerReceiver(sreBluetoothReceiver,bluetoothFilter)
            execStartBluetoothDiscovery()
        }
    }

    private fun pauseBluetoothListener(){
        if (sreBluetoothReceiver != null){
            try{
                execStopBluetoothDiscovery()
                unregisterReceiver(sreBluetoothReceiver)
            }catch(e: Exception){
                //NOP
            }
        }
    }

    fun execStartBluetoothDiscovery() {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        bluetoothAdapter.startDiscovery()
        if (sreBluetoothCallback == null){
            val alreadyKnownDevices = HashMap<String,BluetoothDevice>()
            if (!bluetoothAdapter.bondedDevices.isEmpty()){
                for (device in bluetoothAdapter.bondedDevices){
                    alreadyKnownDevices[device.address] = device
                }
            }
            sreBluetoothReceiver?.addAlreadyKnownDevices(alreadyKnownDevices)
            sreBluetoothCallback = SreBluetoothScanCallback(handleBluetooth)
            sreBluetoothCallback?.addAlreadyKnownDevices(alreadyKnownDevices)
            bluetoothAdapter.bluetoothLeScanner.startScan(sreBluetoothCallback)
        }
    }

    private fun execStopBluetoothDiscovery() {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        bluetoothAdapter.cancelDiscovery()
        if (sreBluetoothCallback != null){
            bluetoothAdapter.bluetoothLeScanner.stopScan(sreBluetoothCallback)
            bluetoothAdapter.bluetoothLeScanner.flushPendingScanResults(sreBluetoothCallback)
            sreBluetoothCallback = null
        }
    }

    open fun handleBluetoothData(deviceName: List<BluetoothDevice>): Boolean {
        return false
    }

    private val handleBluetooth: (List<BluetoothDevice>) -> Boolean = { devices: List<BluetoothDevice> ->
        if (handleBluetoothData(devices)) {
            //NOP
        }
        true
    }
}
