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
import android.widget.LinearLayout.HORIZONTAL
import android.widget.LinearLayout.VERTICAL
import android.widget.TextView
import kotlinx.android.synthetic.main.scroll_holder.*
import uzh.scenere.R
import uzh.scenere.datamodel.Project
import uzh.scenere.helpers.StringHelper
import uzh.scenere.helpers.getStringValue
import uzh.scenere.views.SwipeButton
import uzh.scenere.views.SwipeButton.SwipeButtonExecution


class ProjectsActivity : AbstractManagementActivity() {
    override fun getConfiguredLayout(): Int {
        return R.layout.activity_projects
    }

    enum class ProjectsMode{
        VIEW, EDIT
    }

    private val inputMap: HashMap<String,EditText> = HashMap()
    private val inputLabelName = "Project Name"
    private val inputLabelDescription = "Project Description"
    private var activeProject: Project? = null

    private var creationButton: SwipeButton? = null
    private var projectsMode: ProjectsMode = ProjectsMode.VIEW

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        creationButton =
                SwipeButton(this,"Create New Project")
                        .setButtonMode(SwipeButton.SwipeButtonMode.DOUBLE)
                        .setColors(Color.WHITE,Color.GRAY)
                        .setButtonStates(false,true,false,false)
                        .setButtonIcons(R.string.icon_null,R.string.icon_edit,null,null,null)
                        .setExecutable(generateCreationExecutable())
                        .updateViews(false )
        holder_linear_layout_holder.addView(creationButton)
        createTitle("",holder_linear_layout_holder)
        customizeToolbar(null,null,null,null,null)
        holder_text_info_title.text = ""
    }

    private fun generateCreationExecutable(project: Project? = null): SwipeButtonExecution {
        return object: SwipeButtonExecution{
            override fun execRight() {
                openInput()
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
                openInput(project)
            }
            override fun execUp() {
                if (project!=null){
                    toast(inputLabelDescription+": "+project.description)
                }
                Handler().postDelayed({ button.collapse() }, 500)
            }
        }

    }

    private fun openInput(project: Project? = null) {
        activeProject = project
        cleanInfoHolder(getString(R.string.projects_create))
        holder_text_info_content_wrap.addView(createLine(inputLabelName,false, project?.name))
        holder_text_info_content_wrap.addView(createLine(inputLabelDescription, true, project?.description))
        execMorphInfoBar(InfoState.MAXIMIZED)
        customizeToolbar(R.string.icon_check, null, null, null, R.string.icon_cross)
        projectsMode = ProjectsMode.EDIT
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
        inputMap.put(labelText,input)
        return wrapper
    }

    override fun onToolbarRightClicked() { //CLOSE
        if (projectsMode == ProjectsMode.EDIT){
            execMorphInfoBar(InfoState.MINIMIZED)
            holder_text_info_title.text = ""
            holder_text_info_content.text = ""
            customizeToolbar(null,null,null,null,null)
            activeProject = null
            projectsMode = ProjectsMode.VIEW
            creationButton?.collapse()
        }
    }

    override fun onToolbarLeftClicked() { //SAVE
        if (projectsMode == ProjectsMode.EDIT){
            for (entry in inputMap){
                if (!StringHelper.hasText(entry.value.text)){
                    toast("Not all required information entered!")
                    return
                }
            }
            val name = inputMap[inputLabelName]!!.getStringValue()
            val description = inputMap[inputLabelDescription]!!.getStringValue()
            val projectBuilder = Project.ProjectBuilder(name, description)
            if (activeProject != null){
                removeProject(activeProject!!)
                projectBuilder.copyId(activeProject!!)
            }
            val project = projectBuilder.build()
            val swipeButton = SwipeButton(this, name)
                    .setColors(Color.WHITE, Color.GRAY)
                    .setButtonStates(true, true, true, false)
                    .updateViews(false)
            swipeButton.dataObject = project
            swipeButton.setExecutable(generateProjectExecutable(swipeButton,project))
            holder_linear_layout_holder.addView(swipeButton)
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
                return
            }
        }
    }


}