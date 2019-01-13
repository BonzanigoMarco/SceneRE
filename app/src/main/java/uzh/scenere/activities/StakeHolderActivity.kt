package uzh.scenere.activities

import android.graphics.Color
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.LinearLayout.HORIZONTAL
import android.widget.LinearLayout.VERTICAL
import android.widget.TextView
import kotlinx.android.synthetic.main.scroll_holder.*
import uzh.scenere.R
import uzh.scenere.const.Constants.Companion.BUNDLE_PROJECT
import uzh.scenere.datamodel.Project
import uzh.scenere.datamodel.Stakeholder
import uzh.scenere.datamodel.database.DatabaseHelper
import uzh.scenere.helpers.StringHelper
import uzh.scenere.helpers.getStringValue
import uzh.scenere.views.SwipeButton
import uzh.scenere.views.SwipeButton.SwipeButtonExecution


class StakeHolderActivity : AbstractManagementActivity() {
    override fun getConfiguredLayout(): Int {
        return R.layout.activity_stakeholder
    }

    enum class StakeholderMode{
        VIEW, EDIT_CREATE, OBJECT, STAKEHOLDER
    }

    private val inputMap: HashMap<String,EditText> = HashMap()
    private val inputLabelName = "Stakeholder Name"
    private val inputLabelIntroduction = "Stakeholder Description"
    private var activeProject: Project? = null
    private var activeStakeholder: Stakeholder? = null

