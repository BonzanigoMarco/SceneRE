package uzh.scenere.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.support.v4.content.ContextCompat
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.*
import kotlinx.android.synthetic.main.scroll_holder.*
import uzh.scenere.R
import uzh.scenere.const.Constants
import uzh.scenere.datamodel.*
import uzh.scenere.helpers.CollectionsHelper
import uzh.scenere.helpers.DatabaseHelper
import uzh.scenere.helpers.NumberHelper
import uzh.scenere.helpers.StringHelper
import uzh.scenere.views.*
import uzh.scenere.views.SreTextView.TextStyle.*
import java.util.*


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
    private var scrollY: Int? = null

    //*********
    //* REACT *
    //*********
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //Debug
//        DatabaseHelper.getInstance(applicationContext).dropAndRecreate(Path::class)
//        DatabaseHelper.getInstance(applicationContext).dropAndRecreate(Attribute::class)
//        DatabaseHelper.getInstance(applicationContext).dropAndRecreate(Element::class)
//        DatabaseHelper.getInstance(applicationContext).dropAndRecreate(AbstractObject::class)
//        DatabaseHelper.getInstance(applicationContext).dropAndRecreateAll()
    }

    override fun onResume() {
        super.onResume()
        collapseAndRefreshAllButtons()
    }

    override fun onToolbarLeftClicked() { //SAVE
        if (isInputOpen()) {
            if (!execDoAdditionalCheck()) {
                return
            }
            for (entry in inputMap) {
                if (!StringHelper.hasText(entry.value.text)) {
                    notify("Not all required information entered!")
                    return
                }
            }
            createEntity()
            if (isSpacingEnabled()) {
                createTitle("", getContentHolderLayout())
            }
            if (getContentWrapperLayout() is SwipeButtonScrollView) {
                if (isInEditMode() && !isInAddMode()){
                    execScrollBack()
                }else{
                    execScroll()
                }
            }
            onToolbarRightClicked()
        } else {
            super.onBackPressed()
        }
    }

    override fun onBackPressed() {
        if (getContentWrapperLayout() is SwipeButtonScrollView &&
                (getContentWrapperLayout() as SwipeButtonScrollView).scrollY > 0){
            execFullScrollUp()
        }else{
            super.onBackPressed()
        }
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
        if (isInputOpen()) {
            execMorphInfoBar(InfoState.MINIMIZED)
            getInfoTitle().text = StringHelper.styleString(getSpannedStringFromId(getConfiguredInfoString()), fontAwesome)
            getInfoContent().text = ""
            resetToolbar()
            resetEditMode()
            activeButton?.collapse()
            activeButton = null
            execScrollBack()
        }
    }

    open fun execScroll(scrollBackToPrevious: Boolean = false) {
        if (scrollBackToPrevious){
            execScrollBack()
        }else{
            execFullScroll()
        }
    }

    private fun execFullScroll() {
        var alreadyLoaded = false
        if (getContentHolderLayout() is SwipeButtonSortingLayout) {
            execMinimizeKeyboard()
            alreadyLoaded = (getContentHolderLayout() as SwipeButtonSortingLayout).scrollToLastAdded()
            execMinimizeKeyboard()
        }
        if (!alreadyLoaded || (getContentWrapperLayout() as SwipeButtonScrollView).scrollY == 0) {
            Handler().postDelayed({ (getContentWrapperLayout() as SwipeButtonScrollView).fullScroll(View.FOCUS_DOWN) }, 250)
        }
    }

    private fun execScrollBack() {
        Handler().postDelayed({(getContentWrapperLayout() as SwipeButtonScrollView).scrollTo(0,NumberHelper.nvl(scrollY,0))},250)
    }

    open fun execFullScrollUp() {
        (getContentWrapperLayout() as SwipeButtonScrollView).scrollTo(0, 0)
    }

    /**
     * @return true if all checks succeed
     */
    open fun execDoAdditionalCheck(): Boolean {
        return true
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
        SINGLE_LINE_EDIT, MULTI_LINE_EDIT, LOOKUP, SINGLE_LINE_TEXT, MULTI_LINE_TEXT, SINGLE_LINE_CONTEXT_EDIT, MULTI_LINE_CONTEXT_EDIT, NUMBER_EDIT
    }

    @Suppress("UNCHECKED_CAST")
    protected fun createLine(labelText: String, inputType: LineInputType, presetValue: String? = null, data: Any? = null, executable: (() -> Unit)? = null): View? {
        if (CollectionsHelper.oneOf(inputType, LineInputType.SINGLE_LINE_EDIT, LineInputType.MULTI_LINE_EDIT, LineInputType.NUMBER_EDIT)) {
            val layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            layoutParams.setMargins(marginSmall!!, marginSmall!!, marginSmall!!, marginSmall!!)
            val wrapper = LinearLayout(this)
            wrapper.layoutParams = layoutParams
            wrapper.weightSum = 2f
            wrapper.orientation = if (inputType == LineInputType.MULTI_LINE_EDIT) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
            val label = SreTextView(this, wrapper, getString(R.string.label, labelText), BORDERLESS_DARK)
            label.setWeight(1f)
            label.setSize(WRAP_CONTENT, if (inputType == LineInputType.MULTI_LINE_EDIT) MATCH_PARENT else 0)
            label.textAlignment = View.TEXT_ALIGNMENT_TEXT_START
            val input = SreEditText(this, wrapper, null, getString(R.string.input, labelText))
            input.textAlignment = if (inputType == LineInputType.MULTI_LINE_EDIT) View.TEXT_ALIGNMENT_TEXT_START else View.TEXT_ALIGNMENT_TEXT_END
            input.textSize = textSize!!
            input.inputType = if (inputType == LineInputType.NUMBER_EDIT) InputType.TYPE_CLASS_NUMBER else InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            input.setText(presetValue)
            input.setWeight(1f)
            input.setSize(MATCH_PARENT, if (inputType == LineInputType.MULTI_LINE_EDIT) MATCH_PARENT else 0)
            input.setSingleLine((inputType != LineInputType.MULTI_LINE_EDIT))
            wrapper.addView(label)
            wrapper.addView(input)
            inputMap[labelText] = input
            return wrapper
        } else if (CollectionsHelper.oneOf(inputType, LineInputType.SINGLE_LINE_CONTEXT_EDIT, LineInputType.MULTI_LINE_CONTEXT_EDIT)) {
            val layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            val childParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            childParams.setMargins(marginSmall!!, marginSmall!!, marginSmall!!, marginSmall!!)
            val wrapper = LinearLayout(this)
            wrapper.layoutParams = layoutParams
            wrapper.weightSum = 2f
            wrapper.orientation = if (inputType == LineInputType.MULTI_LINE_CONTEXT_EDIT) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
            val label = SreTextView(this, wrapper, getString(R.string.label, labelText), BORDERLESS_DARK)
            label.setWeight(1f)
            label.setSize(WRAP_CONTENT, if (inputType == LineInputType.MULTI_LINE_EDIT) MATCH_PARENT else WRAP_CONTENT)
            label.textAlignment = View.TEXT_ALIGNMENT_TEXT_START
            val input = SreMultiAutoCompleteTextView(this, ArrayList())
            input.setBackgroundColor(ContextCompat.getColor(this, R.color.srePrimary))
            input.setPadding(marginSmall!!, marginSmall!!, marginSmall!!, marginSmall!!)
            input.textAlignment = if (inputType == LineInputType.MULTI_LINE_CONTEXT_EDIT) View.TEXT_ALIGNMENT_TEXT_START else View.TEXT_ALIGNMENT_TEXT_END
            input.layoutParams = childParams
            input.textSize = textSize!!
            input.hint = labelText
            input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            input.setText(presetValue)
            input.setSingleLine((inputType != LineInputType.MULTI_LINE_CONTEXT_EDIT))
            wrapper.addView(label)
            wrapper.addView(input)
            inputMap[labelText] = input
            return wrapper
        } else if (CollectionsHelper.oneOf(inputType, LineInputType.SINGLE_LINE_TEXT, LineInputType.MULTI_LINE_TEXT)) {
            val layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            layoutParams.setMargins(marginSmall!!, marginSmall!!, marginSmall!!, marginSmall!!)
            val wrapper = LinearLayout(this)
            wrapper.layoutParams = layoutParams
            wrapper.orientation = if (inputType == LineInputType.MULTI_LINE_TEXT) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
            val label = SreTextView(this, wrapper, getString(R.string.label, labelText), BORDERLESS_DARK)
            label.setSize(WRAP_CONTENT, if (inputType == LineInputType.MULTI_LINE_TEXT) MATCH_PARENT else WRAP_CONTENT)
            label.textAlignment = View.TEXT_ALIGNMENT_TEXT_START
            val scrollWrapper = ScrollView(this)
            val text = SreTextView(this, scrollWrapper, presetValue, MEDIUM)
            scrollWrapper.addView(text)
            text.textAlignment = if (inputType == LineInputType.MULTI_LINE_TEXT) View.TEXT_ALIGNMENT_TEXT_START else View.TEXT_ALIGNMENT_TEXT_END
            text.setSize(WRAP_CONTENT, if (inputType == LineInputType.MULTI_LINE_TEXT) MATCH_PARENT else WRAP_CONTENT)
            text.setSingleLine((inputType != LineInputType.MULTI_LINE_TEXT))
            wrapper.addView(label)
            wrapper.addView(scrollWrapper)
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
            val label = SreTextView(this, wrapper, getString(R.string.label, labelText), BORDERLESS_DARK)
            label.setSize(WRAP_CONTENT, MATCH_PARENT)
            label.setWeight(1f)
            label.textAlignment = View.TEXT_ALIGNMENT_TEXT_START
            val spinner = Spinner(applicationContext)
            val spinnerArrayAdapter = ArrayAdapter<String>(this, R.layout.sre_spinner_item, data as Array<String>)
            spinnerArrayAdapter.setDropDownViewResource(R.layout.sre_spinner_item)
            spinner.adapter = spinnerArrayAdapter
            spinner.dropDownVerticalOffset = textSize!!.toInt()
            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val spinnerText = spinner.selectedItem as String
                    if (executable != null) {
                        return executable()
                    }
                    if (StringHelper.hasText(spinnerText)) {
                        for (t in 0 until selectionCarrier.childCount) {
                            if ((selectionCarrier.getChildAt(t) as TextView).text == spinnerText) {
                                spinner.setSelection(0)
                                return // Item already selected
                            }
                        }
                        addSpinnerSelection(spinnerText, selectionCarrier, labelText, spinner)
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    //NOP
                }
            };
            spinner.setPadding(marginSmall!!, marginSmall!!, marginSmall!!, marginSmall!!)
            spinner.layoutParams = childParams
            wrapper.addView(label)
            wrapper.addView(spinner)
            outerWrapper.addView(wrapper)
            outerWrapper.addView(selectionCarrier)
            if (StringHelper.hasText(presetValue)) {
                val split = presetValue!!.split(";")
                for (value in split) {
                    addSpinnerSelection(value, selectionCarrier, labelText, spinner)
                }
            }
            return outerWrapper
        }
        return null
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun addSpinnerSelection(spinnerText: String, selectionCarrier: LinearLayout, labelText: String, spinner: Spinner) {
        val textView = SreTextView(applicationContext, selectionCarrier, spinnerText, DARK)
        val textParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        textParams.setMargins(marginSmall!!, marginSmall!!, marginSmall!!, marginSmall!!)
        textView.layoutParams = textParams
        textView.text = spinnerText
        textView.textAlignment = View.TEXT_ALIGNMENT_CENTER
        textView.setBackgroundColor(ContextCompat.getColor(applicationContext,R.color.sreWhite))
        textView.setTextColor(ContextCompat.getColor(applicationContext,R.color.sreBlack))
        textView.setPadding(marginSmall!!, marginSmall!!, marginSmall!!, marginSmall!!)
        textView.setOnTouchListener { _, _ ->
            selectionCarrier.removeView(textView)
            multiInputMap[labelText]?.remove(textView)
            false
        }
        selectionCarrier.addView(textView)
        if (multiInputMap[labelText] == null) {
            val list = ArrayList<TextView>()
            list.add(textView)
            multiInputMap[labelText] = list
        } else {
            multiInputMap[labelText]?.add(textView)
        }
        spinner.setSelection(0)
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
                    swipeButton.dataObject is Scenario -> swipeButton.setCounter(DatabaseHelper.getInstance(applicationContext).readBulk(AbstractObject::class, swipeButton.dataObject).size, null)
                    swipeButton.dataObject is AbstractObject -> swipeButton.setCounter(DatabaseHelper.getInstance(applicationContext).readBulk(Attribute::class, (swipeButton.dataObject as AbstractObject).id).size, null)
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
        if (objectName == null) {
            return
        }
        val textPrior = getInfoTitle().text
        val textColorPrior = getInfoTitle().currentTextColor
        getInfoTitle().text = resources.getString(R.string.deleted, objectName)
        getInfoTitle().setTextColor(ContextCompat.getColor(applicationContext,R.color.srePrimaryWarn))
        Handler().postDelayed({
            getInfoTitle().text = textPrior
            getInfoTitle().setTextColor(textColorPrior)
        }, 1000)
    }

    fun isInputOpen(): Boolean{
        return (isInEditMode() || isInAddMode())
    }
    abstract fun isInEditMode(): Boolean
    abstract fun isInAddMode(): Boolean
    abstract fun isInViewMode(): Boolean
    abstract fun resetEditMode()
    abstract fun createEntity()
    abstract fun getConfiguredInfoString(): Int
    open fun isSpacingEnabled(): Boolean {
        return false
    }
    open fun isCanceling(): Boolean {
        return false
    }

    open fun getContentWrapperLayout(): ViewGroup {
        return scroll_holder_scroll
    }

    open fun getContentHolderLayout(): ViewGroup {
        return scroll_holder_linear_layout_holder
    }

    open fun getInfoWrapper(): LinearLayout {
        return scroll_holder_layout_info
    }

    open fun getInfoTitle(): TextView {
        return scroll_holder_text_info_title
    }

    open fun getInfoContentWrap(): LinearLayout {
        return scroll_holder_text_info_content_wrap
    }

    open fun getInfoContent(): TextView {
        return scroll_holder_text_info_content
    }

    open fun resetToolbar() {
        customizeToolbarText(resources.getText(R.string.icon_back).toString(), null, getLockIcon(), resources.getText(R.string.icon_info).toString(), null)
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
                scrollY = (getContentWrapperLayout() as SwipeButtonScrollView).scrollY
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

    fun getTextsFromLookupChoice(id: String): ArrayList<String> {
        val list = ArrayList<String>()
        if (!StringHelper.hasText(id)) {
            return list
        }
        val multi = multiInputMap[id]
        if (multi == null || multi.isEmpty()) {
            return list
        }
        for (text in multi) {
            list.add(text.text.toString())
        }
        return list
    }

    private var handlerId = 0L
    private fun execShowInformation(titleId: Int?, contentId: Int?) {
        if (titleId == null || contentId == null) {
            return
        }
        execMorphInfoBar(InfoState.NORMAL)
        getInfoTitle().text = resources.getString(titleId)
        getInfoContent().text = resources.getString(contentId)
        val localHandlerId = Random().nextLong()
        handlerId = localHandlerId
        Handler().postDelayed({
            if (localHandlerId == handlerId) {
                getInfoTitle().text = null
                getInfoContent().text = null
                execMorphInfoBar(InfoState.MINIMIZED)
            }
        }, 5000)
    }
}