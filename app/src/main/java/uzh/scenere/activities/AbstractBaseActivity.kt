package uzh.scenere.activities

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.SpannedString
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
import android.app.Activity
import android.content.res.Configuration
import android.view.inputmethod.InputMethodManager


abstract class AbstractBaseActivity : AppCompatActivity() {
    protected var marginSmall: Int? = null
    protected var textSize: Float? = null
    protected var fontAwesome: Typeface? = null
    protected var fontNormal: Typeface = Typeface.DEFAULT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(getConfiguredLayout())
        readVariables()
    }

    private fun readVariables() {
        marginSmall = NumberHelper.nvl(applicationContext.resources.getDimension(R.dimen.dimMarginSmall), 0).toInt()
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4f, applicationContext.resources.displayMetrics)
        fontAwesome = Typeface.createFromAsset(applicationContext.assets, "FontAwesome900.otf")
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
        }
    }

    open fun onToolbarClicked(view: View) {
        when (view.id) {
            R.id.toolbar_action_left -> onToolbarLeftClicked()
            R.id.toolbar_action_center_left -> onToolbarCenterLeftClicked()
            R.id.toolbar_action_center -> onToolbarCenterClicked()
            R.id.toolbar_action_center_right -> onToolbarCenterRightClicked()
            R.id.toolbar_action_right -> onToolbarRightClicked()
        }
    }

    open fun onButtonClicked(view: View) {
        //NOP
    }

    open fun onToolbarLeftClicked() {
        //NOP
    }

    open fun onToolbarCenterLeftClicked() {
        //NOP
    }

    open fun onToolbarCenterClicked() {
        //NOP
    }

    open fun onToolbarCenterRightClicked() {
        //NOP
    }

    open fun onToolbarRightClicked() {
        //NOP
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        onLayoutRendered()
    }

    open fun onLayoutRendered(){
        //NOP
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        execAdaptToOrientationChange()
    }

    //************
    //* CREATION *
    //************
    protected fun createLayoutParams(weight: Float, textView: TextView? = null, crop: Int = 0): LinearLayout.LayoutParams {
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
            textView.gravity = Gravity.CENTER//TODO: Remove if not necesssary anymore (Gravity.CENTER or Gravity.TOP)
            textView.textAlignment = View.TEXT_ALIGNMENT_CENTER
            textView.layoutParams = layoutParams
        }
        return layoutParams
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

    fun toast(string: String) {
        Toast.makeText(this, string, Toast.LENGTH_SHORT).show()
    }

    protected fun execMinimizeKeyboard(){
        val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        val focusView = currentFocus ?: View(this)
        inputMethodManager.hideSoftInputFromWindow(focusView.windowToken, 0)
        focusView.clearFocus()
    }

    open fun execAdaptToOrientationChange() {
        //NOP
    }

    //*******
    //* GUI *
    //*******
    protected fun customizeToolbarId(l: Int?, cl: Int?, c: Int?, cr: Int?, r: Int?) {
        toolbar_action_left.text = StringHelper.lookupOrEmpty(l, applicationContext)
        toolbar_action_center_left.text = StringHelper.lookupOrEmpty(cl, applicationContext)
        toolbar_action_center.text = StringHelper.lookupOrEmpty(c, applicationContext)
        toolbar_action_center_right.text = StringHelper.lookupOrEmpty(cr, applicationContext)
        toolbar_action_right.text = StringHelper.lookupOrEmpty(r, applicationContext)
    }

    protected fun adaptToolbarId(l: Int?, cl: Int?, c: Int?, cr: Int?, r: Int?) {
        toolbar_action_left.text = if (l != null) StringHelper.lookupOrEmpty(l, applicationContext) else toolbar_action_left.text
        toolbar_action_center_left.text = if (cl != null)  StringHelper.lookupOrEmpty(cl, applicationContext) else toolbar_action_center_left.text
        toolbar_action_center.text = if (c != null)  StringHelper.lookupOrEmpty(c, applicationContext) else toolbar_action_center.text
        toolbar_action_center_right.text = if (cr != null)  StringHelper.lookupOrEmpty(cr, applicationContext) else toolbar_action_center_right.text
        toolbar_action_right.text = if (r != null)  StringHelper.lookupOrEmpty(r, applicationContext) else toolbar_action_right.text
    }

    protected fun customizeToolbarText(l: String?, cl: String?, c: String?, cr: String?, r: String?) {
        toolbar_action_left.text = l
        toolbar_action_center_left.text = cl
        toolbar_action_center.text = c
        toolbar_action_center_right.text = cr
        toolbar_action_right.text = r
    }

    protected fun adaptToolbarText(l: String?, cl: String?, c: String?, cr: String?, r: String?) {
        toolbar_action_left.text = l ?: toolbar_action_left.text
        toolbar_action_center_left.text = cl ?: toolbar_action_center_left.text
        toolbar_action_center.text = c ?: toolbar_action_center.text
        toolbar_action_center_right.text = cr ?: toolbar_action_center_right.text
        toolbar_action_right.text = r ?: toolbar_action_right.text
    }

    protected fun getSpannedStringFromId(id: Int): SpannedString{
        return getText(id) as SpannedString
    }
}
