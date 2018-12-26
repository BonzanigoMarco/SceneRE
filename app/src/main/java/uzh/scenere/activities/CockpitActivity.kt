package uzh.scenere.activities

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.Sensor
import android.os.Bundle
import android.os.Handler
import android.support.v4.content.ContextCompat
import android.support.v7.widget.Toolbar
import android.view.*
import android.widget.LinearLayout
import android.widget.TextView
import uzh.scenere.R
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_cockpit.*
import uzh.scenere.const.Constants.Companion.PERMISSION_REQUEST_ALL
import uzh.scenere.datamodel.Project
import uzh.scenere.helpers.CommunicationHelper
import uzh.scenere.helpers.PermissionHelper
import uzh.scenere.sensors.SensorHelper
import uzh.scenere.views.SwipeButton

class CockpitActivity : AbstractBaseActivity() {
    enum class CockpitMode(var id: Int, var label: String, var description: Int) {
        PERMISSIONS(0,"Missing Permissions",R.string.cockpit_info_permissions),
        COMMUNICATIONS(1,"Communication Systems", R.string.cockpit_info_communications),
        SENSORS(2,"Available Sensors", R.string.cockpit_info_sensors);
        fun next(): CockpitMode {
            return get((id+1))
        }
        private fun get(id: Int): CockpitMode{
            for (m in CockpitMode.values()){
                if (m.id == id)
                    return m
            }
            return get(0)
        }
        fun getDescription(context: Context): String{
            return context.resources.getString(description)
        }
    }
    private var mode: CockpitMode = CockpitMode.PERMISSIONS
    private var toolbar: Toolbar? = null
    private var activeButton: SwipeButton? = null

    override fun getConfiguredLayout(): Int {
        return R.layout.activity_cockpit
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        toolbar = findViewById(R.id.cockpit_toolbar);
        setSupportActionBar(toolbar);
    }
    private fun createViews() {
        cockpit_linear_layout_holder.removeAllViews()
        createTitle(mode.label)
        when(this.mode){
            CockpitMode.PERMISSIONS -> {
                for (permission in PermissionHelper.getRequiredPermissions(this)) {
                    val granted = PermissionHelper.check(this, permission)
                    val swipeButton = SwipeButton(applicationContext, permission.label)
                            .setButtonMode(SwipeButton.SwipeButtonMode.DOUBLE)
                            .setIndividualButtonColors(if (granted) Color.GREEN else Color.RED, Color.WHITE, Color.GRAY, Color.GRAY)
                            .setButtonIcons(if (granted) R.string.icon_check else R.string.icon_cross, R.string.icon_sign, null, null)
                            .setButtonStates(true, true, false, false)
                            .updateViews(false)
                    swipeButton.dataObject = permission
                    swipeButton.outputObject = cockpit_text_info
                    swipeButton.setExecutable(generatePermissionExecutable(permission, swipeButton))
                    cockpit_linear_layout_holder.addView(swipeButton)
                }
            }
            CockpitMode.COMMUNICATIONS -> {
                for (comm in CommunicationHelper.getCommunications()){
                    val granted = CommunicationHelper.check(this, comm)
                    val swipeButton = SwipeButton(this, comm.label)
                            .setButtonMode(SwipeButton.SwipeButtonMode.DOUBLE)
                            .setIndividualButtonColors(if (granted) Color.WHITE else Color.RED, if (granted) Color.GREEN else Color.WHITE, Color.GRAY, Color.GRAY)
                            .setButtonIcons(R.string.icon_cross, R.string.icon_check, null, null)
                            .setButtonStates(true, true, false, false)
                            .updateViews(false)
                    swipeButton.dataObject = comm
                    swipeButton.outputObject = cockpit_text_info
                    swipeButton.setExecutable(generateCommunicationExecutable(comm, swipeButton))
                    cockpit_linear_layout_holder.addView(swipeButton)
                }
            }
            CockpitMode.SENSORS -> {
                for (sensor in SensorHelper.getInstance(this).getSensorArray()){
                    val swipeButton = SwipeButton(this, SensorHelper.getInstance(this).getTypeName(sensor.name))
                            .setButtonMode(SwipeButton.SwipeButtonMode.DOUBLE)
                            .setIndividualButtonColors(Color.WHITE, Color.WHITE, Color.GRAY, Color.GRAY)
                            .setButtonIcons(R.string.icon_eye_closed, R.string.icon_eye, null, null)
                            .setButtonStates(true, true, false, false)
                            .updateViews(false)
                    swipeButton.dataObject = sensor
                    swipeButton.outputObject = cockpit_text_info
                    swipeButton.setExecutable(generateSensorExecutable(sensor, swipeButton))
                    cockpit_linear_layout_holder.addView(swipeButton)
                }
            }
        }
    }

