package uzh.scenere.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.Sensor
import android.os.Bundle
import android.os.Handler
import android.support.v4.content.ContextCompat
import kotlinx.android.synthetic.main.sre_toolbar.*
import uzh.scenere.R
import uzh.scenere.const.Constants
import uzh.scenere.const.Constants.Companion.NOTHING
import uzh.scenere.const.Constants.Companion.PERMISSION_REQUEST_ALL
import uzh.scenere.const.Constants.Companion.PERMISSION_REQUEST_GPS
import uzh.scenere.const.Constants.Companion.PNG_FILE
import uzh.scenere.const.Constants.Companion.TUTORIAL_UID_IDENTIFIER
import uzh.scenere.helpers.CommunicationHelper
import uzh.scenere.helpers.DatabaseHelper
import uzh.scenere.helpers.PermissionHelper
import uzh.scenere.helpers.StringHelper
import uzh.scenere.sensors.SensorHelper
import uzh.scenere.views.SreTutorialLayoutDialog
import uzh.scenere.views.SwipeButton


class CockpitActivity : AbstractManagementActivity() {

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
        return R.string.icon_explain_cockpit
    }

    enum class CockpitMode(var id: Int, var label: String, var description: Int) {
        PERMISSIONS(0, "Missing Permissions", R.string.cockpit_info_permissions),
        COMMUNICATIONS(1, "Communication Systems", R.string.cockpit_info_communications),
        SENSORS(2, "Available Sensors", R.string.cockpit_info_sensors),
        FUNCTIONS(3,"Administrator Functions",R.string.cockpit_info_admin);

        fun next(): CockpitMode {
            return get((id + 1))
        }

        fun previous(): CockpitMode {
            return get((id - 1))
        }

        private fun get(id: Int): CockpitMode {
            for (m in CockpitMode.values()) {
                if (m.id == id)
                    return m
            }
            return if (id < 0) get(CockpitMode.values().size - 1) else get(0)
        }

        fun getDescription(context: Context): String {
            return context.resources.getString(description)
        }
    }

    private var mode: CockpitMode = CockpitMode.PERMISSIONS

    override fun getConfiguredLayout(): Int {
        return R.layout.activity_cockpit
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        customizeToolbarId(R.string.icon_back, R.string.icon_backward, R.string.icon_win_min, R.string.icon_forward, null)
        getInfoTitle().text = StringHelper.styleString(getSpannedStringFromId(getConfiguredInfoString()), fontAwesome)
        recreateViews()
        if (PermissionHelper.getRequiredPermissions(this).isEmpty()){
            tutorialOpen = SreTutorialLayoutDialog(this,screenWidth,"info_bars","info_toolbar").addEndExecutable { tutorialOpen = false }.show(tutorialOpen)
        }else{
            tutorialOpen = SreTutorialLayoutDialog(this,screenWidth,"info_bars", "info_toolbar","info_permissions").addEndExecutable { tutorialOpen = false }.show(tutorialOpen)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_ALL && permissions.isNotEmpty()) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                activeButton?.setIndividualButtonColors(ContextCompat.getColor(applicationContext,R.color.srePrimarySafe), ContextCompat.getColor(applicationContext,R.color.srePrimaryPastel), ContextCompat.getColor(applicationContext,R.color.srePrimaryDisabled), ContextCompat.getColor(applicationContext,R.color.srePrimaryDisabled))
                        ?.setButtonIcons(R.string.icon_check, R.string.icon_sign, null, null, null)
                        ?.updateViews(false)
            } else {
                activeButton?.setIndividualButtonColors(ContextCompat.getColor(applicationContext,R.color.srePrimaryWarn), ContextCompat.getColor(applicationContext,R.color.srePrimaryPastel), ContextCompat.getColor(applicationContext,R.color.srePrimaryDisabled), ContextCompat.getColor(applicationContext,R.color.srePrimaryDisabled))
                        ?.setButtonIcons(R.string.icon_cross, R.string.icon_sign, null, null, null)
                        ?.updateViews(false)
            }
            activeButton?.collapse()
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PERMISSION_REQUEST_GPS && resultCode == Activity.RESULT_OK){
            CommunicationHelper.registerGpsListener(this@CockpitActivity)
        }
        recreateViews()
    }

    override fun onResume() {
        super.onResume()
        // Persist Username on granting Permission
        DatabaseHelper.getInstance(applicationContext).readAndMigrate(Constants.USER_NAME, String::class, NOTHING, false)
        DatabaseHelper.getInstance(applicationContext).readAndMigrate(Constants.USER_ID, String::class, NOTHING, false)
        recreateViews()
    }

    override fun onToolbarCenterRightClicked() {
        SensorHelper.getInstance(this).unregisterTextGraphListener()
        mode = mode.next()
        getInfoContent().text = mode.getDescription(applicationContext)
        recreateViews()
    }

    override fun onToolbarCenterLeftClicked() {
        SensorHelper.getInstance(this).unregisterTextGraphListener()
        mode = mode.previous()
        getInfoContent().text = mode.getDescription(applicationContext)
        recreateViews()
    }

    override fun onToolbarCenterClicked() {
        toolbar_action_center.text = execMorphInfoBar(null)
    }

    private fun recreateViews() {
        getInfoContent().text = mode.getDescription(applicationContext)
        getContentHolderLayout().removeAllViews()
        getContentWrapperLayout().scrollTo(0,0)
        createTitle(mode.label,getContentHolderLayout())
        when (this.mode) {
            CockpitMode.PERMISSIONS -> {
                for (permission in PermissionHelper.getRequiredPermissions(this)) {
                    val swipeButton = SwipeButton(applicationContext, permission.label)
                            .setButtonMode(SwipeButton.SwipeButtonMode.DOUBLE)
                            .setButtonIcons(R.string.icon_null,R.string.icon_check, null, null, null)
                            .setButtonStates(true, true, false, false)
                            .setAutoCollapse(true)
                            .updateViews(true)
                    swipeButton.dataObject = permission
                    swipeButton.outputObject = getInfoContent()
                    swipeButton.setExecutable(generatePermissionExecutable(permission, swipeButton))
                    getContentHolderLayout().addView(swipeButton)
                    createTitle("",getContentHolderLayout()) //Spacer
                }
            }
            CockpitMode.COMMUNICATIONS -> {
                for (communication in CommunicationHelper.getCommunications()) {
                    val granted = CommunicationHelper.check(this, communication)
                    val swipeButton = SwipeButton(this, communication.label)
                            .setButtonMode(SwipeButton.SwipeButtonMode.DOUBLE)
                            .setIndividualButtonColors(if (granted) ContextCompat.getColor(applicationContext,R.color.srePrimaryPastel) else ContextCompat.getColor(applicationContext,R.color.srePrimaryWarn), if (granted) ContextCompat.getColor(applicationContext,R.color.srePrimarySafe) else ContextCompat.getColor(applicationContext,R.color.srePrimaryPastel), ContextCompat.getColor(applicationContext,R.color.srePrimaryDisabled), ContextCompat.getColor(applicationContext,R.color.srePrimaryDisabled))
                            .setButtonIcons(R.string.icon_cross, R.string.icon_check, null, null, null)
                            .setButtonStates(true, true, false, false)
                            .setAutoCollapse(true)
                            .updateViews(true)
                    swipeButton.dataObject = communication
                    swipeButton.outputObject = getInfoContent()
                    swipeButton.setExecutable(generateCommunicationExecutable(communication, swipeButton))
                    getContentHolderLayout().addView(swipeButton)
                    createTitle("",getContentHolderLayout()) //Spacer
                }
                tutorialOpen = SreTutorialLayoutDialog(this,screenWidth,"info_communications").addEndExecutable { tutorialOpen = false }.show(tutorialOpen)
            }
            CockpitMode.SENSORS -> {
                for (sensor in SensorHelper.getInstance(this).getSensorArray()) {
                    val swipeButton = SwipeButton(this, SensorHelper.getInstance(this).getTypeName(sensor.name))
                            .setButtonMode(SwipeButton.SwipeButtonMode.DOUBLE)
                            .setIndividualButtonColors(ContextCompat.getColor(applicationContext,R.color.srePrimaryPastel), ContextCompat.getColor(applicationContext,R.color.srePrimaryPastel), ContextCompat.getColor(applicationContext,R.color.srePrimaryDisabled), ContextCompat.getColor(applicationContext,R.color.srePrimaryDisabled))
                            .setButtonIcons(R.string.icon_eye_closed, R.string.icon_eye, null, null, null)
                            .setButtonStates(true, true, false, false)
                            .setAutoCollapse(true)
                            .updateViews(true)
                    swipeButton.dataObject = sensor
                    swipeButton.outputObject = getInfoContent()
                    swipeButton.setExecutable(generateSensorExecutable(sensor, swipeButton))
                    getContentHolderLayout().addView(swipeButton)
                    createTitle("",getContentHolderLayout()) //Spacer
                }
                tutorialOpen = SreTutorialLayoutDialog(this,screenWidth,"info_sensors").addEndExecutable { tutorialOpen = false }.show(tutorialOpen)
            }
            CockpitMode.FUNCTIONS -> {
                val resetTutorial = SwipeButton(this, getString(R.string.cockpit_tutorial_reset))
                        .setButtonMode(SwipeButton.SwipeButtonMode.DOUBLE)
                        .setButtonIcons(R.string.icon_null, R.string.icon_cogwheels, null, null, null)
                        .setButtonStates(false, true, false, false)
                        .setExecutable(object : SwipeButton.SwipeButtonExecution{
                            override fun execRight() {
                                DatabaseHelper.getInstance(applicationContext).deletePreferenceUids(TUTORIAL_UID_IDENTIFIER)
                                showInfoText(getString(R.string.cockpit_tutorial_reset_confirm))
                            }
                        })
                        .setAutoCollapse(true)
                        .updateViews(true)
                val disableTutorial = SwipeButton(this, getString(R.string.cockpit_tutorial_disable))
                        .setButtonMode(SwipeButton.SwipeButtonMode.DOUBLE)
                        .setButtonIcons(R.string.icon_null, R.string.icon_cogwheels, null, null, null)
                        .setButtonStates(false, true, false, false)
                        .setExecutable(object : SwipeButton.SwipeButtonExecution{
                            override fun execRight() {
                                for (imageName in applicationContext.assets.list("drawable")){
                                    DatabaseHelper.getInstance(applicationContext).write(TUTORIAL_UID_IDENTIFIER.plus(imageName.replace(PNG_FILE,NOTHING)),true,DatabaseHelper.DataMode.PREFERENCES)
                                }
                                showInfoText(getString(R.string.cockpit_tutorial_disable_confirm))
                            }
                        })
                        .setAutoCollapse(true)
                        .updateViews(true)
                val wipeData = SwipeButton(this, getString(R.string.cockpit_wipe_data))
                        .setButtonMode(SwipeButton.SwipeButtonMode.DOUBLE)
                        .setButtonIcons(R.string.icon_null, R.string.icon_cogwheels, null, null, null)
                        .setButtonStates(false, true, false, false)
                        .setExecutable(object : SwipeButton.SwipeButtonExecution{
                            override fun execRight() {
                                val userName = DatabaseHelper.getInstance(applicationContext).read(Constants.USER_NAME, String::class, Constants.NOTHING)
                                DatabaseHelper.getInstance(applicationContext).dropAndRecreateAll()
                                DatabaseHelper.getInstance(applicationContext).write(Constants.USER_NAME,userName)
                                showInfoText(getString(R.string.cockpit_wipe_data_confirm), R.color.srePrimaryWarn)
                            }
                        })
                        .setColors(ContextCompat.getColor(applicationContext,R.color.srePrimaryWarn),ContextCompat.getColor(applicationContext,R.color.srePrimaryDisabled))
                        .setAutoCollapse(true)
                        .updateViews(true)
                getContentHolderLayout().addView(resetTutorial)
                getContentHolderLayout().addView(disableTutorial)
                getContentHolderLayout().addView(wipeData)
                tutorialOpen = SreTutorialLayoutDialog(this,screenWidth,"info_functions").addEndExecutable { tutorialOpen = false }.show(tutorialOpen)
            }
        }
    }

    private fun generatePermissionExecutable(permission: PermissionHelper.Companion.PermissionGroups, button: SwipeButton): SwipeButton.SwipeButtonExecution {
        return object : SwipeButton.SwipeButtonExecution {
            override fun execRight() {
                if (!PermissionHelper.check(this@CockpitActivity, permission)) {
                    PermissionHelper.request(this@CockpitActivity, permission)
                    activeButton = button
                }
            }
            override fun execLeft() {
                getInfoContent().text = permission.getDescription(applicationContext)
            }
            override fun execReset() {
                getInfoContent().text = mode.getDescription(applicationContext)
            }
        }
    }

    private fun generateCommunicationExecutable(communication: CommunicationHelper.Companion.Communications, button: SwipeButton): SwipeButton.SwipeButtonExecution {
        return object : SwipeButton.SwipeButtonExecution {
            override fun execRight() {
                if (!CommunicationHelper.check(this@CockpitActivity, communication)) {
                    val active = CommunicationHelper.toggle(this@CockpitActivity, communication)
                    button.setIndividualButtonColors(if (active) ContextCompat.getColor(applicationContext,R.color.srePrimaryPastel) else ContextCompat.getColor(applicationContext,R.color.srePrimaryWarn), if (active) ContextCompat.getColor(applicationContext,R.color.srePrimarySafe) else ContextCompat.getColor(applicationContext,R.color.srePrimaryPastel), ContextCompat.getColor(applicationContext,R.color.srePrimaryDisabled), ContextCompat.getColor(applicationContext,R.color.srePrimaryDisabled)).updateViews(false)
                }
            }

            override fun execLeft() {
                if (CommunicationHelper.check(this@CockpitActivity, communication)) {
                    val active = CommunicationHelper.toggle(this@CockpitActivity, communication)
                    button.setIndividualButtonColors(if (active) ContextCompat.getColor(applicationContext,R.color.srePrimaryPastel) else ContextCompat.getColor(applicationContext,R.color.srePrimaryWarn), if (active) ContextCompat.getColor(applicationContext,R.color.srePrimarySafe) else ContextCompat.getColor(applicationContext,R.color.srePrimaryPastel), ContextCompat.getColor(applicationContext,R.color.srePrimaryDisabled), ContextCompat.getColor(applicationContext,R.color.srePrimaryDisabled)).updateViews(false)
                }
            }

            override fun execReset() {
                getInfoContent().text = mode.getDescription(applicationContext)
            }
        }
    }

    private fun generateSensorExecutable(sensor: Sensor, button: SwipeButton): SwipeButton.SwipeButtonExecution {
        return object : SwipeButton.SwipeButtonExecution {
            override fun execRight() {
                SensorHelper.getInstance(applicationContext).registerTextGraphListener(sensor, getInfoContent())
                getInfoTitle().text = resources.getString(R.string.observing,SensorHelper.getInstance(applicationContext).getTypeName(sensor.name))
            }

            override fun execLeft() {
                SensorHelper.getInstance(applicationContext).unregisterTextGraphListener()
                getInfoTitle().text = StringHelper.styleString(getSpannedStringFromId(getConfiguredInfoString()), fontAwesome)
            }

            override fun execReset() {
                getInfoContent().text = mode.getDescription(applicationContext)
            }
        }
    }

}