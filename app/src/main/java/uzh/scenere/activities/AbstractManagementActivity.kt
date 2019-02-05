package uzh.scenere.activities

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.support.v4.content.ContextCompat
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import kotlinx.android.synthetic.main.scroll_holder.*
import uzh.scenere.R
import uzh.scenere.datamodel.*
import uzh.scenere.helpers.CollectionsHelper
import uzh.scenere.helpers.DatabaseHelper
import uzh.scenere.helpers.StringHelper
import uzh.scenere.views.*


abstract class AbstractManagementActivity : AbstractBaseActivity() {

    enum class LockState {
        LOCKED, UNLOCKED
    }

    enum class ManagementMode {
        VIEW, EDIT_CREATE, OBJECTS, EDITOR
    }

    protected val inputMap: HashMap<String, TextView> = HashMap()
    protected val multiInputMap: HashMap<String, ArrayList<TextView>> = HashMap()
    protected var lockState: LockState = LockState.LOCKED
    protected var creationButton: SwipeButton? = null
    protected var activeButton: SwipeButton? = null

    //*********
    //* REACT *
    //*********
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //Debug
        //DatabaseHelper.getInstance(applicationContext).dropAndRecreate(Path::class)
        //DatabaseHelper.getInstance(applicationContext).dropAndRecreate(Attribute::class)
        //DatabaseHelper.getInstance(applicationContext).dropAndRecreate(Element::class)
    }

    override fun onResume() {
        super.onResume()
        collapseAndRefreshAllButtons()
    }

    override fun onToolbarLeftClicked() { //SAVE
        if (isInEditMode()) {
            if (!execDoAdditionalCheck()){
                return
            }
            for (entry in inputMap) {
                if (!StringHelper.hasText(entry.value.text)) {
                    toast("Not all required information entered!")
                    return
                }
            }
            createEntity()
            if (isSpacingEnabled()) {
                createTitle("", getContentHolderLayout())
            }
            if (getContentWrapperLayout() is SwipeButtonScrollView){
                (getContentWrapperLayout() as SwipeButtonScrollView).fullScroll(View.FOCUS_DOWN)
            }
            onToolbarRightClicked()
        } else {
            onBackPressed()
        }
    }

    /**
     * @return true if all checks succeed
     */
    open fun execDoAdditionalCheck(): Boolean {
        return true
    }

    override fun onToolbarCenterClicked() { //LOCK & UNLOCK
        if (isInViewMode()) {
            adaptToolbarText(null, null, changeLockState(), null, null)
            for (v in 0 until getContentHolderLayout().childCount) {
                if (getContentHolderLayout().getChildAt(v) is SwipeButton) {
                    (getContentHolderLayout().getChildAt(v) as SwipeButton).setButtonStates(lockState == LockState.UNLOCKED, true, true, true).updateViews(false)
                }
            }
        }
    }

    override fun onToolbarRightClicked() { //CLOSE
        if (isInEditMode()) {
            execMorphInfoBar(InfoState.MINIMIZED)
            getInfoTitle().text = StringHelper.styleString(getSpannedStringFromId(getConfiguredInfoString()), fontAwesome)
            getInfoContent().text = ""
            customizeToolbarText(resources.getText(R.string.icon_back).toString(), null, getLockIcon(), null, null)
            resetEditMode()
            activeButton?.collapse()
            activeButton = null
        }
    }

    override fun onLayoutRendered() {
        if (infoState == null) {
            execMorphInfoBar(InfoState.INITIALIZE)
        }
    }

    //************
    //* CREATION *
    //************
    enum class LineInputType {
        SINGLE_LINE_EDIT, MULTI_LINE_EDIT, LOOKUP, SINGLE_LINE_TEXT, MULTI_LINE_TEXT
    }

    @Suppress("UNCHECKED_CAST")
    protected fun createLine(labelText: String, inputType: LineInputType, presetValue: String? = null, data: Any? = null, executable: (() -> Unit)? = null): View? {
        if (CollectionsHelper.oneOf(inputType, LineInputType.SINGLE_LINE_EDIT, LineInputType.MULTI_LINE_EDIT)) {
            val layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            val childParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            childParams.setMargins(marginSmall!!, marginSmall!!, marginSmall!!, marginSmall!!)
            val wrapper = LinearLayout(this)
            wrapper.layoutParams = layoutParams
            wrapper.weightSum = 2f
            wrapper.orientation = if (inputType == LineInputType.MULTI_LINE_EDIT) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
            val label = TextView(this)
            label.text = getString(R.string.label, labelText)
            label.textSize = textSize!!
            label.layoutParams = childParams
            val input = EditText(this)
            input.setBackgroundColor(ContextCompat.getColor(this, R.color.srePrimary))
            input.setPadding(marginSmall!!, marginSmall!!, marginSmall!!, marginSmall!!)
            input.textAlignment = if (inputType == LineInputType.MULTI_LINE_EDIT) View.TEXT_ALIGNMENT_TEXT_START else View.TEXT_ALIGNMENT_TEXT_END
            input.layoutParams = childParams
            input.textSize = textSize!!
            input.hint = labelText
            input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            input.setText(presetValue)
            input.setSingleLine((inputType != LineInputType.MULTI_LINE_EDIT))
            wrapper.addView(label)
            wrapper.addView(input)
            inputMap[labelText] = input
            return wrapper
        } else if (CollectionsHelper.oneOf(inputType, LineInputType.SINGLE_LINE_TEXT, LineInputType.MULTI_LINE_TEXT)){
            val layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            val childParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            childParams.setMargins(marginSmall!!, marginSmall!!, marginSmall!!, marginSmall!!)
            val wrapper = LinearLayout(this)
            wrapper.layoutParams = layoutParams
            wrapper.weightSum = 2f
            wrapper.orientation = if (inputType == LineInputType.MULTI_LINE_TEXT) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
            val label = TextView(this)
            label.text = getString(R.string.label, labelText)
            label.textSize = textSize!!
            label.layoutParams = childParams
            val text = TextView(this)
            text.setBackgroundColor(ContextCompat.getColor(this, R.color.srePrimary))
            text.setPadding(marginSmall!!, marginSmall!!, marginSmall!!, marginSmall!!)
            text.textAlignment = if (inputType == LineInputType.MULTI_LINE_TEXT) View.TEXT_ALIGNMENT_TEXT_START else View.TEXT_ALIGNMENT_TEXT_END
            text.layoutParams = childParams
            text.textSize = textSize!!
            text.text = presetValue
            text.setSingleLine((inputType != LineInputType.MULTI_LINE_TEXT))
            wrapper.addView(label)
            wrapper.addView(text)
            inputMap[labelText] = text
            return wrapper
        } else if (inputType == LineInputType.LOOKUP && data != null) {
            val layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            val childParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            childParams.setMargins(marginSmall!!, marginSmall!!, marginSmall!!, marginSmall!!)
            val wrapper = LinearLayout(this)
            val selectionCarrier = LinearLayout(this)
            val outerWrapper = LinearLayout(this)
            wrapper.layoutParams = layoutParams
            outerWrapper.layoutParams = layoutParams
            selectionCarrier.layoutParams = layoutParams
            wrapper.weightSum = 2f
            outerWrapper.weightSum = 2f
            wrapper.orientation = LinearLayout.HORIZONTAL
            outerWrapper.orientation = LinearLayout.VERTICAL
            selectionCarrier.orientation = LinearLayout.VERTICAL
            selectionCarrier.gravity = Gravity.CENTER
            val label = TextView(this)
            label.text = getString(R.string.label, labelText)
            label.textSize = textSize!!
            label.layoutParams = childParams
            val spinner = Spinner(applicationContext)
            val spinnerArrayAdapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, data as Array<String>)
            spinnerArrayAdapter.setDropDownViewResource(R.layout.sre_spinner_item)
            spinner.adapter = spinnerArrayAdapter
            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                @SuppressLint("ClickableViewAccessibility")
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val spinnerText = spinner.selectedItem as String
                    if (executable != null){
                        return executable()
                    }
                    if (StringHelper.hasText(spinnerText)) {
                        for (t in 0 until selectionCarrier.childCount) {
                            if ((selectionCarrier.getChildAt(t) as TextView).text == spinnerText) {
                                spinner.setSelection(0)
                                return // Item already selected
                            }
                        }
                        val textView = TextView(applicationContext)
                        val textParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                        textParams.setMargins(marginSmall!!, marginSmall!!, marginSmall!!, marginSmall!!)
                        textView.layoutParams = textParams
                        textView.text = spinnerText
                        textView.textAlignment = View.TEXT_ALIGNMENT_CENTER
                        textView.setBackgroundColor(Color.WHITE)
                        textView.setTextColor(Color.BLACK)
                        textView.setPadding(marginSmall!!, marginSmall!!, marginSmall!!, marginSmall!!)
                        textView.setOnTouchListener { _, _ ->
                            selectionCarrier.removeView(textView)
                            multiInputMap[labelText]?.remove(textView)
                            false
                        }
                        selectionCarrier.addView(textView)
                        if (multiInputMap[labelText] == null){
                            val list = ArrayList<TextView>()
                            list.add(textView)
                            multiInputMap[labelText] = list
                        }else{
                            multiInputMap[labelText]?.add(textView)
                        }
                        spinner.setSelection(0)
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    //NOP
                }
            };

            spinner.setBackgroundColor(ContextCompat.getColor(this, R.color.srePrimary))
            spinner.setPadding(marginSmall!!, marginSmall!!, marginSmall!!, marginSmall!!)
            spinner.textAlignment = if (inputType == LineInputType.MULTI_LINE_EDIT) View.TEXT_ALIGNMENT_TEXT_START else View.TEXT_ALIGNMENT_TEXT_END
            spinner.layoutParams = childParams
            wrapper.addView(label)
            wrapper.addView(spinner)
            outerWrapper.addView(wrapper)
            outerWrapper.addView(selectionCarrier)
            return outerWrapper
        }
        return null
    }

    //*******
    //* GUI *
    //*******
    protected fun changeLockState(): String {
        lockState = if (lockState == LockState.LOCKED) LockState.UNLOCKED else LockState.LOCKED
        return getLockIcon()
    }

    protected fun getLockIcon(): String {
        return when (lockState) {
            LockState.LOCKED -> resources.getString(R.string.icon_lock)
            LockState.UNLOCKED -> resources.getString(R.string.icon_lock_open)
        }
    }

    private fun collapseAndRefreshAllButtons() {
        for (v in 0 until getContentHolderLayout().childCount) {
            if (getContentHolderLayout().getChildAt(v) is SwipeButton) {
                val swipeButton = getContentHolderLayout().getChildAt(v) as SwipeButton
                if (swipeButton.state != SwipeButton.SwipeButtonState.MIDDLE) {
                    swipeButton.collapse()
                }
                when {
                    swipeButton.dataObject is Project -> swipeButton.setCounter(DatabaseHelper.getInstance(applicationContext).readBulk(Stakeholder::class, swipeButton.dataObject).size,
                            DatabaseHelper.getInstance(applicationContext).readBulk(Scenario::class, swipeButton.dataObject).size)
                    swipeButton.dataObject is Scenario -> swipeButton.setCounter(DatabaseHelper.getInstance(applicationContext).readBulk(Object::class, swipeButton.dataObject).size, null)
                    swipeButton.dataObject is Object -> swipeButton.setCounter(DatabaseHelper.getInstance(applicationContext).readBulk(Attribute::class, (swipeButton.dataObject as Object).id).size, null)
                }
            }
        }
    }

    protected fun cleanInfoHolder(titleText: String) {
        customizeToolbarText(resources.getString(R.string.icon_check), null, null, null, resources.getString(R.string.icon_cross))
        getInfoTitle().text = titleText
        getInfoContent().visibility = View.GONE
        removeExcept(getInfoContentWrap(), getInfoContent())
        inputMap.clear()
        getInfoContentWrap().orientation = LinearLayout.VERTICAL
        val layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        getInfoContentWrap().layoutParams = layoutParams
    }

    protected fun showDeletionConfirmation(objectName: String?) {
        if (objectName == null){
            return
        }
        val textPrior = getInfoTitle().text
        val textColorPrior = getInfoTitle().currentTextColor
        getInfoTitle().text = resources.getString(R.string.deleted, objectName)
        getInfoTitle().setTextColor(Color.RED)
        Handler().postDelayed({
            getInfoTitle().text = textPrior
            getInfoTitle().setTextColor(textColorPrior)
        }, 1000)
    }

    abstract fun isInEditMode(): Boolean
    abstract fun isInViewMode(): Boolean
    abstract fun resetEditMode()
    abstract fun createEntity()
    abstract fun getConfiguredInfoString(): Int
    open fun isSpacingEnabled(): Boolean {
        return true
    }
    open fun getContentWrapperLayout(): ViewGroup{
        return scroll_holder_scroll
    }
    open fun getContentHolderLayout(): ViewGroup{
        return scroll_holder_linear_layout_holder
    }
    open fun getInfoWrapper(): LinearLayout{
        return scroll_holder_layout_info
    }
    open fun getInfoTitle(): TextView{
        return scroll_holder_text_info_title
    }
    open fun getInfoContentWrap(): LinearLayout{
        return scroll_holder_text_info_content_wrap
    }
    open fun getInfoContent(): TextView{
        return scroll_holder_text_info_content
    }

    //*************
    //* EXECUTION *
    //************
    enum class InfoState {
        MINIMIZED, NORMAL, MAXIMIZED, INITIALIZE
    }

    private var infoState: InfoState? = null

    protected fun execMorphInfoBar(state: InfoState? = null): CharSequence {
        if (state != null) {
            infoState = state
        } else {
            when (infoState) {
                InfoState.MINIMIZED -> infoState = InfoState.NORMAL
                InfoState.NORMAL -> infoState = InfoState.MAXIMIZED
                InfoState.MAXIMIZED -> infoState = InfoState.MINIMIZED
                else -> {
                } //NOP
            }
        }
        return execMorphInfoBarInternal()
    }

    protected var contentDefaultMaxLines = 2
    private fun execMorphInfoBarInternal(): CharSequence {
        when (infoState) {
            InfoState.INITIALIZE -> {
                getContentWrapperLayout().layoutParams = createLayoutParams(1f)
                getInfoWrapper().layoutParams = createLayoutParams(9f)
                createLayoutParams(0f, getInfoTitle())
                getInfoContentWrap().layoutParams = createLayoutParams(1f)
                infoState = InfoState.MINIMIZED
                return resources.getText(R.string.icon_win_min)
            }
            InfoState.MINIMIZED -> {
                WeightAnimator(getInfoWrapper(), 9f, 250).play()
                WeightAnimator(getContentWrapperLayout(), 1f, 250).play()
                createLayoutParams(0f, getInfoTitle())
                getInfoContentWrap().layoutParams = createLayoutParams(1f)
                execMinimizeKeyboard()
                return resources.getText(R.string.icon_win_min)
            }
            InfoState.NORMAL -> {
                WeightAnimator(getContentWrapperLayout(), 3f, 250).play()
                WeightAnimator(getInfoWrapper(), 7f, 250).play()
                createLayoutParams(2f, getInfoTitle(), 1)
                getInfoContentWrap().layoutParams = createLayoutParams(1f)
                getInfoContent().maxLines = contentDefaultMaxLines
                return resources.getText(R.string.icon_win_norm)
            }
            InfoState.MAXIMIZED -> {
                WeightAnimator(getContentWrapperLayout(), 10f, 250).play()
                WeightAnimator(getInfoWrapper(), 0f, 250).play()
                createLayoutParams(2.7f, getInfoTitle(), 1)
                getInfoContentWrap().layoutParams = createLayoutParams(0.3f)
                getInfoContent().maxLines = 10
                return resources.getText(R.string.icon_win_max)
            }
        }
        return resources.getText(R.string.icon_null)
    }

    fun getTextsFromLookupChoice(id: String): ArrayList<String>{
        val list = ArrayList<String>()
        if (!StringHelper.hasText(id)){
            return list
        }
        val multi = multiInputMap[id]
        if (multi == null || multi.isEmpty()){
            return list
        }
        for (text in multi){
            list.add(text.text.toString())
        }
        return list
    }

}