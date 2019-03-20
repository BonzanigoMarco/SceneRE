package uzh.scenere.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pManager
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import kotlinx.android.synthetic.main.activity_share.*
import uzh.scenere.R
import uzh.scenere.const.Constants
import uzh.scenere.const.Constants.Companion.FOLDER_EXPORT
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
import uzh.scenere.datastructures.ShareWrapper
import uzh.scenere.helpers.*
import uzh.scenere.views.*
import java.io.File
import java.net.ServerSocket


//UP UP DOWN DOWN LEFT RIGHT LEFT RIGHT B A

class ShareActivity : AbstractManagementActivity() {

    override fun getConfiguredRootLayout(): ViewGroup? {
        return share_root
    }

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

    override fun isUsingNfc(): Boolean {
        return true
    }

    enum class ShareMode(private val index: Int, val label: String, private val enabled: Boolean) {
        FILE_EXPORT(1,"File Export Mode",true),
        FILE_IMPORT(2,"File Import Mode",true),
        WIFI_EXPORT(3,"Wi-Fi Export Mode",true),
        WIFI_IMPORT(4, "Wi-Fi Import Mode",true);

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
    private var walkthroughSwitch: SreButton? = null
    private var includeWalkthroughs: WalkthroughExport = WalkthroughExport.INCLUDE
    private val progressBar = ProgressBar(applicationContext)
    private var activeDeviceButton: SreButton? = null
    private var activeSocket: ServerSocket? = null

    private fun attachProgressBar(grp: ViewGroup){
        detachProgressBar()
        grp.addView(progressBar)
    }

    private fun detachProgressBar(){
        if (progressBar.parent != null && progressBar.parent is ViewGroup){
            (progressBar.parent as ViewGroup).removeView(progressBar)
        }
    }

    enum class  WalkthroughExport{
        EXCLUDE,INCLUDE,ONLY;

        fun getLabelText(context: Context): String {
            return when (this){
                EXCLUDE -> context.getString(R.string.share_exclude_walkthroughs)
                INCLUDE -> context.getString(R.string.share_include_walkthroughs)
                ONLY -> context.getString(R.string.share_only_walkthroughs)
            }
        }
    }

