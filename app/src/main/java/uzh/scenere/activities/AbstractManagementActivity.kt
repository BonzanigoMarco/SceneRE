package uzh.scenere.activities

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.support.v4.content.ContextCompat
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import kotlinx.android.synthetic.main.scroll_holder.*
import uzh.scenere.R
import uzh.scenere.datamodel.Attribute
import uzh.scenere.datamodel.Object
import uzh.scenere.datamodel.Project
import uzh.scenere.datamodel.Scenario
import uzh.scenere.datamodel.Stakeholder
import uzh.scenere.helpers.DatabaseHelper
import uzh.scenere.helpers.StringHelper
import uzh.scenere.views.SwipeButton

abstract class AbstractManagementActivity : AbstractBaseActivity() {

    enum class LockState{
        LOCKED, UNLOCKED
    }
    enum class ManagementMode {
        VIEW, EDIT_CREATE, OBJECTS, EDITOR
    }

    protected val inputMap: HashMap<String, EditText> = HashMap()
    protected var lockState: LockState = LockState.LOCKED
    protected var creationButton: SwipeButton? = null
    protected var activeButton: SwipeButton? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    protected fun changeLockState(): String{
        lockState = if (lockState==LockState.LOCKED) LockState.UNLOCKED else LockState.LOCKED
        return getLockIcon()
    }

    protected fun getLockIcon(): String{
        return when(lockState){
            LockState.LOCKED -> resources.getString(R.string.icon_lock)
            LockState.UNLOCKED -> resources.getString(R.string.icon_lock_open)
        }
    }

    override fun onResume() {
        super.onResume()
        collapseAndRefreshAllButtons()
    }

    private fun collapseAndRefreshAllButtons() {
        for (v in 0 until holder_linear_layout_holder.childCount) {
            if (holder_linear_layout_holder.getChildAt(v) is SwipeButton){
                val swipeButton = holder_linear_layout_holder.getChildAt(v) as SwipeButton
                if (swipeButton.state != SwipeButton.SwipeButtonState.MIDDLE) {
                    swipeButton.collapse()
                }
                if (swipeButton.dataObject is Project){
                    swipeButton.setCounter(DatabaseHelper.getInstance(applicationContext).readBulk(Stakeholder::class,swipeButton.dataObject).size,
                            DatabaseHelper.getInstance(applicationContext).readBulk(Scenario::class,swipeButton.dataObject).size)
                }else if (swipeButton.dataObject is Scenario){
                    swipeButton.setCounter(DatabaseHelper.getInstance(applicationContext).readBulk(Object::class,swipeButton.dataObject).size,null)
                }else if (swipeButton.dataObject is Object){
                    swipeButton.setCounter(DatabaseHelper.getInstance(applicationContext).readBulk(Attribute::class,(swipeButton.dataObject as Object).id).size,null)
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

    protected fun showDeletionConfirmation(objectName: String){
        val textPrior = holder_text_info_title.text
        val textColorPrior = holder_text_info_title.currentTextColor
        holder_text_info_title.text = resources.getString(R.string.deleted,objectName)
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

    protected fun createLine(labelText: String, linebreak: Boolean = false, presetValue: String? = null): View? {
        val layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        val childParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        childParams.setMargins(marginSmall!!, marginSmall!!, marginSmall!!, marginSmall!!)
        val wrapper = LinearLayout(this)
        wrapper.layoutParams = layoutParams
        wrapper.weightSum = 2f
        wrapper.orientation = if (linebreak) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
        val label = TextView(this)
        label.text = getString(R.string.label, labelText)
        label.textSize = textSize!!
        label.layoutParams = childParams
        val input = EditText(this)
        input.setBackgroundColor(ContextCompat.getColor(this, R.color.srePrimary))
        input.setPadding(marginSmall!!, marginSmall!!, marginSmall!!, marginSmall!!)
        input.textAlignment = if (linebreak) View.TEXT_ALIGNMENT_TEXT_START else View.TEXT_ALIGNMENT_TEXT_END
        input.layoutParams = childParams
        input.textSize = textSize!!
        input.hint = labelText
        input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        input.setText(presetValue)
        input.setSingleLine(!linebreak)
        wrapper.addView(label)
        wrapper.addView(input)
        inputMap[labelText] = input
        return wrapper
    }

    abstract fun isInEditMode(): Boolean
    abstract fun isInViewMode(): Boolean
    abstract fun resetEditMode()
    abstract fun createEntity()
    abstract fun getConfiguredInfoString(): Int

    override fun onToolbarRightClicked() { //CLOSE
        if (isInEditMode()) {
            execMorphInfoBar(InfoState.MINIMIZED)
            holder_text_info_title.text = StringHelper.styleString(getSpannedStringFromId(getConfiguredInfoString()), fontAwesome)
            holder_text_info_content.text = ""
            customizeToolbarText(null, null, getLockIcon(), null, null)
            resetEditMode()
            activeButton?.collapse()
            activeButton = null
        }
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

    override fun onToolbarLeftClicked() { //SAVE
        if (isInEditMode()) {
            for (entry in inputMap) {
                if (!StringHelper.hasText(entry.value.text)) {
                    toast("Not all required information entered!")
                    return
                }
            }
            createEntity()
            createTitle("", holder_linear_layout_holder)
            holder_scroll.fullScroll(View.FOCUS_DOWN)
            onToolbarRightClicked()
        }
    }
}