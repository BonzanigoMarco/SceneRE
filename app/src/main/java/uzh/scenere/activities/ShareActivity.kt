package uzh.scenere.activities

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.view.Gravity
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.activity_share.*
import uzh.scenere.R
import uzh.scenere.const.Constants
import uzh.scenere.const.Constants.Companion.IMPORT_DATA_FILE
import uzh.scenere.const.Constants.Companion.IMPORT_DATA_FOLDER
import uzh.scenere.const.Constants.Companion.IMPORT_FOLDER
import uzh.scenere.const.Constants.Companion.NOTHING
import uzh.scenere.const.Constants.Companion.NOT_VALIDATED
import uzh.scenere.const.Constants.Companion.SRE_FILE
import uzh.scenere.const.Constants.Companion.VALIDATION_EMPTY
import uzh.scenere.const.Constants.Companion.VALIDATION_FAILED
import uzh.scenere.const.Constants.Companion.VALIDATION_INVALID
import uzh.scenere.const.Constants.Companion.VALIDATION_NO_DATA
import uzh.scenere.const.Constants.Companion.VALIDATION_OK
import uzh.scenere.datamodel.*
import uzh.scenere.datamodel.steps.AbstractStep
import uzh.scenere.datamodel.trigger.AbstractTrigger
import uzh.scenere.helpers.*
import uzh.scenere.views.*
import java.io.File

