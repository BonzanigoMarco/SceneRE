package uzh.scenere.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.Sensor
import android.os.Bundle
import android.os.Handler
import android.widget.Toast
import kotlinx.android.synthetic.main.scroll_holder.*
import kotlinx.android.synthetic.main.sre_toolbar.*
import uzh.scenere.R
import uzh.scenere.const.Constants.Companion.PERMISSION_REQUEST_ALL
import uzh.scenere.const.Constants.Companion.PERMISSION_REQUEST_GPS
import uzh.scenere.helpers.CommunicationHelper
import uzh.scenere.helpers.PermissionHelper
import uzh.scenere.sensors.SensorHelper
import uzh.scenere.views.SwipeButton


class CockpitActivity : AbstractBaseActivity() {
    enum class CockpitMode(var id: Int, var label: String, var description: Int) {
        PERMISSIONS(0, "Missing Permissions", R.string.cockpit_info_permissions),
        COMMUNICATIONS(1, "Communication Systems", R.string.cockpit_info_communications),
        SENSORS(2, "Available Sensors", R.string.cockpit_info_sensors);

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
    private var activeButton: SwipeButton? = null

    override fun getConfiguredLayout(): Int {
        return R.layout.activity_cockpit
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSupportActionBar(findViewById(R.id.cockpit_toolbar))
        recreateViews()
    }

    private fun recreateViews() {
        holder_text_info_content.text = mode.getDescription(applicationContext)
        holder_linear_layout_holder.removeAllViews()
        createTitle(mode.label,holder_linear_layout_holder)
        when (this.mode) {
            CockpitMode.PERMISSIONS -> {
                for (permission in PermissionHelper.getRequiredPermissions(this)) {
                    val granted = PermissionHelper.check(this, permission)
                    val swipeButton = SwipeButton(applicationContext, permission.label)
                            .setButtonMode(SwipeButton.SwipeButtonMode.DOUBLE)
                            .setIndividualButtonColors(if (granted) Color.GREEN else Color.RED, Color.WHITE, Color.GRAY, Color.GRAY)
                            .setButtonIcons(if (granted) R.string.icon_check else R.string.icon_cross, R.string.icon_sign, null, null, null)
                            .setButtonStates(true, true, false, false)
                            .updateViews(false)
                    swipeButton.dataObject = permission
                    swipeButton.outputObject = holder_text_info_content
                    swipeButton.setExecutable(generatePermissionExecutable(permission, swipeButton))
                    holder_linear_layout_holder.addView(swipeButton)
                    createTitle("",holder_linear_layout_holder) //Spacer
                }
            }
            CockpitMode.COMMUNICATIONS -> {
                for (communication in CommunicationHelper.getCommunications()) {
                    val granted = CommunicationHelper.check(this, communication)
                    val swipeButton = SwipeButton(this, communication.label)
                            .setButtonMode(SwipeButton.SwipeButtonMode.DOUBLE)
                            .setIndividualButtonColors(if (granted) Color.WHITE else Color.RED, if (granted) Color.GREEN else Color.WHITE, Color.GRAY, Color.GRAY)
                            .setButtonIcons(R.string.icon_cross, R.string.icon_check, null, null, null)
                            .setButtonStates(true, true, false, false)
                            .updateViews(false)
                    swipeButton.dataObject = communication
                    swipeButton.outputObject = holder_text_info_content
                    swipeButton.setExecutable(generateCommunicationExecutable(communication, swipeButton))
                    holder_linear_layout_holder.addView(swipeButton)
                    createTitle("",holder_linear_layout_holder) //Spacer
                }
            }
            CockpitMode.SENSORS -> {
                for (sensor in SensorHelper.getInstance(this).getSensorArray()) {
                    val swipeButton = SwipeButton(this, SensorHelper.getInstance(this).getTypeName(sensor.name))
                            .setButtonMode(SwipeButton.SwipeButtonMode.DOUBLE)
                            .setIndividualButtonColors(Color.WHITE, Color.WHITE, Color.GRAY, Color.GRAY)
                            .setButtonIcons(R.string.icon_eye_closed, R.string.icon_eye, null, null, null)
                            .setButtonStates(true, true, false, false)
                            .updateViews(false)
                    swipeButton.dataObject = sensor
                    swipeButton.outputObject = holder_text_info_content
                    swipeButton.setExecutable(generateSensorExecutable(sensor, swipeButton))
                    holder_linear_layout_holder.addView(swipeButton)
                    createTitle("",holder_linear_layout_holder) //Spacer
                }
            }
        }
    }

