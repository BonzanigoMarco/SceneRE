package uzh.scenere.activities

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.widget.RelativeLayout
import kotlinx.android.synthetic.main.activity_main_menu.*
import uzh.scenere.R
import uzh.scenere.const.Constants.Companion.NOTHING
import uzh.scenere.helpers.DipHelper
import uzh.scenere.helpers.PermissionHelper
import uzh.scenere.helpers.StringHelper
import uzh.scenere.views.SreButton
import uzh.scenere.views.SreTutorialLayoutDialog

class MainMenuActivity : AbstractBaseActivity() {
    override fun getConfiguredLayout(): Int {
        return R.layout.activity_main_menu
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        val projectsButton = SreButton(applicationContext, main_menu_root, NOTHING,RelativeLayout.LayoutParams.WRAP_CONTENT,RelativeLayout.LayoutParams.MATCH_PARENT).addRule(RelativeLayout.BELOW, R.id.main_menu_text_label).addRule(RelativeLayout.CENTER_VERTICAL,RelativeLayout.TRUE).addExecutable { startActivity(Intent(this, ProjectsActivity::class.java)) }
        val walkthroughButton = SreButton(applicationContext, main_menu_root, NOTHING,RelativeLayout.LayoutParams.WRAP_CONTENT,RelativeLayout.LayoutParams.MATCH_PARENT).addRule(RelativeLayout.BELOW, projectsButton.id).addRule(RelativeLayout.CENTER_VERTICAL,RelativeLayout.TRUE).addExecutable { startActivity(Intent(this, WalkthroughActivity::class.java)) }
        val analyticsButton = SreButton(applicationContext, main_menu_root, NOTHING,RelativeLayout.LayoutParams.WRAP_CONTENT,RelativeLayout.LayoutParams.MATCH_PARENT).addRule(RelativeLayout.BELOW, walkthroughButton.id).addRule(RelativeLayout.CENTER_VERTICAL,RelativeLayout.TRUE).addExecutable { startActivity(Intent(this, AnalyticsActivity::class.java)) }
        val shareButton = SreButton(applicationContext, main_menu_root, NOTHING,RelativeLayout.LayoutParams.WRAP_CONTENT,RelativeLayout.LayoutParams.MATCH_PARENT).addRule(RelativeLayout.BELOW, analyticsButton.id).addRule(RelativeLayout.CENTER_VERTICAL,RelativeLayout.TRUE).addExecutable { startActivity(Intent(this, ShareActivity::class.java)) }
        val cockpitButton = SreButton(applicationContext, main_menu_root, NOTHING,RelativeLayout.LayoutParams.WRAP_CONTENT,RelativeLayout.LayoutParams.MATCH_PARENT).addRule(RelativeLayout.BELOW, shareButton.id).addRule(RelativeLayout.CENTER_VERTICAL,RelativeLayout.TRUE).addExecutable { startActivity(Intent(this, CockpitActivity::class.java)) }
        val glossaryButton = SreButton(applicationContext, main_menu_root, NOTHING,RelativeLayout.LayoutParams.WRAP_CONTENT,RelativeLayout.LayoutParams.MATCH_PARENT).addRule(RelativeLayout.BELOW, cockpitButton.id).addRule(RelativeLayout.CENTER_VERTICAL,RelativeLayout.TRUE).addExecutable { startActivity(Intent(this, GlossaryActivity::class.java)) }

        analyticsButton.isEnabled = true

        projectsButton.setAllCaps(false)
        walkthroughButton.setAllCaps(false)
        analyticsButton.setAllCaps(false)
        shareButton.setAllCaps(false)
        cockpitButton.setAllCaps(false)
        glossaryButton.setAllCaps(false)

        projectsButton.textSize = DipHelper.get(resources).dip6.toFloat()
        walkthroughButton.textSize = DipHelper.get(resources).dip6.toFloat()
        analyticsButton.textSize = DipHelper.get(resources).dip6.toFloat()
        shareButton.textSize = DipHelper.get(resources).dip6.toFloat()
        cockpitButton.textSize = DipHelper.get(resources).dip6.toFloat()
        glossaryButton.textSize = DipHelper.get(resources).dip6.toFloat()
        
        projectsButton.text = StringHelper.styleString(R.string.projects_icon_label,applicationContext)
        walkthroughButton.text = StringHelper.styleString(R.string.walkthroughs_icon_label,applicationContext)
        analyticsButton.text = StringHelper.styleString(R.string.analytics_icon_label,applicationContext)
        shareButton.text = StringHelper.styleString(R.string.share_icon_label,applicationContext)
        cockpitButton.text = StringHelper.styleString(R.string.cockpit_icon_label,applicationContext)
        glossaryButton.text = StringHelper.styleString(R.string.glossary_icon_label,applicationContext)
        
        main_menu_root.addView(projectsButton)
        main_menu_root.addView(walkthroughButton)
        main_menu_root.addView(analyticsButton)
        main_menu_root.addView(shareButton)
        main_menu_root.addView(cockpitButton)
        main_menu_root.addView(glossaryButton)
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