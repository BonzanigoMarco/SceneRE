package uzh.scenere.activities

import android.app.Activity
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Typeface
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
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
import uzh.scenere.const.Constants.Companion.NOTHING
import uzh.scenere.helpers.DipHelper
import uzh.scenere.helpers.NumberHelper
import uzh.scenere.helpers.StringHelper
import java.io.IOException
import java.lang.Exception
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


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(getConfiguredLayout())
        readVariables()
        initNfc()
    }

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
    }

    override fun onPause() {
        if (nfcAdapter != null) {
            nfcAdapter!!.disableForegroundDispatch(this)
        }
        super.onPause()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (nfcReady && intent != null){
            getNfcTagInfo(intent)
        }
    }

    private var nfcDataWrite: String? = null
    private var nfcDataRead: String? = null

    fun setDataToWrite(data: String){
        nfcDataWrite = data
    }

    fun getDataToRead(): String?{
        return nfcDataRead
    }

    open fun execUseNfcData(data: String){
        //NOP
    }

    private fun getNfcTagInfo(intent: Intent) {
        val tag: Tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        if (nfcDataWrite != null){
            writeDataToTag(tag,nfcDataWrite!!)
            nfcDataWrite = null
        }else{
            nfcDataRead = getDataFromTag(tag,intent)
            if (nfcDataRead != null){
                execUseNfcData(nfcDataRead!!)
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

    private fun writeDataToTag(tag: Tag, message: String): Boolean{
        val nDefTag = Ndef.get(tag)
        val ndefMessage = dataToNdefMessage(message)
        try {

            nDefTag?.let {
                it.connect()
                if (it.maxSize < ndefMessage.toByteArray().size) {
                    notify(getString(R.string.nfc_too_large))
                    return false
                }
                return if (it.isWritable) {
                    it.writeNdefMessage(ndefMessage)
                    it.close()
                    notify(getString(R.string.nfc_success))
                    true
                } else {
                    notify(getString(R.string.nfc_write_not_supported))
                    false
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
                        notify(getString(R.string.nfc_success))
                        true
                    } catch (e: IOException) {
                        notify(getString(R.string.nfc_no_init))
                        false
                    }
                }
                return false
            }catch(e: Exception){
                notify(getString(R.string.nfc_write_not_supported))
            }
        }
        return false
    }

    private fun dataToNdefMessage(data: String): NdefMessage{
        val pathPrefix = Constants.APPLICATION_ID
        val nfcRecord = NdefRecord(NdefRecord.TNF_EXTERNAL_TYPE, pathPrefix.toByteArray(), ByteArray(0), data.toByteArray())
        return NdefMessage(arrayOf(nfcRecord))
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

    abstract fun getConfiguredLayout(): Int

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
        //NOP
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
        titleText.setTextColor(ContextCompat.getColor(applicationContext,R.color.srePrimaryDark))
        holder.addView(titleText)
    }

    fun toast(toast: String) {
        Toast.makeText(this, toast, Toast.LENGTH_SHORT).show()
    }

    //TODO queue
    fun notify(title: String? = null, content: String? = null){
        val notification = NotificationCompat.Builder(applicationContext, Constants.APPLICATION_ID)
                .setSmallIcon(android.R.drawable.btn_star)
                .setContentTitle(title)
                .setColor(ContextCompat.getColor(this,R.color.srePrimary))
                .setColorized(true)
                .setDefaults(Notification.DEFAULT_ALL)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            notification.priority = NotificationManager.IMPORTANCE_HIGH
        }else{
            notification.priority = Notification.PRIORITY_MAX
        }
        if (content != null){
            notification.setContentText(content)
        }
        val text = StringHelper.nvl(title,NOTHING).plus(StringHelper.nvl(content,NOTHING))
        val notificationManager = NotificationManagerCompat.from(this)
        notificationManager.notify(0, notification.build())
        Handler().postDelayed( {notificationManager.cancel(0) },2000L+(50*text.length))
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

    class SreAsyncTask() : AsyncTask<Void, Void, String?>() {
        override fun doInBackground(vararg params: Void?): String? {
            //NOP
            return null
        }

        override fun onPreExecute() {
            //NOP
        }

        override fun onPostExecute(result: String?) {
            //NOP
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
                    return v0 as T
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
}
