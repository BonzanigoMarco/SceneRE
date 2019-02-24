package uzh.scenere.activities

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.widget.RelativeLayout
import kotlinx.android.synthetic.main.activity_main_menu.*
import uzh.scenere.R
import uzh.scenere.helpers.PermissionHelper
import uzh.scenere.views.SreButton
import uzh.scenere.views.SreTutorialLayoutDialog

class MainMenuActivity : AbstractBaseActivity() {
    override fun getConfiguredLayout(): Int {
        return R.layout.activity_main_menu
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        val projectsButton = SreButton(applicationContext, main_menu_root, "Projects",RelativeLayout.LayoutParams.WRAP_CONTENT,RelativeLayout.LayoutParams.MATCH_PARENT).addRule(RelativeLayout.BELOW, R.id.main_menu_text_label).addRule(RelativeLayout.CENTER_VERTICAL,RelativeLayout.TRUE).addExecutable { startActivity(Intent(this, ProjectsActivity::class.java)) }
        val walkthroughButton = SreButton(applicationContext, main_menu_root, "Walkthrough",RelativeLayout.LayoutParams.WRAP_CONTENT,RelativeLayout.LayoutParams.MATCH_PARENT).addRule(RelativeLayout.BELOW, projectsButton.id).addRule(RelativeLayout.CENTER_VERTICAL,RelativeLayout.TRUE).addExecutable { startActivity(Intent(this, WalkthroughActivity::class.java)) }
        val analyticsButton = SreButton(applicationContext, main_menu_root, "Analytics",RelativeLayout.LayoutParams.WRAP_CONTENT,RelativeLayout.LayoutParams.MATCH_PARENT).addRule(RelativeLayout.BELOW, walkthroughButton.id).addRule(RelativeLayout.CENTER_VERTICAL,RelativeLayout.TRUE).addExecutable { startActivity(Intent(this, AnalyticsActivity::class.java)) }
        val shareButton = SreButton(applicationContext, main_menu_root, "Share",RelativeLayout.LayoutParams.WRAP_CONTENT,RelativeLayout.LayoutParams.MATCH_PARENT).addRule(RelativeLayout.BELOW, analyticsButton.id).addRule(RelativeLayout.CENTER_VERTICAL,RelativeLayout.TRUE).addExecutable { startActivity(Intent(this, ShareActivity::class.java)) }
        val cockpitButton = SreButton(applicationContext, main_menu_root, "Cockpit",RelativeLayout.LayoutParams.WRAP_CONTENT,RelativeLayout.LayoutParams.MATCH_PARENT).addRule(RelativeLayout.BELOW, shareButton.id).addRule(RelativeLayout.CENTER_VERTICAL,RelativeLayout.TRUE).addExecutable { startActivity(Intent(this, CockpitActivity::class.java)) }

        shareButton.isEnabled = false


        main_menu_root.addView(projectsButton)
        main_menu_root.addView(walkthroughButton)
        main_menu_root.addView(analyticsButton)
        main_menu_root.addView(shareButton)
        main_menu_root.addView(cockpitButton)
        if (!PermissionHelper.check(applicationContext,PermissionHelper.Companion.PermissionGroups.STORAGE)){
            projectsButton.isEnabled = false
            walkthroughButton.isEnabled = false
            analyticsButton.isEnabled = false
            shareButton.isEnabled = false
            tutorialOpen = SreTutorialLayoutDialog(this,screenWidth,"info_cockpit").addEndExecutable { tutorialOpen = false }.show(tutorialOpen)
        }
    }

    override fun onBackPressed() {
        this.finishAffinity();
    }
}