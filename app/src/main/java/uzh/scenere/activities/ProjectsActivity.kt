package uzh.scenere.activities

import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_projects.*
import uzh.scenere.R
import uzh.scenere.sensors.SensorHelper
import uzh.scenere.views.SwipeButton

class ProjectsActivity : AbstractManagementActivity() {
    override fun getConfiguredLayout(): Int {
        return R.layout.activity_projects
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        project_management_linear_layout_holder.addView(
                SwipeButton(this,"Create New Project")
                        .setButtonMode(SwipeButton.SwipeButtonMode.DOUBLE)
                        .setColors(Color.WHITE,Color.GRAY)
                        .setButtonStates(false,true,false,false)
                        .setExecutable(generateExecutable())
                        .updateViews(false)
        )
        for (i in 1..5) {
            project_management_linear_layout_holder.addView(
                    SwipeButton(this,"Ye olde Project")
                            .setColors(Color.WHITE,Color.GRAY)
                            .setButtonStates(false,true,true,true)
                            .setExecutable(generateExecutable())
                            .updateViews(false)
            )
        }
    }

    override fun onLayoutRendered() {
        addSpacerLayout(project_management_layout_scenario_management)
    }

    private fun addSpacerLayout(spacer: ViewGroup) {
        val layout = LinearLayout(this)
        val params = LinearLayout.LayoutParams(spacer.width, (spacer.height*1.5f).toInt())
        layout.layoutParams = params
        project_management_linear_layout_holder.addView(layout)
    }

    private fun generateExecutable(): SwipeButton.SwipeButtonExecution {
        return object : SwipeButton.SwipeButtonExecution{
            override fun execLeft() {
                Toast.makeText(applicationContext,"Left", Toast.LENGTH_SHORT).show()
            }
            override fun execRight() {
                Toast.makeText(applicationContext,"Right", Toast.LENGTH_SHORT).show()
            }
            override fun execUp() {
                Toast.makeText(applicationContext,"Up", Toast.LENGTH_SHORT).show()
            }
            override fun execDown() {
                Toast.makeText(applicationContext,SensorHelper.getInstance(applicationContext).createSensorArray(), Toast.LENGTH_SHORT).show()
            }
            override fun execReset() {
            }

        }
    }
}