    private fun createTitle(title: String) {
        val titleText = TextView(this)
        val titleParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        titleText.layoutParams = titleParams
        titleText.textAlignment = View.TEXT_ALIGNMENT_CENTER
        titleText.gravity = Gravity.CENTER
        titleText.text = title
        titleText.setTextColor(Color.BLACK)
        cockpit_linear_layout_holder.addView(titleText)
    }

    private fun generatePermissionExecutable(permission: PermissionHelper.Companion.PermissionGroups,   button: SwipeButton): SwipeButton.SwipeButtonExecution {
        return object : SwipeButton.SwipeButtonExecution{
            override fun execRight() {
                if (!PermissionHelper.check(this@CockpitActivity,permission)){
                    PermissionHelper.request(this@CockpitActivity,permission)
                    activeButton = button
                }else{
                    Toast.makeText(applicationContext,"Permission already granted!", Toast.LENGTH_SHORT).show()
                    Handler().postDelayed({button.collapse()}, 500)
                }
            }

            override fun execLeft() {
                cockpit_text_info.text = permission.getDescription(applicationContext)
            }

            override fun execReset() {
                cockpit_text_info.text = mode.getDescription(applicationContext)
            }
        }
    }
    private fun generateCommunicationExecutable(communication: CommunicationHelper.Companion.Communications, button: SwipeButton): SwipeButton.SwipeButtonExecution {
        return object : SwipeButton.SwipeButtonExecution{
            override fun execRight() {
                if (!CommunicationHelper.check(applicationContext,communication)) {
                    val active = CommunicationHelper.toggle(applicationContext, communication)
                    button.setIndividualButtonColors(if (active) Color.WHITE else Color.RED, if (active) Color.GREEN else Color.WHITE, Color.GRAY, Color.GRAY).updateViews(true)
                    Handler().postDelayed({button.collapse()}, 500)
                }else{
                    Toast.makeText(applicationContext,communication.label+" already enabled!", Toast.LENGTH_SHORT).show()
                    Handler().postDelayed({button.collapse()}, 500)
                }
            }

            override fun execLeft() {
                if (CommunicationHelper.check(applicationContext,communication)) {
                    val active = CommunicationHelper.toggle(applicationContext, communication)
                    button.setIndividualButtonColors(if (active) Color.WHITE else Color.RED, if (active) Color.GREEN else Color.WHITE, Color.GRAY, Color.GRAY).updateViews(true)
                    Handler().postDelayed({button.collapse()}, 500)
                }else{
                    Toast.makeText(applicationContext,communication.label+" already disabled!", Toast.LENGTH_SHORT).show()
                    Handler().postDelayed({button.collapse()}, 500)
                }
            }

            override fun execReset() {
                cockpit_text_info.text = mode.getDescription(applicationContext)
            }
        }
    }
    private fun generateSensorExecutable(sensor: Sensor, button: SwipeButton): SwipeButton.SwipeButtonExecution {
        return object : SwipeButton.SwipeButtonExecution{
            override fun execRight() {
                SensorHelper.getInstance(applicationContext).registerTextGraphListener(sensor,cockpit_text_info)
                Handler().postDelayed({button.collapse()}, 500)
            }

            override fun execLeft() {
                SensorHelper.getInstance(applicationContext).unregisterTextGraphListener()
                Handler().postDelayed({button.collapse()}, 500)
            }

            override fun execReset() {
                cockpit_text_info.text = mode.getDescription(applicationContext)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_ALL && permissions.isNotEmpty()) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                activeButton?.setIndividualButtonColors(Color.GREEN, Color.WHITE, Color.GRAY, Color.GRAY)
                        ?.setButtonIcons(R.string.icon_check, R.string.icon_sign, null, null)
                        ?.updateViews(true)
                Toast.makeText(applicationContext,"Permission granted!", Toast.LENGTH_SHORT).show()
            }else{
                activeButton?.setIndividualButtonColors(Color.RED, Color.WHITE, Color.GRAY, Color.GRAY)
                        ?.setButtonIcons(R.string.icon_cross, R.string.icon_sign, null, null)
                        ?.updateViews(true)
                Toast.makeText(applicationContext,"Permission denied!", Toast.LENGTH_SHORT).show()
            }
            activeButton?.collapse()
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        createViews()
    }

    override fun onResume() {
        super.onResume()
        createViews()
    }

    override fun onLayoutRendered() {
        addSpacerLayout(cockpit_layout_info)
    }

    private fun addSpacerLayout(spacer: ViewGroup) {
        val layout = LinearLayout(this)
        val params = LinearLayout.LayoutParams(spacer.width, (spacer.height*0.5f).toInt())
        layout.layoutParams = params
        cockpit_linear_layout_holder.addView(layout)
    }





























    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_cockpit, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId


        if (item.itemId == R.id.cockpit_menu_next) {
            mode = mode.next()
            cockpit_text_info.text = mode.getDescription(applicationContext)
            createViews()
            return true
        }

        return super.onOptionsItemSelected(item)
    }
}