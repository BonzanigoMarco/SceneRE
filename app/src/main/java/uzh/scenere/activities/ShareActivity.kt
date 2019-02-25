package uzh.scenere.activities

import android.os.Bundle
import kotlinx.android.synthetic.main.activity_share.*
import uzh.scenere.R
import uzh.scenere.datamodel.Project
import uzh.scenere.datamodel.steps.AbstractStep
import uzh.scenere.datamodel.trigger.AbstractTrigger
import uzh.scenere.helpers.DataHelper
import uzh.scenere.helpers.DatabaseHelper
import uzh.scenere.helpers.DateHelper
import uzh.scenere.helpers.FileHelper
import uzh.scenere.views.SreButton
import uzh.scenere.views.SreTextView
import java.lang.Exception

class ShareActivity : AbstractManagementActivity() {

    override fun isInEditMode(): Boolean {
        return false
    }

    override fun isInAddMode(): Boolean {
        return false
    }

    override fun isInViewMode(): Boolean {
        return true
    }

    override fun resetEditMode() {
        //NOP
    }

    override fun createEntity() {
        //NOP
    }

    override fun getConfiguredInfoString(): Int {
        return R.string.icon_null
    }

    override fun getConfiguredLayout(): Int {
        return R.layout.activity_share
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bytes = exportDatabaseToBinary()
        importBinaryToDatabase(bytes)
        resetToolbar()
    }

    private fun exportDatabaseToBinary(): ByteArray {
        val list = ArrayList<Project>()
        val projects = DatabaseHelper.getInstance(applicationContext).readBulk(Project::class, null)
        for (p in projects) {
            val project = DatabaseHelper.getInstance(applicationContext).readFull(p.id, Project::class)
            if (project != null) {
                list.add(project.reloadScenarios(applicationContext))
            }
        }
        val bytes = DataHelper.toByteArray(list.toTypedArray())
        val fileName = "scenere_export_" + DateHelper.getCurrentTimestamp()
        val destination = FileHelper.writeFile(applicationContext, bytes, fileName)
        notify("File write Location:",destination+fileName)
        share_layout_button_holder.addView(SreButton(applicationContext,share_layout_button_holder,"Open Export Folder").addExecutable { FileHelper.openFolder(applicationContext,destination) })
        return bytes
    }

    @Suppress("UNCHECKED_CAST")
    private fun importBinaryToDatabase(bytes: ByteArray, testMode: Boolean = true) {
        val obj = DataHelper.toObject(bytes, Any::class)
        var projectArray: Array<Project> = emptyArray()
        if (obj != null) {
            try {
                projectArray = (obj as Array<Project>)
            } catch (e: Exception) {
                //NOP
            }
        }
        var projectCount = 0
        var stakeholderCount = 0
        var objectCount = 0
        var attributeCount = 0
        var scenarioCount = 0
        var pathCount = 0
        var stepCount = 0
        var triggerCount = 0
        for (project in projectArray){
            projectCount++
            stakeholderCount += project.stakeholders.size
            for (scenario in project.scenarios){
                scenarioCount ++
                for (o in scenario.objects){
                    objectCount ++
                    attributeCount += o.attributes.size
                }
                for (shPath in scenario.paths.entries){
                    for (path in shPath.value.entries){
                        pathCount++
                        for (element in path.value.elements){
                            if (element.value is AbstractStep){
                                stepCount++
                            }else if (element.value is AbstractTrigger){
                                triggerCount++
                            }
                        }
                    }
                }
            }
        }
        val statistic = "Data contains\n $projectCount Project(s),\n" +
                " $stakeholderCount Stakeholder(s),\n" +
                " $objectCount Object(s),\n" +
                " $attributeCount Attribute(s),\n" +
                " $scenarioCount Scenario(s),\n" +
                " $pathCount Path(s),\n" +
                " $stepCount Step(s) and\n" +
                " $triggerCount Trigger(s)."

        share_layout_button_holder.addView(SreTextView(applicationContext,share_layout_button_holder,statistic))

    }
}