    private fun generatePermissionExecutable(permission: PermissionHelper.Companion.PermissionGroups, button: SwipeButton): SwipeButton.SwipeButtonExecution {
        return object : SwipeButton.SwipeButtonExecution {
            override fun execRight() {
                if (!PermissionHelper.check(this@CockpitActivity, permission)) {
                    PermissionHelper.request(this@CockpitActivity, permission)
                    activeButton = button
                } else {
                    Handler().postDelayed({ button.collapse() }, 500)
                }
            }

            override fun execLeft() {
                holder_text_info_content.text = permission.getDescription(applicationContext)
            }

            override fun execReset() {
                holder_text_info_content.text = mode.getDescription(applicationContext)
            }
        }
    }

    private fun generateCommunicationExecutable(communication: CommunicationHelper.Companion.Communications, button: SwipeButton): SwipeButton.SwipeButtonExecution {
        return object : SwipeButton.SwipeButtonExecution {
            override fun execRight() {
                if (!CommunicationHelper.check(this@CockpitActivity, communication)) {
                    val active = CommunicationHelper.toggle(this@CockpitActivity, communication)
                    button.setIndividualButtonColors(if (active) Color.WHITE else Color.RED, if (active) Color.GREEN else Color.WHITE, Color.GRAY, Color.GRAY).updateViews(true)
                    Handler().postDelayed({ button.collapse() }, 500)
                } else {
                    Handler().postDelayed({ button.collapse() }, 500)
                }
            }

            override fun execLeft() {
                if (CommunicationHelper.check(this@CockpitActivity, communication)) {
                    val active = CommunicationHelper.toggle(this@CockpitActivity, communication)
                    button.setIndividualButtonColors(if (active) Color.WHITE else Color.RED, if (active) Color.GREEN else Color.WHITE, Color.GRAY, Color.GRAY).updateViews(true)
                    Handler().postDelayed({ button.collapse() }, 500)
                } else {
                    Handler().postDelayed({ button.collapse() }, 500)
                }
            }

            override fun execReset() {
                holder_text_info_content.text = mode.getDescription(applicationContext)
            }
        }
    }

    private fun generateSensorExecutable(sensor: Sensor, button: SwipeButton): SwipeButton.SwipeButtonExecution {
        return object : SwipeButton.SwipeButtonExecution {
            override fun execRight() {
                SensorHelper.getInstance(applicationContext).registerTextGraphListener(sensor, holder_text_info_content)
                Handler().postDelayed({ button.collapse() }, 500)
            }

            override fun execLeft() {
                SensorHelper.getInstance(applicationContext).unregisterTextGraphListener()
                Handler().postDelayed({ button.collapse() }, 500)
            }

            override fun execReset() {
                holder_text_info_content.text = mode.getDescription(applicationContext)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_ALL && permissions.isNotEmpty()) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                activeButton?.setIndividualButtonColors(Color.GREEN, Color.WHITE, Color.GRAY, Color.GRAY)
                        ?.setButtonIcons(R.string.icon_check, R.string.icon_sign, null, null, null)
                        ?.updateViews(true)
            } else {
                activeButton?.setIndividualButtonColors(Color.RED, Color.WHITE, Color.GRAY, Color.GRAY)
                        ?.setButtonIcons(R.string.icon_cross, R.string.icon_sign, null, null, null)
                        ?.updateViews(true)
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
        recreateViews()
    }

    override fun onToolbarRightClicked() {
        SensorHelper.getInstance(this).unregisterTextGraphListener()
        mode = mode.next()
        holder_text_info_content.text = mode.getDescription(applicationContext)
        recreateViews()
    }

    override fun onToolbarLeftClicked() {
        SensorHelper.getInstance(this).unregisterTextGraphListener()
        mode = mode.previous()
        holder_text_info_content.text = mode.getDescription(applicationContext)
        recreateViews()
    }

    override fun onToolbarMiddleClicked() {
        val mapIntent = CommunicationHelper.getMapIntent()
        if (mapIntent == null){
            toast("GPS not ready")
            return
        }
        startActivity(mapIntent)
    }

    override fun onToolbarMiddleRightClicked() {
        toolbar_action_center_right.text = execMorphInfoBar(null)
    }
}