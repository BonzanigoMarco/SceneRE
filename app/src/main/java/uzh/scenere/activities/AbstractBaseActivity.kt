package uzh.scenere.activities

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import kotlinx.android.synthetic.main.scroll_holder.*
import kotlinx.android.synthetic.main.sre_toolbar.*
import uzh.scenere.R
import uzh.scenere.helpers.NumberHelper
import uzh.scenere.helpers.StringHelper
import uzh.scenere.views.WeightAnimator


abstract class AbstractBaseActivity : AppCompatActivity() {
    protected var marginSmall: Int? = null
    protected var textSize: Float? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(getConfiguredLayout())
        readVariables()
    }

    private fun readVariables() {
        marginSmall = NumberHelper.nvl(applicationContext.resources.getDimension(R.dimen.dimMarginSmall), 0).toInt()
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4f, applicationContext.resources.displayMetrics)
    }

    abstract fun getConfiguredLayout(): Int

    open fun onNavigationButtonClicked(view: View) {
        when (view.id) {
            R.id.startup_button_continue -> startActivity(Intent(this, MainMenuActivity::class.java))
            R.id.main_menu_button_analytics -> startActivity(Intent(this, AnalyticsActivity::class.java))
            R.id.main_menu_button_project_management -> startActivity(Intent(this, ProjectsActivity::class.java))
            R.id.main_menu_button_walkthrough -> startActivity(Intent(this, WalkthroughActivity::class.java))
            R.id.main_menu_button_share -> startActivity(Intent(this, ShareActivity::class.java))
            R.id.main_menu_button_cockpit -> startActivity(Intent(this, CockpitActivity::class.java))
            R.id.projects_button_scenario_management -> startActivity(Intent(this, ScenariosActivity::class.java))
            R.id.scenario_management_button_resource_management -> startActivity(Intent(this, ResourcesActivity::class.java))
            R.id.scenario_management_button_scenario_editors -> startActivity(Intent(this, EditorActivity::class.java))
        }
    }

    open fun onToolbarClicked(view: View) {
        when (view.id) {
            R.id.toolbar_action_left -> onToolbarLeftClicked()
            R.id.toolbar_action_center_left -> onToolbarMiddleLeftClicked()
            R.id.toolbar_action_center -> onToolbarMiddleClicked()
            R.id.toolbar_action_center_right -> onToolbarMiddleRightClicked()
            R.id.toolbar_action_right -> onToolbarRightClicked()
        }
    }

    open fun onButtonClicked(view: View) {
        //NOP
    }

    open fun onToolbarLeftClicked() {
        //NOP
    }

    open fun onToolbarMiddleLeftClicked() {
        //NOP
    }

    open fun onToolbarMiddleClicked() {
        //NOP
    }

    open fun onToolbarMiddleRightClicked() {
        //NOP
    }

    open fun onToolbarRightClicked() {
        //NOP
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        onLayoutRendered()
    }

    open fun onLayoutRendered() {
        if (infoState == null) {
            execMorphInfoBar(InfoState.INITIALIZE)
        }
    }

    enum class InfoState {
        MINIMIZED, NORMAL, MAXIMIZED, INITIALIZE
    }

    private var infoState: InfoState? = null

    protected fun execMorphInfoBar(state: InfoState? = null): CharSequence {
        if (state != null){
            infoState = state
        }else{
            when (infoState) {
                InfoState.MINIMIZED -> infoState = InfoState.NORMAL
                InfoState.NORMAL -> infoState = InfoState.MAXIMIZED
                InfoState.MAXIMIZED -> infoState = InfoState.MINIMIZED
            }
        }
        return execMorphInfoBarInternal()
    }

    private fun execMorphInfoBarInternal(): CharSequence {
        when (infoState) {
            InfoState.INITIALIZE -> {
                holder_scroll.layoutParams = createLayoutParams(1f)
                holder_layout_info.layoutParams = createLayoutParams(9f)
                createLayoutParams(0f, holder_text_info_title)
                holder_text_info_content_wrap.layoutParams = createLayoutParams(1f)
                infoState = InfoState.MINIMIZED
                return resources.getText(R.string.icon_win_min)
            }
            InfoState.MINIMIZED -> {
                WeightAnimator(holder_layout_info, 9f, 250).play()
                WeightAnimator(holder_scroll, 1f, 250).play()
                createLayoutParams(0f, holder_text_info_title)
                holder_text_info_content_wrap.layoutParams = createLayoutParams(1f)
                return resources.getText(R.string.icon_win_min)
            }
            InfoState.NORMAL -> {
                WeightAnimator(holder_scroll,3f,250).play()
                WeightAnimator(holder_layout_info,7f,250).play()
                createLayoutParams(2f, holder_text_info_title, 1)
                holder_text_info_content_wrap.layoutParams = createLayoutParams(1f)
                holder_text_info_content.maxLines = 2
                return resources.getText(R.string.icon_win_norm)
            }
            InfoState.MAXIMIZED -> {
                WeightAnimator(holder_scroll,10f,250).play()
                WeightAnimator(holder_layout_info,0f,250).play()
                createLayoutParams(2.7f, holder_text_info_title, 1)
                holder_text_info_content_wrap.layoutParams = createLayoutParams(0.3f)
                holder_text_info_content.maxLines = 10
                return resources.getText(R.string.icon_win_max)
            }
        }
        return resources.getText(R.string.icon_null)
    }

    private fun createLayoutParams(weight: Float, textView: TextView? = null, crop: Int = 0): LinearLayout.LayoutParams {
        val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT,
                weight
        )
        if (textView != null) {
            val margin = NumberHelper.nvl(this.resources?.getDimension(R.dimen.dimMarginSmall), 0).toInt()
            textView.setPadding(0, 0, 0, 0)
            layoutParams.setMargins(margin, margin, margin, margin)
            when (crop) {
                0 -> {
                    textView.setPadding(0, -margin / 2, 0, 0)
                }
                1 -> layoutParams.setMargins(margin, margin, margin, margin / 2)
                2 -> layoutParams.setMargins(margin, margin / 2, margin, margin)
            }
            textView.gravity = (Gravity.CENTER or Gravity.TOP)
            textView.textAlignment = View.TEXT_ALIGNMENT_CENTER
            textView.layoutParams = layoutParams
        }
        return layoutParams
    }

    fun toast(string: String) {
        Toast.makeText(this, string, Toast.LENGTH_SHORT).show()
    }

    protected fun createTitle(title: String, holder: ViewGroup) {
        val titleText = TextView(this)
        val titleParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        titleText.layoutParams = titleParams
        titleText.textAlignment = View.TEXT_ALIGNMENT_CENTER
        titleText.gravity = Gravity.CENTER
        titleText.text = title
        titleText.setTextColor(Color.BLACK)
        holder.addView(titleText)
    }

    protected fun customizeToolbar(l: Int?, cl: Int?, c: Int?, cr: Int?, r: Int?) {
        toolbar_action_left.text = StringHelper.lookupOrEmpty(l, applicationContext)
        toolbar_action_center_left.text = StringHelper.lookupOrEmpty(cl, applicationContext)
        toolbar_action_center.text = StringHelper.lookupOrEmpty(c, applicationContext)
        toolbar_action_center_right.text = StringHelper.lookupOrEmpty(cr, applicationContext)
        toolbar_action_right.text = StringHelper.lookupOrEmpty(r, applicationContext)
    }
}
