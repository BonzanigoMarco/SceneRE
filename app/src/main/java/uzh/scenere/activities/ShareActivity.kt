package uzh.scenere.activities

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.activity_share.*
import uzh.scenere.R
import uzh.scenere.const.Constants
import uzh.scenere.const.Constants.Companion.IMPORT_DATA_FILE
import uzh.scenere.const.Constants.Companion.IMPORT_DATA_FOLDER
import uzh.scenere.const.Constants.Companion.IMPORT_FOLDER
import uzh.scenere.const.Constants.Companion.NOTHING
import uzh.scenere.const.Constants.Companion.NOT_VALIDATED
import uzh.scenere.const.Constants.Companion.VALIDATION_EMPTY
import uzh.scenere.const.Constants.Companion.VALIDATION_FAILED
import uzh.scenere.const.Constants.Companion.VALIDATION_OK
import uzh.scenere.datamodel.Project
import uzh.scenere.datamodel.ShareWrapper
import uzh.scenere.datamodel.Walkthrough
import uzh.scenere.datamodel.steps.AbstractStep
import uzh.scenere.datamodel.trigger.AbstractTrigger
import uzh.scenere.helpers.*
import uzh.scenere.views.SreButton
import uzh.scenere.views.SreContextAwareTextView
import uzh.scenere.views.SreTextView
import uzh.scenere.views.SwipeButton
import java.io.File


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

    override fun resetToolbar() {
        customizeToolbarId(R.string.icon_back, null, null, R.string.icon_info, null)
    }

    enum class ShareMode(private val index: Int, val label: String, val enabled: Boolean) {
        FILE_EXPORT(1,"File Export Mode",true),
        FILE_IMPORT(2,"File Import Mode",true),
        NFC_EXPORT(3,"NFC Export Mode",false),
        NFC_IMPORT(4, "NFC Import Mode",false);

        fun next(mode: ShareMode = this): ShareMode{
            if (mode.index >= ShareMode.values().size){
                if (ShareMode.values().first().enabled){
                    return ShareMode.values().first()
                }
                return next(ShareMode.values().first())
            }
            if (ShareMode.values()[mode.index].enabled){
                return ShareMode.values()[mode.index]
            }
            return next(ShareMode.values()[mode.index])
        }

        fun previous(mode: ShareMode = this): ShareMode{
            if (mode.index <= 1){
                if (ShareMode.values().last().enabled){
                    return ShareMode.values().last()
                }
                return next(ShareMode.values().last())
            }
            if (ShareMode.values()[mode.index-2].enabled){
                return ShareMode.values()[mode.index-2]
            }
            return next(ShareMode.values()[mode.index-2])
        }
    }

    private var mode: ShareMode = ShareMode.values().last()
    private var importFolder: String = NOTHING
    private var controlButton: SreButton? = null
    private var controlInput: SreTextView? = null
    private var cachedBinary: ByteArray? = null
    private val fileMap =  HashMap<File,SreButton>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        creationButton = SwipeButton(this, "File Export Mode")
                .setColors(ContextCompat.getColor(applicationContext,R.color.sreWhite), ContextCompat.getColor(applicationContext,R.color.srePrimaryDisabledDark))
                .setButtonMode(SwipeButton.SwipeButtonMode.DOUBLE)
                .setButtonIcons(R.string.icon_backward, R.string.icon_forward, R.string.icon_null, R.string.icon_null, null)
                .setButtonStates(true,true, false, false)
                .adaptMasterLayoutParams(true)
                .setFirstPosition()
                .setExecutable(object: SwipeButton.SwipeButtonExecution{
                    override fun execLeft() {
                        swapMode(false)
                    }
                    override fun execRight() {
                        swapMode(true)
                    }
                })
                .setAutoCollapse(true)
                .updateViews(true)
        share_layout_button_holder.addView(creationButton)
        swapMode(true)
        importFolder = DatabaseHelper.getInstance(applicationContext).read(IMPORT_FOLDER,String::class, NOTHING)
        resetToolbar()
    }

    private fun swapMode(forward: Boolean){
        mode = if (forward) mode.next() else mode.previous()
        creationButton?.setText(mode.label)
        when (mode){
            ShareMode.FILE_EXPORT -> {
                execLoadFileExport()
            }
            ShareMode.FILE_IMPORT -> {
                execLoadFileImport()
            }
            ShareMode.NFC_EXPORT -> {}
            ShareMode.NFC_IMPORT -> {}
        }
    }

    private fun execLoadFileExport() {
        getContentHolderLayout().removeAllViews()
        controlButton = SreButton(applicationContext, getContentHolderLayout(), "Collect your Data").addExecutable {
            cachedBinary = exportDatabaseToBinary()
            if (cachedBinary != null) {
                createStatisticsFromCachedBinary()
                controlButton?.text = "Export Data"
                controlButton?.addExecutable {
                    val fileName = "scenere_export_" + DateHelper.getCurrentTimestamp()
                    val destination = FileHelper.writeFile(applicationContext, cachedBinary!!, fileName)
                    notify("File Export Location:", destination + fileName)
                    controlButton?.text = "Open Export Location"
                    controlButton?.addExecutable {
                        FileHelper.openFolder(applicationContext, destination)
                    }
                }
            }
        }
        getContentHolderLayout().addView(controlButton)
    }

    private fun createStatisticsFromCachedBinary(): ShareWrapper {
        val wrapper = analyzeBinary(cachedBinary!!)
        val statistics = SreContextAwareTextView(applicationContext, getContentHolderLayout(), arrayListOf("Owner", "Size", "Creation Date", "Content", "File-Format", "Project(s)", "Stakeholder(s)", "Object(s)", "Attribute(s)", "Scenario(s)", "Path(s)", "Step(s)", "Trigger(s)", "Walkthrough(s)"), ArrayList())
        when (wrapper.validationCode){
                VALIDATION_OK -> {
                    statistics.text = wrapper.statistics
                }
                VALIDATION_EMPTY, NOT_VALIDATED -> {
                    statistics.text = "Wrong File-Format or Content is missing!"
                }
                VALIDATION_FAILED -> {
                    statistics.text = "Wrong File-Format!"
                }
        }
        statistics.isEnabled = false
        getContentHolderLayout().addView(statistics)
        return wrapper
    }

    private fun execLoadFileImport() {
        getContentHolderLayout().removeAllViews()
        controlButton = SreButton(applicationContext, getContentHolderLayout(), "Locate File in Import Folder").addExecutable { openFileInput() }
        controlInput = SreTextView(applicationContext, getContentHolderLayout(), if (StringHelper.hasText(importFolder)) importFolder else "No Folder selected")
        getContentHolderLayout().addView(controlButton)
        getContentHolderLayout().addView(controlInput)
        loadImportFolder()
    }

    private fun loadImportFolder(){
        if (StringHelper.hasText(importFolder)){
            val filesInFolder = FileHelper.getFilesInFolder(importFolder)
            var upperLimit = filesInFolder.size
            if (upperLimit > 100){
                notify("Too many Files","More than 100 Files in current Folder... remove some!")
                upperLimit = 100
            }
            for (f in 0 until upperLimit){
                val file = filesInFolder[f]
                if (!fileMap.containsKey(file)){
                    val button = SreButton(applicationContext, getContentHolderLayout(), file.name).addExecutable { execLoadFile(file) }
                    getContentHolderLayout().addView(button)
                    fileMap[file] = button
                }else{
                    getContentHolderLayout().addView(fileMap[file])
                }
            }
        }
    }

    private fun execLoadFile(file: File){
        getContentHolderLayout().removeAllViews()
        cachedBinary = FileHelper.readFile(applicationContext, file.path)
        val buttonWrap = LinearLayout(applicationContext)
        buttonWrap.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT)
        buttonWrap.orientation = LinearLayout.HORIZONTAL
        buttonWrap.gravity = Gravity.CENTER
        val backButton = SreButton(applicationContext, getContentHolderLayout(), "Return").addExecutable { execLoadFileImport() }
        buttonWrap.addView(backButton)
        if (cachedBinary != null){
            val wrapper = createStatisticsFromCachedBinary()
            if (wrapper.validationCode == VALIDATION_OK){
                val importButton = SreButton(applicationContext, getContentHolderLayout(), "Import").addExecutable { importBinaryToDatabase(wrapper) }
                buttonWrap.addView(importButton)
            }
        }
        getContentHolderLayout().addView(buttonWrap)
    }

    private fun importBinaryToDatabase(binary: ByteArray?) {
        //TODO
    }

    private fun importBinaryToDatabase(wrapper: ShareWrapper) {
        //TODO
    }

    private fun exportDatabaseToBinary(): ByteArray {
        val shareWrapper = ShareWrapper()
        val list = ArrayList<Project>()
        val projects = DatabaseHelper.getInstance(applicationContext).readBulk(Project::class, null)
        for (p in projects) {
            val project = DatabaseHelper.getInstance(applicationContext).readFull(p.id, Project::class)
            if (project != null) {
                list.add(project.reloadScenarios(applicationContext))
            }
        }
        // Projects & Time
        shareWrapper.withProjects(list.toTypedArray()).withTimestamp()
        // Name
        val userName = DatabaseHelper.getInstance(applicationContext).read(Constants.USER_NAME, String::class, NOTHING)
        shareWrapper.withOwner(userName)
        // Walkthroughs
        val walkthroughs = DatabaseHelper.getInstance(applicationContext).readBulk(Walkthrough::class, null)
        shareWrapper.withWalkthroughs(walkthroughs.toTypedArray())
        return DataHelper.toByteArray(shareWrapper)
    }

    private fun analyzeBinary(bytes: ByteArray): ShareWrapper{
        val wrapper: ShareWrapper?
        try{
            wrapper = ObjectHelper.nvl(DataHelper.toObject(bytes, ShareWrapper::class), ShareWrapper().validate(VALIDATION_EMPTY))
        }catch(e: Exception){
            return ShareWrapper().validate(VALIDATION_FAILED)
        }
        val size = bytes.size / 1000
        var projectCount = 0
        var stakeholderCount = 0
        var objectCount = 0
        var attributeCount = 0
        var scenarioCount = 0
        var pathCount = 0
        var stepCount = 0
        var triggerCount = 0
        var walkthroughCount = 0
        var userName = NOTHING
        var timeStamp = NOTHING
        walkthroughCount = wrapper.walkthroughArray.size
        userName = wrapper.owner
        timeStamp = DateHelper.toTimestamp(wrapper.timeMs, "dd.MM.yyyy HH:mm:ss")
        for (project in wrapper.projectArray) {
            projectCount++
            stakeholderCount += project.stakeholders.size
            for (scenario in project.scenarios) {
                scenarioCount++
                for (o in scenario.objects) {
                    objectCount++
                    attributeCount += o.attributes.size
                }
                for (shPath in scenario.paths.entries) {
                    for (path in shPath.value.entries) {
                        pathCount++
                        for (element in path.value.elements) {
                            if (element.value is AbstractStep) {
                                stepCount++
                            } else if (element.value is AbstractTrigger) {
                                triggerCount++
                            }
                        }
                    }
                }
            }
        }
        wrapper.statistics = " Owner: $userName\n" +
                " Size: $size kB \n" +
                " Creation Date: $timeStamp\n" +
                " Content:\n" +
                " $projectCount Project(s),\n" +
                " $stakeholderCount Stakeholder(s),\n" +
                " $objectCount Object(s),\n" +
                " $attributeCount Attribute(s),\n" +
                " $scenarioCount Scenario(s),\n" +
                " $pathCount Path(s),\n" +
                " $stepCount Step(s), \n" +
                " $triggerCount Trigger(s), \n" +
                " $walkthroughCount Walkthrough(s)."
        return wrapper.validate(if (projectCount==0) VALIDATION_EMPTY else VALIDATION_OK)
        }

    private fun openFileInput() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "resource/folder"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        val finalIntent = Intent.createChooser(intent, "Select a File for Import")
        if (finalIntent.resolveActivity(packageManager) != null) {
            startActivityForResult(finalIntent, IMPORT_DATA_FILE)
        }
    }

    fun openFolderInput() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        val finalIntent = Intent.createChooser(intent, "Select a Folder for Import")
        if (finalIntent.resolveActivity(packageManager) != null) {
            startActivityForResult(finalIntent, IMPORT_DATA_FOLDER);
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            IMPORT_DATA_FILE -> {
                if (resultCode == Activity.RESULT_OK && data != null && data.data is Uri) {
                    val filePath = (data.data as Uri).path
                    importFolder = FileHelper.removeFileFromPath(filePath)
                    controlInput?.text = importFolder
                    DatabaseHelper.getInstance(applicationContext).write(IMPORT_FOLDER,importFolder)
                    execLoadFileImport()
                }
            }
            IMPORT_DATA_FOLDER -> {
                if (resultCode == Activity.RESULT_OK && data != null && data.data is Uri) {
                    val filePath = (data.data as Uri).path
                    controlInput?.text = filePath
                    DatabaseHelper.getInstance(applicationContext).write(IMPORT_FOLDER,importFolder)
                    execLoadFileImport()
                }
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }
}