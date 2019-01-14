package uzh.scenere.activities

import android.content.Intent
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
import uzh.scenere.datamodel.database.DatabaseHelper
import uzh.scenere.helpers.StringHelper
import uzh.scenere.helpers.getStringValue
import uzh.scenere.views.SwipeButton
import uzh.scenere.views.SwipeButton.SwipeButtonExecution


class ProjectsActivity : AbstractManagementActivity() {
    override fun getConfiguredLayout(): Int {
        return R.layout.activity_projects
    }

    enum class ProjectsMode{
        VIEW, EDIT_CREATE, OBJECT, STAKEHOLDER
    }

    private val inputMap: HashMap<String,EditText> = HashMap()
    private val inputLabelTitle = "Project Title"
    private val inputLabelDescription = "Project Description"
    private var activeProject: Project? = null

    private var creationButton: SwipeButton? = null
    private var activeButton: SwipeButton? = null
    private var projectsMode: ProjectsMode = ProjectsMode.VIEW

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        creationButton =
                SwipeButton(this,"Create New Project")
                        .setButtonMode(SwipeButton.SwipeButtonMode.DOUBLE)
                        .setColors(Color.WHITE,Color.GRAY)
                        .setButtonStates(false,true,false,false)
                        .setButtonIcons(R.string.icon_null,R.string.icon_edit,null,null,null)
                        .updateViews(true )
        creationButton!!.setExecutable(generateCreationExecutable(creationButton!!))
        holder_linear_layout_holder.addView(creationButton)
        createTitle("",holder_linear_layout_holder)
        for (project in DatabaseHelper.getInstance(applicationContext).readBulk(Project::class)){
            addProjectToList(project)
        }
        holder_text_info_title.text = StringHelper.styleString(getSpannedStringFromId(R.string.icon_explain_projects),fontAwesome)
        customizeToolbarText(null,null,getLockIcon(),null,null)
    }

    override fun onResume() {
        super.onResume()
        collapseAllButtons()
    }

    private fun collapseAllButtons() {
        for (v in 0 until holder_linear_layout_holder.childCount) {
            if (holder_linear_layout_holder.getChildAt(v) is SwipeButton &&
                    (holder_linear_layout_holder.getChildAt(v) as SwipeButton).state != SwipeButton.SwipeButtonState.MIDDLE) {
                (holder_linear_layout_holder.getChildAt(v) as SwipeButton).collapse()
            }
        }
    }

    private fun addProjectToList(project: Project) {
        val swipeButton = SwipeButton(this, project.title)
                .setColors(Color.WHITE, Color.GRAY)
                .setButtonIcons(R.string.icon_delete, R.string.icon_edit, R.string.icon_person, R.string.icon_scenario, null)
                .setButtonStates(lockState == LockState.UNLOCKED, true, true, true)
                .updateViews(true)
        swipeButton.dataObject = project
        swipeButton.setExecutable(generateProjectExecutable(swipeButton, project))
        holder_linear_layout_holder.addView(swipeButton)
    }

    private fun generateCreationExecutable(button: SwipeButton, project: Project? = null): SwipeButtonExecution {
        return object: SwipeButtonExecution{
            override fun execRight() {
                activeButton = button
                openInput(ProjectsMode.EDIT_CREATE)
            }
        }
    }

    private fun generateProjectExecutable(button: SwipeButton, project: Project? = null): SwipeButtonExecution {
        return object: SwipeButtonExecution{
            override fun execLeft() {
                if (project!=null){
                    removeProject(project)
                }
            }
            override fun execRight() {
                activeButton = button
                openInput(ProjectsMode.EDIT_CREATE,project)
            }
            override fun execUp() {
                activeButton = button
                openInput(ProjectsMode.STAKEHOLDER,project)
            }
            override fun execDown() {
                activeButton = button
                openInput(ProjectsMode.OBJECT,project)
            }
        }
    }

    private fun openInput(projectsMode: ProjectsMode, project: Project? = null) {
        activeProject = project
        this.projectsMode = projectsMode
        when(projectsMode){
            ProjectsMode.VIEW -> {}//NOP
            ProjectsMode.EDIT_CREATE -> {
                //[Title]: [TitleInput]
                //[Description]:
                //[DescriptionInput]
                cleanInfoHolder(if (activeProject==null) getString(R.string.projects_create) else getString(R.string.projects_edit))
                holder_text_info_content_wrap.addView(createLine(inputLabelTitle,false, project?.title))
                holder_text_info_content_wrap.addView(createLine(inputLabelDescription, true, project?.description))
            }
            ProjectsMode.OBJECT -> {
                holder_text_info_content_wrap.addView(createLine(inputLabelTitle,false, project?.title))
                holder_text_info_content_wrap.addView(createLine(inputLabelDescription, true, project?.description))
            }
            ProjectsMode.STAKEHOLDER -> {
                val intent = Intent(this,StakeholdersActivity::class.java)
                intent.putExtra(BUNDLE_PROJECT,project)
                startActivity(intent)
                return
            }
        }

        execMorphInfoBar(InfoState.MAXIMIZED)
        customizeToolbarText(resources.getString(R.string.icon_check), null, getLockIcon(), null, resources.getString(R.string.icon_cross))
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
        if (projectsMode == ProjectsMode.EDIT_CREATE){
            execMorphInfoBar(InfoState.MINIMIZED)
            holder_text_info_title.text = StringHelper.styleString(getSpannedStringFromId(R.string.icon_explain_projects),fontAwesome)
            holder_text_info_content.text = ""
            customizeToolbarText(null,null,getLockIcon(),null,null)
            activeProject = null
            projectsMode = ProjectsMode.VIEW
            activeButton?.collapse()
            activeButton = null
            execMinimizeKeyboard()
        }
    }

    override fun onToolbarCenterClicked() {
        adaptToolbarText(null,null,changeLockState(),null,null)
        for (v in 0 until holder_linear_layout_holder.childCount){
            if (holder_linear_layout_holder.getChildAt(v) is SwipeButton){
                (holder_linear_layout_holder.getChildAt(v) as SwipeButton).setButtonStates(lockState==LockState.UNLOCKED,true,true,true).updateViews(false)
            }
        }
    }

    override fun onToolbarLeftClicked() { //SAVE
        if (projectsMode == ProjectsMode.EDIT_CREATE){
            for (entry in inputMap){
                if (!StringHelper.hasText(entry.value.text)){
                    toast("Not all required information entered!")
                    return
                }
            }
            val title = inputMap[inputLabelTitle]!!.getStringValue()
            val description = inputMap[inputLabelDescription]!!.getStringValue()
            val projectBuilder = Project.ProjectBuilder("Jon Doe", title, description) //TODO Read from db
            if (activeProject != null){
                removeProject(activeProject!!)
                projectBuilder.copyId(activeProject!!)
            }
            val project = projectBuilder.build()
            DatabaseHelper.getInstance(applicationContext).write(project.id,project)
            addProjectToList(project)
            createTitle("",holder_linear_layout_holder)
            holder_scroll.fullScroll(View.FOCUS_DOWN)
            onToolbarRightClicked()
        }
    }

    private fun removeProject(project: Project) {
        for (viewPointer in 0 until holder_linear_layout_holder.childCount){
            if (holder_linear_layout_holder.getChildAt(viewPointer) is SwipeButton &&
                    (holder_linear_layout_holder.getChildAt(viewPointer) as SwipeButton).dataObject == project){
                holder_linear_layout_holder.removeViewAt(viewPointer)
                DatabaseHelper.getInstance(applicationContext).delete(project.id)
                return
            }
        }
    }
}