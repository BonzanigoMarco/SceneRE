package uzh.scenere.activities

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.scroll_holder.*
import uzh.scenere.R
import uzh.scenere.const.Constants.Companion.BUNDLE_PROJECT
import uzh.scenere.datamodel.Project
import uzh.scenere.datamodel.Stakeholder
import uzh.scenere.helpers.DatabaseHelper
import uzh.scenere.helpers.StringHelper
import uzh.scenere.helpers.getStringValue
import uzh.scenere.views.SwipeButton
import uzh.scenere.views.SwipeButton.SwipeButtonExecution


class StakeholdersActivity : AbstractManagementActivity() {

    override fun getConfiguredInfoString(): Int {
        return R.string.icon_explain_stakeholders
    }
    override fun getConfiguredLayout(): Int {
        return R.layout.activity_stakeholders
    }

    enum class StakeholderMode{
        VIEW, EDIT_CREATE
    }
    private var stakeholdersMode: StakeholderMode = StakeholderMode.VIEW
    override fun isInViewMode(): Boolean {
        return stakeholdersMode == StakeholderMode.VIEW
    }

    override fun isInEditMode(): Boolean {
        return stakeholdersMode == StakeholderMode.EDIT_CREATE
    }

    override fun isInAddMode(): Boolean {
        return stakeholdersMode == StakeholderMode.EDIT_CREATE
    }
    override fun resetEditMode() {
        activeStakeholder = null
        stakeholdersMode = StakeholderMode.VIEW
    }

    private val inputLabelName = "Stakeholder Name"
    private val inputLabelDescription = "Stakeholder Description"
    private var activeProject: Project? = null
    private var activeStakeholder: Stakeholder? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activeProject = intent.getSerializableExtra(BUNDLE_PROJECT) as Project
        creationButton =
                SwipeButton(this,"Create New Stakeholder")
                        .setButtonMode(SwipeButton.SwipeButtonMode.DOUBLE)
                        .setColors(Color.WHITE,Color.GRAY)
                        .setButtonStates(false,true,false,false)
                        .setButtonIcons(R.string.icon_null,R.string.icon_edit,null,null,R.string.icon_stakeholder)
                        .setFirstPosition()
                        .updateViews(true )
        creationButton!!.setExecutable(generateCreationExecutable(creationButton!!))
        scroll_holder_linear_layout_holder.addView(creationButton)
        createTitle("",scroll_holder_linear_layout_holder)
        for (stakeholder in DatabaseHelper.getInstance(applicationContext).readBulk(Stakeholder::class,activeProject)){
            addStakeholderToList(stakeholder)
        }
        scroll_holder_text_info_title.text = StringHelper.styleString(getSpannedStringFromId(R.string.icon_explain_stakeholders),fontAwesome)
        customizeToolbarText(resources.getText(R.string.icon_back).toString(),null,getLockIcon(),null,null)
    }

    private fun addStakeholderToList(stakeholder: Stakeholder) {
        val swipeButton = SwipeButton(this, stakeholder.name)
                .setColors(Color.WHITE, Color.GRAY)
                .setButtonMode(SwipeButton.SwipeButtonMode.DOUBLE)
                .setButtonIcons(R.string.icon_delete, R.string.icon_edit, null, null, null)
                .setButtonStates(lockState == LockState.UNLOCKED, true, false, false)
                .updateViews(true)
        swipeButton.dataObject = stakeholder
        swipeButton.setExecutable(generateStakeholderExecutable(swipeButton, stakeholder))
        scroll_holder_linear_layout_holder.addView(swipeButton)
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
                    removeStakeholder(stakeholder,true)
                    showDeletionConfirmation(stakeholder.name)
                }
            }
            override fun execRight() {
                activeButton = button
                openInput(StakeholderMode.EDIT_CREATE,stakeholder)
            }
            override fun execReset() {
                resetEditMode()
            }
        }
    }

    override fun createEntity() {
        val name = inputMap[inputLabelName]!!.getStringValue()
        val introduction = inputMap[inputLabelDescription]!!.getStringValue()
        val stakeholderBuilder = Stakeholder.StakeholderBuilder(activeProject!!,name, introduction) //TODO Link to Project
        if (activeStakeholder != null){
            removeStakeholder(activeStakeholder!!)
            stakeholderBuilder.copyId(activeStakeholder!!)
        }
        val stakeholder = stakeholderBuilder.build()
        DatabaseHelper.getInstance(applicationContext).write(stakeholder.id,stakeholder)
        addStakeholderToList(stakeholder)
    }

    private fun openInput(stakeholdersMode: StakeholderMode, stakeholder: Stakeholder? = null) {
        activeStakeholder = stakeholder
        this.stakeholdersMode = stakeholdersMode
        cleanInfoHolder(if (activeStakeholder==null) getString(R.string.stakeholders_create) else getString(R.string.stakeholders_edit))
        when(stakeholdersMode){
            StakeholderMode.EDIT_CREATE -> {
                scroll_holder_text_info_content_wrap.addView(createLine(inputLabelName,LineInputType.SINGLE_LINE_EDIT, stakeholder?.name))
                scroll_holder_text_info_content_wrap.addView(createLine(inputLabelDescription, LineInputType.MULTI_LINE_EDIT, stakeholder?.description))
            }
        }

        execMorphInfoBar(InfoState.MAXIMIZED)
    }

    private fun removeStakeholder(stakeholder: Stakeholder, dbRemoval: Boolean = false) {
        for (viewPointer in 0 until scroll_holder_linear_layout_holder.childCount){
            if (scroll_holder_linear_layout_holder.getChildAt(viewPointer) is SwipeButton &&
                    (scroll_holder_linear_layout_holder.getChildAt(viewPointer) as SwipeButton).dataObject == stakeholder){
                scroll_holder_linear_layout_holder.removeViewAt(viewPointer)
                if (dbRemoval){
                    DatabaseHelper.getInstance(applicationContext).delete(stakeholder.id, Stakeholder::class)
                }
                return
            }
        }
    }
}