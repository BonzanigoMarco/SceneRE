package uzh.scenere.activities

import android.annotation.SuppressLint
import android.graphics.Color
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
import uzh.scenere.views.SwipeButton
import uzh.scenere.views.WeightAnimator
import java.util.RandomAccess

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
                createTitle("", holder_linear_layout_holder)
            }
            holder_scroll.fullScroll(View.FOCUS_DOWN)
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
            for (v in 0 until holder_linear_layout_holder.childCount) {
                if (holder_linear_layout_holder.getChildAt(v) is SwipeButton) {
                    (holder_linear_layout_holder.getChildAt(v) as SwipeButton).setButtonStates(lockState == LockState.UNLOCKED, true, true, true).updateViews(false)
                }
            }
        }
    }

    override fun onToolbarRightClicked() { //CLOSE
        if (isInEditMode()) {
            execMorphInfoBar(InfoState.MINIMIZED)
            holder_text_info_title.text = StringHelper.styleString(getSpannedStringFromId(getConfiguredInfoString()), fontAwesome)
            holder_text_info_content.text = ""
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
        SINGLE_LINE_TEXT, MULTI_LINE_TEXT, LOOKUP
    }

    @Suppress("UNCHECKED_CAST")
    protected fun createLine(labelText: String, inputType: LineInputType, presetValue: String? = null, data: Any? = null): View? {
        if (CollectionsHelper.oneOf(inputType, LineInputType.SINGLE_LINE_TEXT, LineInputType.MULTI_LINE_TEXT)) {
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
            val input = EditText(this)
            input.setBackgroundColor(ContextCompat.getColor(this, R.color.srePrimary))
            input.setPadding(marginSmall!!, marginSmall!!, marginSmall!!, marginSmall!!)
            input.textAlignment = if (inputType == LineInputType.MULTI_LINE_TEXT) View.TEXT_ALIGNMENT_TEXT_START else View.TEXT_ALIGNMENT_TEXT_END
            input.layoutParams = childParams
            input.textSize = textSize!!
            input.hint = labelText
            input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            input.setText(presetValue)
            input.setSingleLine((inputType != LineInputType.MULTI_LINE_TEXT))
            wrapper.addView(label)
            wrapper.addView(input)
            inputMap[labelText] = input
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
            spinner.textAlignment = if (inputType == LineInputType.MULTI_LINE_TEXT) View.TEXT_ALIGNMENT_TEXT_START else View.TEXT_ALIGNMENT_TEXT_END
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
        for (v in 0 until holder_linear_layout_holder.childCount) {
            if (holder_linear_layout_holder.getChildAt(v) is SwipeButton) {
                val swipeButton = holder_linear_layout_holder.getChildAt(v) as SwipeButton
                if (swipeButton.state != SwipeButton.SwipeButtonState.MIDDLE) {
                    swipeButton.collapse()
                }
                if (swipeButton.dataObject is Project) {
                    swipeButton.setCounter(DatabaseHelper.getInstance(applicationContext).readBulk(Stakeholder::class, swipeButton.dataObject).size,
                            DatabaseHelper.getInstance(applicationContext).readBulk(Scenario::class, swipeButton.dataObject).size)
                } else if (swipeButton.dataObject is Scenario) {
                    swipeButton.setCounter(DatabaseHelper.getInstance(applicationContext).readBulk(Object::class, swipeButton.dataObject).size, null)
                } else if (swipeButton.dataObject is Object) {
                    swipeButton.setCounter(DatabaseHelper.getInstance(applicationContext).readBulk(Attribute::class, (swipeButton.dataObject as Object).id).size, null)
                }
            }
        }
    }

    protected fun cleanInfoHolder(titleText: String) {
        customizeToolbarText(resources.getString(R.string.icon_check), null, null, null, resources.getString(R.string.icon_cross))
        holder_text_info_title.text = titleText
        holder_text_info_content.visibility = View.GONE
        removeExcept(holder_text_info_content_wrap, holder_text_info_content)
        inputMap.clear()
        holder_text_info_content_wrap.orientation = LinearLayout.VERTICAL
        val layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        holder_text_info_content_wrap.layoutParams = layoutParams
    }

    protected fun showDeletionConfirmation(objectName: String?) {
        if (objectName == null){
            return
        }
        val textPrior = holder_text_info_title.text
        val textColorPrior = holder_text_info_title.currentTextColor
        holder_text_info_title.text = resources.getString(R.string.deleted, objectName)
        holder_text_info_title.setTextColor(Color.RED)
        Handler().postDelayed({
            holder_text_info_title.text = textPrior
            holder_text_info_title.setTextColor(textColorPrior)
        }, 1000)
    }

    private fun removeExcept(holder: ViewGroup, exception: View) {
        if (holder.childCount == 0)
            return
        if (holder.childCount == 1 && holder.getChildAt(0) == exception)
            return
        if (holder.getChildAt(0) != exception) {
            holder.removeViewAt(0)
        } else {
            holder.removeViewAt(holder.childCount - 1)
        }
        removeExcept(holder, exception)
    }

    abstract fun isInEditMode(): Boolean
    abstract fun isInViewMode(): Boolean
    abstract fun resetEditMode()
    abstract fun createEntity()
    abstract fun getConfiguredInfoString(): Int
    open fun isSpacingEnabled(): Boolean {
        return true
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
                holder_scroll.layoutParams = createLayoutParams(1f)
                holder_layout_info.layoutParams = createLayoutParams(9f)
                createLayoutParams(0f, holder_text_info_title)
                holder_text_info_content_wrap.layoutParams = createLayoutParams(1f)
                infoState = InfoState.MINIMIZED
                return resources.getText(R.string.icon_win_min)
            }
            InfoState.MINIMIZED -> {
                WeightAnimator(holder_layout_info, 9f, 250).play()
                WeightAnimator(holder_scroll, 1f, 250).play()
                createLayoutParams(0f, holder_text_info_title)
                holder_text_info_content_wrap.layoutParams = createLayoutParams(1f)
                execMinimizeKeyboard()
                return resources.getText(R.string.icon_win_min)
            }
            InfoState.NORMAL -> {
                WeightAnimator(holder_scroll, 3f, 250).play()
                WeightAnimator(holder_layout_info, 7f, 250).play()
                createLayoutParams(2f, holder_text_info_title, 1)
                holder_text_info_content_wrap.layoutParams = createLayoutParams(1f)
                holder_text_info_content.maxLines = contentDefaultMaxLines
                return resources.getText(R.string.icon_win_norm)
            }
            InfoState.MAXIMIZED -> {
                WeightAnimator(holder_scroll, 10f, 250).play()
                WeightAnimator(holder_layout_info, 0f, 250).play()
                createLayoutParams(2.7f, holder_text_info_title, 1)
                holder_text_info_content_wrap.layoutParams = createLayoutParams(0.3f)
                holder_text_info_content.maxLines = 10
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