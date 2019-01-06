package uzh.scenere.activities

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import kotlinx.android.synthetic.main.scroll_holder.*
import uzh.scenere.R
import uzh.scenere.datamodel.Project
import uzh.scenere.views.SwipeButton
import uzh.scenere.views.SwipeButton.SwipeButtonExecution

class ProjectsActivity : AbstractManagementActivity() {
    override fun getConfiguredLayout(): Int {
        return R.layout.activity_projects
    }

    enum class ProjectsMode{
        VIEW, EDIT
    }

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
        for (i in 1 until 2) {
            createTitle("",holder_linear_layout_holder)
            holder_linear_layout_holder.addView(
                    SwipeButton(this,"Ye olde Project")
                            .setColors(Color.WHITE,Color.GRAY)
                            .setButtonStates(false,true,true,true)
                            .setExecutable(generateProjectExecutable())
                            .updateViews(false)
            )
        }
        createTitle("",holder_linear_layout_holder)
        customizeToolbar(null,null,null,null,null)
        holder_text_info_title.text = ""
    }

    private fun generateCreationExecutable(project: Project? = null): SwipeButtonExecution {
        return object: SwipeButtonExecution{
            override fun execRight() {
                holder_text_info_title.text = getString(R.string.projects_create)
                holder_text_info_content.text = "Name:\nDescription:\n"
                execMorphInfoBar(InfoState.MAXIMIZED)
                customizeToolbar(null,null,null,null,R.string.icon_cross)
                projectsMode = ProjectsMode.EDIT
            }
        }
    }

    override fun onToolbarRightClicked() {
        if (projectsMode == ProjectsMode.EDIT){
            execMorphInfoBar(InfoState.MINIMIZED)
            holder_text_info_title.text = ""
            holder_text_info_content.text = ""
            customizeToolbar(null,null,null,null,null)
            projectsMode = ProjectsMode.VIEW
            creationButton?.collapse()
        }
    }

    private fun generateProjectExecutable(project: Project? = null): SwipeButtonExecution {
        return object: SwipeButtonExecution{}
    }

}