package uzh.scenere.activities

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import uzh.scenere.R

abstract class AbstractBaseActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(getConfiguredLayout())
    }

    abstract fun getConfiguredLayout() : Int

    open fun onNavigationButtonClicked(view: View){
        when(view.id){
            R.id.startup_button_continue->startActivity(Intent(this,MainMenuActivity::class.java))
            R.id.main_menu_button_analytics->startActivity(Intent(this,AnalyticsActivity::class.java))
            R.id.main_menu_button_project_management->startActivity(Intent(this,ProjectsActivity::class.java))
            R.id.main_menu_button_walkthrough->startActivity(Intent(this,WalkthroughActivity::class.java))
            R.id.main_menu_button_share->startActivity(Intent(this,ShareActivity::class.java))
            R.id.main_menu_button_cockpit->startActivity(Intent(this,CockpitActivity::class.java))
            R.id.project_management_button_scenario_management->startActivity(Intent(this,ScenariosActivity::class.java))
            R.id.scenario_management_button_resource_management->startActivity(Intent(this,ResourcesActivity::class.java))
            R.id.scenario_management_button_scenario_editors->startActivity(Intent(this,EditorActivity::class.java))
        }
    }

    open fun onButtonClicked(view: View){
        //NOP
    }



}