    private var openFileUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!PermissionHelper.check(applicationContext,PermissionHelper.Companion.PermissionGroups.STORAGE)){
            startActivity(Intent(this, StartupActivity::class.java))
        }
        if (CollectionHelper.oneOf(intent.action,"android.intent.action.VIEW") &&  intent.data is Uri){
            if (intent.data.path.contains("/root")){
                val path = intent.data.path.replace("/root", NOTHING)
                openFileUri = Uri.parse(path)
            }else{
                openFileUri = intent.data
            }
        }
        creationButton = SwipeButton(this, getString(R.string.share_mode_file_export))
                .setColors(getColorWithStyle(applicationContext,R.color.srePrimaryPastel), getColorWithStyle(applicationContext,R.color.srePrimaryDisabled))
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
        if (openFileUri != null){
            if (StringHelper.nvl(openFileUri!!.authority,NOTHING).contains("dropbox")){
                notify(getString(R.string.share_no_dropbox))
            }else{
                setMode(ShareMode.FILE_IMPORT)
                execLoadFile(File(openFileUri!!.path))
            }
        }
    }

    private fun swapMode(forward: Boolean){
        mode = if (forward) mode.next() else mode.previous()
        swapModeInternal()
    }
    private fun setMode(newMode: ShareMode){
        mode = newMode
        swapModeInternal()
    }

    private fun swapModeInternal() {
        cancelAsyncTask()
        creationButton?.setText(mode.label)
        disableWifiP2p()
        when (mode) {
            ShareMode.FILE_EXPORT -> {
                execLoadFileExport()
            }
            ShareMode.FILE_IMPORT -> {
                execLoadFileImport()
            }
            ShareMode.WIFI_EXPORT -> {
                execLoadFileExport()
            }
            ShareMode.WIFI_IMPORT -> {
                execLoadWifiImport()
            }
        }
    }

    private fun execLoadFileExport() {
        cachedBinary = null
        getContentHolderLayout().removeAllViews()
        controlButton = SreButton(applicationContext, getContentHolderLayout(), getString(R.string.share_collect_data)).setExecutable {
            if (!isAsyncTaskRunning()){
                controlButton?.text = getString(R.string.share_loading)
                attachProgressBar(getContentHolderLayout())
                getContentHolderLayout().removeView(walkthroughSwitch)
            }
            cancelAsyncTask()
            executeAsyncTask({ cachedBinary = exportDatabaseToBinary()},{
                if (cachedBinary != null) {
                    val wrapper = createStatisticsFromCachedBinary(true,mode != ShareMode.WIFI_EXPORT)
                    detachProgressBar()
                    if (wrapper.validationCode == VALIDATION_OK){
                        if (mode == ShareMode.WIFI_EXPORT){
                            prepareButtonForScan()
                        }else{
                            controlButton?.text = getString(R.string.share_export_data)
                            controlButton?.setExecutable {
                                val fileName = getString(R.string.share_export_file_prefix) + DateHelper.getCurrentTimestamp() + SRE_FILE
                                val destinationFile = FileHelper.writeFile(applicationContext, cachedBinary!!, fileName, FOLDER_EXPORT)
                                notify(getString(R.string.share_export_finished))
                                controlButton?.text = getString(R.string.share_export_location)
                                controlButton?.setExecutable {
                                    val success = FileHelper.openFolder(applicationContext, destinationFile.replace("/$fileName",""))
                                    if (!success){
                                        FileHelper.openFile(applicationContext, destinationFile)
                                    }
                                }
                            }
                        }
                    }else{
                        controlButton?.setExecutable {}
                    }
                }else{
                    controlButton?.text = getString(R.string.share_collect_data)
                    notify(getString(R.string.share_collection_failed))
                    getContentHolderLayout().removeView(walkthroughSwitch)
                }
            })
        }
        includeWalkthroughs = WalkthroughExport.INCLUDE
        walkthroughSwitch = SreButton(applicationContext, getContentHolderLayout(), getString(R.string.share_include_walkthroughs))
        walkthroughSwitch?.setExecutable {
            when (includeWalkthroughs){
                WalkthroughExport.EXCLUDE -> includeWalkthroughs = WalkthroughExport.ONLY
                WalkthroughExport.INCLUDE -> includeWalkthroughs = WalkthroughExport.EXCLUDE
                WalkthroughExport.ONLY -> includeWalkthroughs = WalkthroughExport.INCLUDE
            }
            walkthroughSwitch?.text = includeWalkthroughs.getLabelText(applicationContext)
        }
        getContentHolderLayout().addView(controlButton)
        getContentHolderLayout().addView(walkthroughSwitch)
        getInfoTitle().text = NOTHING
        tutorialOpen = SreTutorialLayoutDialog(this,screenWidth,"info_share","info_export").addEndExecutable { tutorialOpen = false }.show(tutorialOpen)
    }

    private fun createStatisticsFromCachedBinary(export: Boolean, addView: Boolean = true): ShareWrapper {
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
        if (addView){
            getContentHolderLayout().addView(statistics)
        }
        getInfoTitle().text = NOTHING
        return wrapper
    }

    private fun execLoadFileImport() {
        getContentHolderLayout().removeAllViews()
        controlButton = SreButton(applicationContext, getContentHolderLayout(), getString(R.string.share_location)).setExecutable { openFileInput() }
        val sreContextAwareTextView = SreContextAwareTextView(applicationContext, getContentHolderLayout(), arrayListOf("Folder"), ArrayList())
        sreContextAwareTextView.text = if (StringHelper.hasText(importFolder)) application.getString(R.string.share_folder,importFolder) else getString(R.string.share_no_folder)
        controlInput = sreContextAwareTextView
        getContentHolderLayout().addView(controlButton)
        getContentHolderLayout().addView(controlInput)
        val files = loadImportFolder()
        getInfoTitle().text = if (files == 0) NOTHING else getString(R.string.share_click_to_import,files)
        tutorialOpen = SreTutorialLayoutDialog(this,screenWidth,"info_share","info_import").addEndExecutable { tutorialOpen = false }.show(tutorialOpen)
    }

    private fun loadImportFolder(): Int{
        var files: Int = 0
        if (StringHelper.hasText(importFolder)){
            var filesInFolder: Array<out File>? = null
            try {
                filesInFolder = FileHelper.getFilesInFolder(importFolder, ".sre")
            }catch(e: java.lang.Exception){

            }
            if (filesInFolder == null){
                notify(getString(R.string.share_too_many_files_title),getString(R.string.share_too_many_files_failed))
                return 0
            }
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
                    val button = IconButton(applicationContext, buttonWrap, R.string.icon_file).setExecutable { execLoadFile(file) }
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

    private fun prepareButtonForScan() {
        controlButton?.text = "Scan for Devices"
        controlButton?.setExecutable {
            execLoadWifiExport()
        }
    }

    private fun prepareButtonForCancel() {
        controlButton?.text = "Cancel Scan"
        controlButton?.setExecutable {
            detachProgressBar()
            stopWifiP2pDeviceDiscovery()
            prepareButtonForScan()
        }
    }

    private fun execLoadWifiExport(){
        removeExcept(getContentHolderLayout(), controlButton)
        attachProgressBar(getContentHolderLayout())
        resetWifiP2p()
        prepareButtonForCancel()
    }

    private fun execLoadWifiImport(){
        getContentHolderLayout().removeAllViews()
        resetWifiP2p()
    }

    override val handlePeerData: (WifiP2pDeviceList?) -> Unit = {
        if (mode == ShareMode.WIFI_EXPORT) {
            getContentHolderLayout().removeAllViews()
            for (device in it!!.deviceList) {
                val button = SreButton(applicationContext, getContentHolderLayout(), "Send to ${device.deviceName}")
                button.data = device
                button.setExecutable {
                    activeSocket?.close()
                    activeDeviceButton?.setTextColor(getColorWithStyle(applicationContext, R.color.srePrimaryPastel))
                    resetWifiP2p()
                    val config = WifiP2pConfig().apply {
                        deviceAddress = device.deviceAddress
                        wps.setup = WpsInfo.PBC
                    }
                    getWifiP2pManager()?.connect(channel, config, object : WifiP2pManager.ActionListener {
                        override fun onSuccess() {
//                            notify("Connected successfully")
                        }

                        override fun onFailure(reason: Int) {
//                            notify("Connection failed")
                        }
                    })
                    activeDeviceButton = button
                    activeDeviceButton?.setTextColor(getColorWithStyle(applicationContext, R.color.srePrimaryAttention))
                }
                getContentHolderLayout().addView(button)
            }
        }
    }

    override val handleOwnData: (WifiP2pDevice?) -> Unit = {
        if (mode == ShareMode.WIFI_IMPORT && it != null){
            getContentHolderLayout().removeAllViews()
            val button = SreButton(applicationContext, getContentHolderLayout(), "Your Device-ID: ${it.deviceName}")
            getContentHolderLayout().addView(button)
        }
    }

    override val collectWifiP2pDataToSend: () -> ByteArray = {
        cachedBinary ?: ByteArray(0)
    }

    override val handleWifiP2pData: (ByteArray) -> Unit = {
        notify("Received ${it.size/1000} kB!")
        cachedBinary = it
        execPrepareDataImport()
    }

    override val createWifiP2pSender: () -> ServerSocket? = {
        val serverSocket = super.createWifiP2pSender.invoke()
        activeSocket = serverSocket
        serverSocket
    }

    private fun execLoadFile(file: File){
        getContentHolderLayout().removeAllViews()
        cachedBinary = FileHelper.readFile(applicationContext, file.path)
        execPrepareDataImport()
    }

    private fun execPrepareDataImport() {
        val buttonWrap = LinearLayout(applicationContext)
        buttonWrap.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        buttonWrap.orientation = LinearLayout.HORIZONTAL
        buttonWrap.gravity = Gravity.CENTER
        val backButton = SreButton(applicationContext, getContentHolderLayout(), getString(R.string.share_return)).setExecutable {
            if (mode == ShareMode.FILE_IMPORT){
                execLoadFileImport()
            }else if (mode == ShareMode.WIFI_IMPORT){
                execLoadWifiImport()
            }
        }
        buttonWrap.addView(backButton)
        if (cachedBinary != null) {
            val wrapper = createStatisticsFromCachedBinary(false)
            if (wrapper.validationCode == VALIDATION_OK) {
                if (wrapper.totalItems > wrapper.oldItems) {
                    val importNewerButton = SreButton(applicationContext, getContentHolderLayout(), if (wrapper.oldItems == 0) getString(R.string.share_import) else getString(R.string.share_import_newer, (wrapper.totalItems - wrapper.oldItems))).setExecutable {
                        getContentHolderLayout().removeView(buttonWrap)
                        attachProgressBar(getContentHolderLayout())
                        executeAsyncTask({ importBinaryToDatabase(wrapper, true) }, {
                            detachProgressBar()
                            notify(getString(R.string.share_import_finished), getString(R.string.share_import_number_successful, (wrapper.totalItems - wrapper.oldItems)))
                            execLoadFileImport()
                        })
                    }
                    buttonWrap.addView(importNewerButton)
                }
                if (wrapper.oldItems > 0) {
                    val importAllButton = SreButton(applicationContext, getContentHolderLayout(), if (wrapper.totalItems > wrapper.oldItems) getString(R.string.share_import_all_1) else getString(R.string.share_import_all_2), null, null, SreButton.ButtonStyle.WARN).setExecutable {
                        getContentHolderLayout().removeView(buttonWrap)
                        attachProgressBar(getContentHolderLayout())
                        executeAsyncTask({ importBinaryToDatabase(wrapper, false) }, {
                            detachProgressBar()
                            notify(getString(R.string.share_import_finished), getString(R.string.share_import_number_successful, wrapper.totalItems))
                            execLoadFileImport()
                        })
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
        val walkthroughs = if (CollectionHelper.oneOf(includeWalkthroughs,WalkthroughExport.ONLY,WalkthroughExport.INCLUDE)) DatabaseHelper.getInstance(applicationContext).readBulk(Walkthrough::class, null) else ArrayList()
        shareWrapper.withWalkthroughs(walkthroughs.toTypedArray(), includeWalkthroughs == WalkthroughExport.ONLY)
        return DataHelper.toByteArray(shareWrapper)
    }

    private fun analyzeBinary(bytes: ByteArray, export: Boolean): ShareWrapper {
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
            walkthroughNewCount += addOneIfNew(walkthrough, export)
            walkthroughList.add(walkthrough)
        }
        for (project in wrapper.projectArray) {
            projectNewCount += addOneIfNew(project, export)
            projectList.add(project)
            for (stakeholder in project.stakeholders){
                stakeholderNewCount += addOneIfNew(stakeholder, export)
                stakeholderList.add(stakeholder)
            }
            for (scenario in project.scenarios) {
                scenarioNewCount += addOneIfNew(scenario, export)
                scenarioList.add(scenario)
                for (o in scenario.objects) {
                    objectNewCount += addOneIfNew(o, export)
                    objectList.add(o)
                    for (attribute in o.attributes){
                        attributeNewCount += addOneIfNew(attribute, export)
                        attributeCount ++
                    }
                }
                for (shPath in scenario.paths.entries) {
                    for (path in shPath.value.entries) {
                        pathNewCount += addOneIfNew(path.value, export)
                        pathList.add(path.value)
                        for (element in path.value.elements) {
                            val iElement = element.value
                            if (iElement is AbstractStep) {
                                stepNewCount += addOneIfNew(iElement, export)
                                stepCount++
                            } else if (iElement is AbstractTrigger) {
                                triggerNewCount += addOneIfNew(iElement, export)
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

        val valid = (projectCount > 0) || (walkthroughCount > 0)
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

    private fun addOneIfNew(versionItem: IVersionItem, export: Boolean): Int{
        if (export){
            return 1
        }
        return if (DatabaseHelper.getInstance(applicationContext).isNewerVersion(versionItem)) 1 else 0
    }

    private fun persist(id: String, versionItem: IVersionItem, onlyNewer: Boolean){
        if (!onlyNewer || onlyNewer && DatabaseHelper.getInstance(applicationContext).isNewerVersion(versionItem)){
            DatabaseHelper.getInstance(applicationContext).disableNewVersioning().write(id,versionItem)
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

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
    }
}