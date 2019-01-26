package uzh.scenere.activities

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import kotlinx.android.synthetic.main.scroll_holder.*
import uzh.scenere.R
import uzh.scenere.const.Constants.Companion.BUNDLE_PROJECT
import uzh.scenere.datamodel.Project
import uzh.scenere.datamodel.Scenario
import uzh.scenere.datamodel.Stakeholder
import uzh.scenere.helpers.DatabaseHelper
import uzh.scenere.helpers.StringHelper
import uzh.scenere.helpers.getStringValue
import uzh.scenere.views.SwipeButton
import uzh.scenere.views.SwipeButton.SwipeButtonExecution


class ProjectsActivity : AbstractManagementActivity() {

    override fun getConfiguredInfoString(): Int {
        return R.string.icon_explain_projects
    }
    override fun getConfiguredLayout(): Int {
        return R.layout.activity_projects
    }

    enum class ProjectsMode{
        VIEW, EDIT_CREATE, SCENARIO, STAKEHOLDER
    }
    private var projectsMode: ProjectsMode = ProjectsMode.VIEW
    override fun isInViewMode(): Boolean {
        return projectsMode == ProjectsMode.VIEW
    }
    override fun isInEditMode(): Boolean {
        return projectsMode == ProjectsMode.EDIT_CREATE
    }
    override fun resetEditMode() {
        activeProject = null
        projectsMode = ProjectsMode.VIEW
    }

    private val inputLabelTitle = "Project Title"
    private val inputLabelDescription = "Project Description"
    private var activeProject: Project? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        creationButton =
                SwipeButton(this,"Create New Project")
                        .setButtonMode(SwipeButton.SwipeButtonMode.DOUBLE)
                        .setColors(Color.WHITE,Color.GRAY)
                        .setButtonStates(false,true,false,false)
                        .setButtonIcons(R.string.icon_null,R.string.icon_edit,null,null,R.string.icon_project)
                        .updateViews(true )
        creationButton!!.setExecutable(generateCreationExecutable(creationButton!!))
        scroll_holder_linear_layout_holder.addView(creationButton)
        createTitle("",scroll_holder_linear_layout_holder)
        for (project in DatabaseHelper.getInstance(applicationContext).readBulk(Project::class,null)){
            addProjectToList(project)
        }
        scroll_holder_text_info_title.text = StringHelper.styleString(getSpannedStringFromId(R.string.icon_explain_projects),fontAwesome)
        customizeToolbarText(resources.getText(R.string.icon_back).toString(),null,getLockIcon(),null,null)
    }

    private fun addProjectToList(project: Project) {
        val swipeButton = SwipeButton(this, project.title)
                .setColors(Color.WHITE, Color.GRAY)
                .setButtonIcons(R.string.icon_delete, R.string.icon_edit, R.string.icon_stakeholder, R.string.icon_scenario, null)
                .setButtonStates(lockState == LockState.UNLOCKED, true, true, true)
                .updateViews(true)
        swipeButton.dataObject = project
        swipeButton.setCounter(DatabaseHelper.getInstance(applicationContext).readBulk(Stakeholder::class,project).size,
                DatabaseHelper.getInstance(applicationContext).readBulk(Scenario::class,project).size)
        swipeButton.setExecutable(generateProjectExecutable(swipeButton, project))
        scroll_holder_linear_layout_holder.addView(swipeButton)
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
                    showDeletionConfirmation(project.title)
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
                openInput(ProjectsMode.SCENARIO,project)
            }
            override fun execReset() {
                resetEditMode()
            }
        }
    }

    override fun createEntity() {
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
                scroll_holder_text_info_content_wrap.addView(createLine(inputLabelTitle,LineInputType.SINGLE_LINE_TEXT, project?.title))
                scroll_holder_text_info_content_wrap.addView(createLine(inputLabelDescription, LineInputType.MULTI_LINE_TEXT, project?.description))
            }
            ProjectsMode.SCENARIO -> {
                val intent = Intent(this,ScenariosActivity::class.java)
                intent.putExtra(BUNDLE_PROJECT,project)
                startActivity(intent)
                return
            }
            ProjectsMode.STAKEHOLDER -> {
                val intent = Intent(this,StakeholdersActivity::class.java)
                intent.putExtra(BUNDLE_PROJECT,project)
                startActivity(intent)
                return
            }
        }

        execMorphInfoBar(InfoState.MAXIMIZED)
    }

    private fun removeProject(project: Project) {
        for (viewPointer in 0 until scroll_holder_linear_layout_holder.childCount){
            if (scroll_holder_linear_layout_holder.getChildAt(viewPointer) is SwipeButton &&
                    (scroll_holder_linear_layout_holder.getChildAt(viewPointer) as SwipeButton).dataObject == project){
                scroll_holder_linear_layout_holder.removeViewAt(viewPointer)
                DatabaseHelper.getInstance(applicationContext).delete(project.id)
                return
            }
        }
    }
}