    private var creationButton: SwipeButton? = null
    private var activeButton: SwipeButton? = null
    private var stakeholdersMode: StakeholderMode = StakeholderMode.VIEW

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activeProject = intent.getSerializableExtra(BUNDLE_PROJECT) as Project
        creationButton =
                SwipeButton(this,"Create New Stakeholder")
                        .setButtonMode(SwipeButton.SwipeButtonMode.DOUBLE)
                        .setColors(Color.WHITE,Color.GRAY)
                        .setButtonStates(false,true,false,false)
                        .setButtonIcons(R.string.icon_null,R.string.icon_edit,null,null,null)
                        .updateViews(true )
        creationButton!!.setExecutable(generateCreationExecutable(creationButton!!))
        holder_linear_layout_holder.addView(creationButton)
        createTitle("",holder_linear_layout_holder)
//        for (stakeholder in DatabaseHelper.getInstance(applicationContext).readBulk(Stakeholder::class)){
//            addStakeholderToList(stakeholder)
//        }
//        holder_text_info_title.text = StringHelper.styleString(getSpannedStringFromId(R.string.icon_explain_stakeholders),fontAwesome)
        customizeToolbarText(null,null,getLockIcon(),null,null)
    }

    private fun addStakeholderToList(stakeholder: Stakeholder) {
        val swipeButton = SwipeButton(this, stakeholder.name)
                .setColors(Color.WHITE, Color.GRAY)
                .setButtonIcons(R.string.icon_delete, R.string.icon_edit, R.string.icon_person, R.string.icon_object, null)
                .setButtonStates(lockState == LockState.UNLOCKED, true, true, true)
                .updateViews(true)
        swipeButton.dataObject = stakeholder
        swipeButton.setExecutable(generateStakeholderExecutable(swipeButton, stakeholder))
        holder_linear_layout_holder.addView(swipeButton)
    }

    private fun generateCreationExecutable(button: SwipeButton, stakeholder: Stakeholder? = null): SwipeButtonExecution {
        return object: SwipeButtonExecution{
            override fun execRight() {
                activeButton = button
                openInput(StakeholderMode.EDIT_CREATE)
            }
        }
    }

    private fun generateStakeholderExecutable(button: SwipeButton, stakeholder: Stakeholder? = null): SwipeButtonExecution {
        return object: SwipeButtonExecution{
            override fun execLeft() {
                if (stakeholder!=null){
                    removeStakeholder(stakeholder)
                }
            }
            override fun execRight() {
                activeButton = button
                openInput(StakeholderMode.EDIT_CREATE,stakeholder)
            }
            override fun execUp() {
                activeButton = button
                openInput(StakeholderMode.STAKEHOLDER,stakeholder)
            }
            override fun execDown() {
                activeButton = button
                openInput(StakeholderMode.OBJECT,stakeholder)
            }
        }
    }

    private fun openInput(stakeholdersMode: StakeholderMode, stakeholder: Stakeholder? = null) {
        activeStakeholder = stakeholder
        this.stakeholdersMode = stakeholdersMode
        cleanInfoHolder(getString(R.string.stakeholders_create))
        when(stakeholdersMode){
            StakeholderMode.VIEW -> {}//NOP
            StakeholderMode.EDIT_CREATE -> {
                //[Title]: [TitleInput]
                //[Description]:
                //[DescriptionInput]
                holder_text_info_content_wrap.addView(createLine(inputLabelName,false, stakeholder?.name))
                holder_text_info_content_wrap.addView(createLine(inputLabelIntroduction, true, stakeholder?.introduction))
            }
            StakeholderMode.OBJECT -> {
                holder_text_info_content_wrap.addView(createLine(inputLabelName,false, stakeholder?.name))
                holder_text_info_content_wrap.addView(createLine(inputLabelIntroduction, true, stakeholder?.introduction))
            }
            StakeholderMode.STAKEHOLDER -> {
                holder_text_info_content_wrap.addView(createLine(inputLabelName,false, stakeholder?.name))
                holder_text_info_content_wrap.addView(createLine(inputLabelIntroduction, true, stakeholder?.introduction))
            }
        }

        execMorphInfoBar(InfoState.MAXIMIZED)
        customizeToolbarText(resources.getString(R.string.icon_check), null, null, null, resources.getString(R.string.icon_cross))
    }

    private fun cleanInfoHolder(titleText: String) {
        holder_text_info_title.text = titleText
        holder_text_info_content.visibility = View.GONE
        removeExcept(holder_text_info_content_wrap, holder_text_info_content)
        inputMap.clear()
        holder_text_info_content_wrap.orientation = VERTICAL
        val layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        holder_text_info_content_wrap.layoutParams = layoutParams
    }

    private fun removeExcept(holder: ViewGroup, exception: View) {
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

    private fun createLine(labelText: String, linebreak: Boolean = false, presetValue: String? = null): View? {
        val layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT)
        val childParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT,1f)
        childParams.setMargins(marginSmall!!,marginSmall!!,marginSmall!!,marginSmall!!)
        val wrapper = LinearLayout(this)
        wrapper.layoutParams = layoutParams
        wrapper.weightSum = 2f
        wrapper.orientation = if (linebreak) VERTICAL else HORIZONTAL
        val label = TextView(this)
        label.text = getString(R.string.label,labelText)
        label.textSize = textSize!!
        label.layoutParams = childParams
        val input = EditText(this)
        input.setBackgroundColor(ContextCompat.getColor(this, R.color.srePrimary))
        input.setPadding(marginSmall!!,marginSmall!!,marginSmall!!,marginSmall!!)
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

    override fun onToolbarRightClicked() { //CLOSE
        if (stakeholdersMode == StakeholderMode.EDIT_CREATE){
            execMorphInfoBar(InfoState.MINIMIZED)
            holder_text_info_title.text = StringHelper.styleString(getSpannedStringFromId(R.string.icon_explain_stakeholder),fontAwesome)
            holder_text_info_content.text = ""
            customizeToolbarText(null,null,getLockIcon(),null,null)
            activeProject = null
            stakeholdersMode = StakeholderMode.VIEW
            activeButton?.collapse()
            activeButton = null
        }
    }

    override fun onToolbarCenterClicked() { //LOCK & UNLOCK
        if (stakeholdersMode == StakeholderMode.VIEW) {
            adaptToolbarText(null, null, changeLockState(), null, null)
            for (v in 0 until holder_linear_layout_holder.childCount) {
                if (holder_linear_layout_holder.getChildAt(v) is SwipeButton) {
                    (holder_linear_layout_holder.getChildAt(v) as SwipeButton).setButtonStates(lockState == LockState.UNLOCKED, true, true, true).updateViews(false)
                }
            }
        }
    }

    override fun onToolbarLeftClicked() { //SAVE
        if (stakeholdersMode == StakeholderMode.EDIT_CREATE){
            for (entry in inputMap){
                if (!StringHelper.hasText(entry.value.text)){
                    toast("Not all required information entered!")
                    return
                }
            }
            val name = inputMap[inputLabelName]!!.getStringValue()
            val introduction = inputMap[inputLabelIntroduction]!!.getStringValue()
            val stakeholderBuilder = Stakeholder.StakeholderBuilder(activeProject!!,name, introduction) //TODO Link to Project
            if (activeProject != null){
                removeStakeholder(activeStakeholder!!)
                stakeholderBuilder.copyId(activeStakeholder!!)
            }
            val stakeholder = stakeholderBuilder.build()
            DatabaseHelper.getInstance(applicationContext).write(stakeholder.id,stakeholder)
            addStakeholderToList(stakeholder)
            createTitle("",holder_linear_layout_holder)
            holder_scroll.fullScroll(View.FOCUS_DOWN)
            onToolbarRightClicked()
        }
    }

    private fun removeStakeholder(stakeholder: Stakeholder) {
        for (viewPointer in 0 until holder_linear_layout_holder.childCount){
            if (holder_linear_layout_holder.getChildAt(viewPointer) is SwipeButton &&
                    (holder_linear_layout_holder.getChildAt(viewPointer) as SwipeButton).dataObject == stakeholder){
                holder_linear_layout_holder.removeViewAt(viewPointer)
                DatabaseHelper.getInstance(applicationContext).delete(stakeholder.id)
                return
            }
        }
    }
}