//UP UP DOWN DOWN LEFT RIGHT LEFT RIGHT B A

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
        customizeToolbarId(R.string.icon_back, null, null, R.string.icon_null, null)
    }

    enum class ShareMode(private val index: Int, val label: String, private val enabled: Boolean) {
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
                return previous(ShareMode.values().last())
            }
            if (ShareMode.values()[mode.index-2].enabled){
                return ShareMode.values()[mode.index-2]
            }
            return previous(ShareMode.values()[mode.index-2])
        }
    }

    private var mode: ShareMode = ShareMode.values().last()
    private var importFolder: String = NOTHING
    private var controlButton: SreButton? = null
    private var controlInput: SreContextAwareTextView? = null
    private var cachedBinary: ByteArray? = null
    private val fileMap =  HashMap<File,LinearLayout>()
    private var includeWalkthroughs = true
    private var walkthroughSwitch: SreButton? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        creationButton = SwipeButton(this, getString(R.string.share_mode_file_export))
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
        tutorialOpen = SreTutorialLayoutDialog(this,screenWidth,"info_share").addEndExecutable { tutorialOpen = false }.show(tutorialOpen)
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
        controlButton = SreButton(applicationContext, getContentHolderLayout(), getString(R.string.share_collect_data)).addExecutable {
            cachedBinary = exportDatabaseToBinary()
            if (cachedBinary != null) {
                val wrapper = createStatisticsFromCachedBinary(true)
                if (wrapper.validationCode == VALIDATION_OK){
                    getContentHolderLayout().removeView(walkthroughSwitch)
                    controlButton?.text = getString(R.string.share_export_data)
                    controlButton?.addExecutable {
                        val fileName = getString(R.string.share_export_file_prefix) + DateHelper.getCurrentTimestamp() + SRE_FILE
                        val destination = FileHelper.writeFile(applicationContext, cachedBinary!!, fileName)
                        notify(getString(R.string.share_export_finished))
                        controlButton?.text = getString(R.string.share_export_location)
                        controlButton?.addExecutable {
                            FileHelper.openFolder(applicationContext, destination)
                        }
                    }
                }else{
                    controlButton?.addExecutable {}
                }
            }
        }
        includeWalkthroughs = true
        walkthroughSwitch = SreButton(applicationContext, getContentHolderLayout(), getString(R.string.share_include_walkthroughs))
        walkthroughSwitch?.addExecutable {
            includeWalkthroughs = !includeWalkthroughs
            walkthroughSwitch?.text = if (includeWalkthroughs) getString(R.string.share_include_walkthroughs) else getString(R.string.share_exclude_walkthroughs)
        }
        getContentHolderLayout().addView(controlButton)
        getContentHolderLayout().addView(walkthroughSwitch)
        getInfoTitle().text = NOTHING
    }

    private fun createStatisticsFromCachedBinary(export: Boolean): ShareWrapper {
        val wrapper = analyzeBinary(cachedBinary!!, export)
        val statistics = SreContextAwareTextView(applicationContext, getContentHolderLayout(), resources.getStringArray(R.array.share_keywords).toCollection(ArrayList()), ArrayList())
        when (wrapper.validationCode){
                VALIDATION_OK -> {
                    statistics.text = wrapper.statistics
                }
                VALIDATION_EMPTY, NOT_VALIDATED -> {
                    statistics.text = getString(R.string.share_error_empty)
                }
                VALIDATION_FAILED -> {
                    statistics.text = getString(R.string.share_error_format)
                }
                VALIDATION_INVALID -> {
                    statistics.text = getString(R.string.share_error_invalid)
                }
                VALIDATION_NO_DATA -> {
                    statistics.text = getString(R.string.share_error_no_data)
                }
        }
        statistics.isEnabled = false
        getContentHolderLayout().addView(statistics)
        getInfoTitle().text = NOTHING
        return wrapper
    }

    private fun execLoadFileImport() {
        getContentHolderLayout().removeAllViews()
        controlButton = SreButton(applicationContext, getContentHolderLayout(), getString(R.string.share_location)).addExecutable { openFileInput() }
        val sreContextAwareTextView = SreContextAwareTextView(applicationContext, getContentHolderLayout(), arrayListOf("Folder"), ArrayList())
        sreContextAwareTextView.text = if (StringHelper.hasText(importFolder)) application.getString(R.string.share_folder,importFolder) else getString(R.string.share_no_folder)
        controlInput = sreContextAwareTextView
        getContentHolderLayout().addView(controlButton)
        getContentHolderLayout().addView(controlInput)
        val files = loadImportFolder()
        getInfoTitle().text = if (files == 0) NOTHING else getString(R.string.share_click_to_import,files)
    }

    private fun loadImportFolder(): Int{
        var files: Int = 0
        if (StringHelper.hasText(importFolder)){
            val filesInFolder = FileHelper.getFilesInFolder(importFolder)
            var upperLimit = filesInFolder.size
            if (upperLimit > 100){
                notify(getString(R.string.share_too_many_files_title),getString(R.string.share_too_many_files))
                upperLimit = 100
            }
            files = upperLimit
            for (f in 0 until upperLimit){
                val file = filesInFolder[f]
                if (!fileMap.containsKey(file)){
                    val buttonWrap = LinearLayout(applicationContext)
                    buttonWrap.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT)
                    buttonWrap.orientation = LinearLayout.HORIZONTAL
                    buttonWrap.gravity = Gravity.CENTER
                    val button = IconButton(applicationContext, buttonWrap, R.string.icon_file).addExecutable { execLoadFile(file) }
                    val textView = SreTextView(applicationContext, buttonWrap, file.name)
                    textView.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT)
                    textView.setMargin(button.getTopMargin())
                    textView.setSingleLine(true)
                    textView.setWeight(4f)
                    buttonWrap.addView(button)
                    buttonWrap.addView(textView)
                    getContentHolderLayout().addView(buttonWrap)
                    fileMap[file] = buttonWrap
                }else{
                    getContentHolderLayout().addView(fileMap[file])
                }
            }
        }
        return files
    }

    private fun execLoadFile(file: File){
        getContentHolderLayout().removeAllViews()
        cachedBinary = FileHelper.readFile(applicationContext, file.path)
        val buttonWrap = LinearLayout(applicationContext)
        buttonWrap.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT)
        buttonWrap.orientation = LinearLayout.HORIZONTAL
        buttonWrap.gravity = Gravity.CENTER
        val backButton = SreButton(applicationContext, getContentHolderLayout(), getString(R.string.share_return)).addExecutable { execLoadFileImport() }
        buttonWrap.addView(backButton)
        if (cachedBinary != null){
            val wrapper = createStatisticsFromCachedBinary(false)
            if (wrapper.validationCode == VALIDATION_OK){
                if (wrapper.totalItems > wrapper.oldItems) {
                    val importNewerButton = SreButton(applicationContext, getContentHolderLayout(), if (wrapper.oldItems == 0) getString(R.string.share_import) else getString(R.string.share_import_newer)).addExecutable {
                        importBinaryToDatabase(wrapper, true)
                        notify(getString(R.string.share_import_finished), getString(R.string.share_import_number_successful,(wrapper.totalItems - wrapper.oldItems)))
                        execLoadFileImport()
                    }
                    buttonWrap.addView(importNewerButton)
                }
                if (wrapper.oldItems > 0){
                    val importAllButton = SreButton(applicationContext, getContentHolderLayout(), if (wrapper.totalItems > wrapper.oldItems) getString(R.string.share_import_all_1,wrapper.oldItems) else getString(R.string.share_import_all_2),null,null,SreButton.ButtonStyle.WARN).addExecutable {
                        importBinaryToDatabase(wrapper,false)
                        notify(getString(R.string.share_import_finished),getString(R.string.share_import_number_successful,wrapper.totalItems))
                        execLoadFileImport()

                    }
                    buttonWrap.addView(importAllButton)
                }
            }
        }
        getContentHolderLayout().addView(buttonWrap)
    }

    private fun importBinaryToDatabase(binary: ByteArray?, onlyNewer: Boolean) {
        if (binary != null) {
            try {
                val wrapper = DataHelper.toObject(binary, ShareWrapper::class)
                if (wrapper != null){
                    importBinaryToDatabase(wrapper,onlyNewer)
                }
            } catch (e: Exception) {
                //NOP
            }
        }
    }

    private fun importBinaryToDatabase(wrapper: ShareWrapper, onlyNewer: Boolean) {
        analyzeWrapper(false, wrapper,0,true,onlyNewer)
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
        val walkthroughs = if (includeWalkthroughs) DatabaseHelper.getInstance(applicationContext).readBulk(Walkthrough::class, null) else ArrayList()
        shareWrapper.withWalkthroughs(walkthroughs.toTypedArray())
        return DataHelper.toByteArray(shareWrapper)
    }

    private fun analyzeBinary(bytes: ByteArray, export: Boolean): ShareWrapper{
        val wrapper: ShareWrapper
        try{
            wrapper = ObjectHelper.nvl(DataHelper.toObject(bytes, ShareWrapper::class), ShareWrapper().validate(VALIDATION_EMPTY))
        }catch(e: Exception){
            return ShareWrapper().validate(VALIDATION_FAILED)
        }
        if (wrapper.validationCode == VALIDATION_EMPTY){
            return wrapper
        }
        return analyzeWrapper(export,wrapper,bytes.size / 1000)
    }

    private fun analyzeWrapper(export: Boolean, wrapper: ShareWrapper, size: Int = 0, write: Boolean = false, onlyNewer: Boolean = true): ShareWrapper {
        var projectNewCount = 0
        val projectList = ArrayList<Project>()
        var stakeholderNewCount = 0
        val stakeholderList = ArrayList<Stakeholder>()
        var objectNewCount = 0
        val objectList = ArrayList<AbstractObject>() //Attributes saved with Objects
        var attributeNewCount = 0
        var attributeCount = 0
        var scenarioNewCount = 0
        val scenarioList = ArrayList<Scenario>()
        var pathNewCount = 0
        val pathList = ArrayList<Path>()
        var stepNewCount = 0
        var stepCount = 0
        var triggerNewCount = 0
        var triggerCount = 0
        var walkthroughNewCount = 0
        val walkthroughList = ArrayList<Walkthrough>()
        val userName = wrapper.owner
        val timeStamp = DateHelper.toTimestamp(wrapper.timeMs, "dd.MM.yyyy HH:mm:ss")
        for (walkthrough in wrapper.walkthroughArray){
            walkthroughNewCount += addOneIfNew(walkthrough)
            walkthroughList.add(walkthrough)
        }
        for (project in wrapper.projectArray) {
            projectNewCount += addOneIfNew(project)
            projectList.add(project)
            for (stakeholder in project.stakeholders){
                stakeholderNewCount += addOneIfNew(stakeholder)
                stakeholderList.add(stakeholder)
            }
            for (scenario in project.scenarios) {
                scenarioNewCount += addOneIfNew(scenario)
                scenarioList.add(scenario)
                for (o in scenario.objects) {
                    objectNewCount += addOneIfNew(o)
                    objectList.add(o)
                    for (attribute in o.attributes){
                        attributeNewCount += addOneIfNew(attribute)
                        attributeCount ++
                    }
                }
                for (shPath in scenario.paths.entries) {
                    for (path in shPath.value.entries) {
                        pathNewCount += addOneIfNew(path.value)
                        pathList.add(path.value)
                        for (element in path.value.elements) {
                            val iElement = element.value
                            if (iElement is AbstractStep) {
                                stepNewCount += addOneIfNew(iElement)
                                stepCount++
                            } else if (iElement is AbstractTrigger) {
                                triggerNewCount += addOneIfNew(iElement)
                                triggerCount++
                            }
                        }
                    }
                }
            }
        }

        val projectCount = projectList.size
        val stakeholderCount = stakeholderList.size
        val objectCount = objectList.size
        val scenarioCount = scenarioList.size
        val pathCount = pathList.size
        val walkthroughCount = walkthroughList.size
        wrapper.statistics = if (export)
            " Owner: $userName\n" +
                    " Size: $size kB \n" +
                    " Creation Date: $timeStamp\n" +
                    " Content:\n" +
                    " $projectCount Project(s)\n" +
                    " $stakeholderCount Stakeholder(s)\n" +
                    " $objectCount Object(s)\n" +
                    " $attributeCount Attribute(s)\n" +
                    " $scenarioCount Scenario(s)\n" +
                    " $pathCount Path(s)\n" +
                    " $stepCount Step(s) \n" +
                    " $triggerCount Trigger(s) \n" +
                    " $walkthroughCount Walkthrough(s)"
        else " Owner: $userName\n" +
                " Size: $size kB \n" +
                " Creation Date: $timeStamp\n" +
                " Content:\n" +
                " $projectCount Project(s), $projectNewCount New\n" +
                " $stakeholderCount Stakeholder(s), $stakeholderNewCount New\n" +
                " $objectCount Object(s), $objectNewCount New\n" +
                " $attributeCount Attribute(s), $attributeNewCount New\n" +
                " $scenarioCount Scenario(s), $scenarioNewCount New\n" +
                " $pathCount Path(s), $pathNewCount New\n" +
                " $stepCount Step(s), $stepNewCount New \n" +
                " $triggerCount Trigger(s), $triggerNewCount New \n" +
                " $walkthroughCount Walkthrough(s), $walkthroughNewCount New "

        val valid = (projectCount > 0)
        if (write && valid){
            walkthroughList.forEach { w -> persist(w.id,w,onlyNewer)}
            projectList.forEach { p -> persist(p.id,p,onlyNewer)}
            stakeholderList.forEach { s -> persist(s.id,s,onlyNewer)}
            objectList.forEach { o -> persist(o.id,o,onlyNewer)}
            scenarioList.forEach { s -> persist(s.id,s,onlyNewer)}
            pathList.forEach { p -> persist(p.id,p,onlyNewer)}
        }
        wrapper.totalItems = (projectCount+stakeholderCount+objectCount+attributeCount+scenarioCount+pathCount+stepCount+triggerCount+walkthroughCount)
        wrapper.oldItems = wrapper.totalItems - (projectNewCount+stakeholderNewCount+objectNewCount+attributeNewCount+scenarioNewCount+pathNewCount+stepNewCount+triggerNewCount+walkthroughNewCount)
        return wrapper.validate(if (valid) VALIDATION_OK else if (export) VALIDATION_NO_DATA else VALIDATION_INVALID)
    }

    private fun addOneIfNew(versionItem: IVersionItem): Int{
        return if (DatabaseHelper.getInstance(applicationContext).isNewerVersion(versionItem)) 1 else 0
    }

    private fun persist(id: String, versionItem: IVersionItem, onlyNewer: Boolean){
        if (!onlyNewer || onlyNewer && DatabaseHelper.getInstance(applicationContext).isNewerVersion(versionItem)){
            DatabaseHelper.getInstance(applicationContext).write(id,versionItem)
        }
    }

    private fun openFileInput() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "resource/folder"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        val finalIntent = Intent.createChooser(intent, getString(R.string.share_import_file))
        if (finalIntent.resolveActivity(packageManager) != null) {
            startActivityForResult(finalIntent, IMPORT_DATA_FILE)
        }
    }

    fun openFolderInput() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        val finalIntent = Intent.createChooser(intent, getString(R.string.share_import_folder))
        if (finalIntent.resolveActivity(packageManager) != null) {
            startActivityForResult(finalIntent, IMPORT_DATA_FOLDER);
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            IMPORT_DATA_FILE,IMPORT_DATA_FOLDER -> {
                if (resultCode == Activity.RESULT_OK && data != null && data.data is Uri) {
                    val filePath = (data.data as Uri).path
                    importFolder = FileHelper.removeFileFromPath(filePath)
                    controlInput?.text = application.getString(R.string.share_folder,importFolder)
                    DatabaseHelper.getInstance(applicationContext).write(IMPORT_FOLDER,importFolder)
                    execLoadFileImport()
                }
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }
}