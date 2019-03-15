package uzh.scenere.activities

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RelativeLayout
import kotlinx.android.synthetic.main.activity_main_menu.*
import uzh.scenere.R
import uzh.scenere.const.Constants.Companion.NOTHING
import uzh.scenere.helpers.DipHelper
import uzh.scenere.helpers.PermissionHelper
import uzh.scenere.helpers.StringHelper
import uzh.scenere.helpers.reStyleText
import uzh.scenere.views.SreButton
import uzh.scenere.views.SreTutorialLayoutDialog

class MainMenuActivity : AbstractBaseActivity() {

    override fun getConfiguredRootLayout(): ViewGroup? {
        return main_menu_root
    }
    override fun getConfiguredLayout(): Int {
        return R.layout.activity_main_menu
    }

    var projectsButton:SreButton? = null
    var walkthroughButton:SreButton? = null
    var analyticsButton:SreButton? = null
    var shareButton:SreButton? = null
    var cockpitButton:SreButton? = null
    var glossaryButton:SreButton? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        projectsButton = SreButton(applicationContext, main_menu_layout_button_holder, NOTHING,RelativeLayout.LayoutParams.WRAP_CONTENT,RelativeLayout.LayoutParams.MATCH_PARENT).setExecutable { startActivity(Intent(this, ProjectsActivity::class.java)) }
        walkthroughButton = SreButton(applicationContext, main_menu_layout_button_holder, NOTHING,RelativeLayout.LayoutParams.WRAP_CONTENT,RelativeLayout.LayoutParams.MATCH_PARENT).setExecutable { startActivity(Intent(this, WalkthroughActivity::class.java)) }
        analyticsButton = SreButton(applicationContext, main_menu_layout_button_holder, NOTHING,RelativeLayout.LayoutParams.WRAP_CONTENT,RelativeLayout.LayoutParams.MATCH_PARENT).setExecutable { startActivity(Intent(this, AnalyticsActivity::class.java)) }
        shareButton = SreButton(applicationContext, main_menu_layout_button_holder, NOTHING,RelativeLayout.LayoutParams.WRAP_CONTENT,RelativeLayout.LayoutParams.MATCH_PARENT).setExecutable { startActivity(Intent(this, ShareActivity::class.java)) }
        cockpitButton = SreButton(applicationContext, main_menu_layout_button_holder, NOTHING,RelativeLayout.LayoutParams.WRAP_CONTENT,RelativeLayout.LayoutParams.MATCH_PARENT).setExecutable { startActivity(Intent(this, CockpitActivity::class.java)) }
        glossaryButton = SreButton(applicationContext, main_menu_layout_button_holder, NOTHING,RelativeLayout.LayoutParams.WRAP_CONTENT,RelativeLayout.LayoutParams.MATCH_PARENT).setExecutable { startActivity(Intent(this, GlossaryActivity::class.java)) }

        prepareButton(
                arrayOf(R.string.projects_icon_label,
                R.string.walkthroughs_icon_label,
                R.string.analytics_icon_label,
                R.string.share_icon_label,
                R.string.glossary_icon_label,
                R.string.cockpit_icon_label),
                arrayOf(projectsButton!!,
                        walkthroughButton!!,
                        analyticsButton!!,
                        shareButton!!,
                        glossaryButton!!,
                        cockpitButton!!
                        ))
    }

    override fun onResume() {
        super.onResume()
        setButtonStates(arrayOf(projectsButton!!,
                walkthroughButton!!,
                analyticsButton!!,
                shareButton!!,
                glossaryButton!!
        ))
    }

    private fun prepareButton(textIds: Array<Int>, buttons: Array<SreButton>){
        if (textIds.size != buttons.size){
            return
        }
        for (b in 0 until textIds.size) {
            buttons[b].textSize = DipHelper.get(resources).dip6.toFloat()
            buttons[b].setWeight(1f, false, LinearLayout.LayoutParams.MATCH_PARENT)
            buttons[b].setMargin(DipHelper.get(resources).dip6)
            buttons[b].text = StringHelper.styleString(textIds[b],applicationContext)
            main_menu_layout_button_holder.addView(buttons[b])
        }
    }

    fun setButtonStates(buttons: Array<SreButton>){
        if (!PermissionHelper.check(applicationContext,PermissionHelper.Companion.PermissionGroups.STORAGE)){
            for (button in buttons){
                button.isEnabled = false
            }
            tutorialOpen = SreTutorialLayoutDialog(this,screenWidth,"info_cockpit").addEndExecutable { tutorialOpen = false }.show(tutorialOpen)
        }else{
            for (button in buttons){
                button.isEnabled = true
            }
        }
    }
    
    override fun onBackPressed() {
        this.finishAffinity();
    }
}