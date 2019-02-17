package uzh.scenere.activities

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.v4.content.ContextCompat
import android.view.View
import android.view.View.*
import android.view.animation.AlphaAnimation
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_startup.*
import uzh.scenere.R
import uzh.scenere.const.Constants
import uzh.scenere.helpers.DatabaseHelper
import uzh.scenere.helpers.StringHelper
import uzh.scenere.helpers.nextSafeInt
import uzh.scenere.views.WeightAnimator
import kotlin.random.Random

class StartupActivity : AbstractBaseActivity() {
    override fun getConfiguredLayout(): Int {
        return R.layout.activity_startup
    }

    private var complete = 0
    private var total = 0
    private var userName: String? = null
    private var interrupted: Boolean = false
    private var closing: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        morphRandom(startup_text_1 as TextView,'S')
        morphRandom(startup_text_2 as TextView,'c')
        morphRandom(startup_text_3 as TextView,'e')
        morphRandom(startup_text_4 as TextView,'n')
        morphRandom(startup_text_5 as TextView,'e')
        morphRandom(startup_text_6 as TextView,'-')
        morphRandom(startup_text_7 as TextView,'R')
        morphRandom(startup_text_8 as TextView,'E')

        userName = DatabaseHelper.getInstance(applicationContext).readAndMigrate(Constants.USER_NAME, String::class, "")

        //TODO, load and cleanup in the background
    }

    private fun morphRandom(text: TextView, returnChar: Char, init: Boolean = true, offset: Int = 0) {
        total += if (init) 1 else 0
        var character: Char = '#'
        if (StringHelper.hasText(text.text)){
            character = text.text[0]
        }
        val dist = Math.abs(returnChar.toInt()-character.toInt())*5
        text.setTextColor(Color.rgb(dist,dist,dist))
        if (character == returnChar){
            return complete()
        }
        var low = if ((returnChar.toInt() - (50-offset)) < 32) 32 else (returnChar.toInt() - (50-offset))
        low = if (low > returnChar.toInt()) returnChar.toInt() else low
        text.text = (low + Random.nextSafeInt(2*(returnChar.toInt()-low))).toChar().toString()
        val offsetNew = if (offset < 50 && Math.random()>0.5) (offset+1) else offset
        Handler().postDelayed({morphRandom(text,returnChar, false, offsetNew)},(50L + Random.nextSafeInt((50-offsetNew))))
    }

    private fun complete() {
        complete++
        WeightAnimator(startup_layout_progress_bar, complete.toFloat(), 250).play()
        if (complete == total){
            logoDisplayFinished()
        }
    }

    fun onStartupInterrupt(v: View){
        interrupted = true
        startup_edit_name.isEnabled = true
        startup_button_continue.visibility = VISIBLE
        startup_text_name.text = "Do you want to change your Name?"
        fadeIn()
    }

    private fun logoDisplayFinished() {
        val primary = ContextCompat.getColor(this, R.color.srePrimaryDark)
        val secondary = ContextCompat.getColor(this, R.color.sreSecondaryDark)
        (startup_text_1 as TextView).setTextColor(primary)
        (startup_text_2 as TextView).setTextColor(primary)
        (startup_text_3 as TextView).setTextColor(primary)
        (startup_text_4 as TextView).setTextColor(primary)
        (startup_text_5 as TextView).setTextColor(primary)
        (startup_text_7 as TextView).setTextColor(secondary)
        (startup_text_8 as TextView).setTextColor(secondary)
        fadeIn()
        if (StringHelper.hasText(userName)){
            Handler().postDelayed({
                fadeOut()
            },1000)
            startup_text_name.text = "Welcome Back"
            startup_edit_name.setText(userName)
            startup_edit_name.isEnabled = false
            Handler().postDelayed({checkInputAndNext()},2000)
        }else{
            startup_button_continue.visibility = VISIBLE
        }
    }

    private fun fadeIn() {
        startup_edit_name.visibility = VISIBLE
        startup_text_name.visibility = VISIBLE
        val fadeIn = AlphaAnimation(0.0f, 1.0f)
        fadeIn.duration = 1000;
        startup_text_name.startAnimation(fadeIn)
        startup_edit_name.startAnimation(fadeIn)
    }

    private fun fadeOut() {
        val fadeOut = AlphaAnimation(1.0f, 0.0f)
        fadeOut.duration = 1000;
        startup_text_name.startAnimation(fadeOut)
        startup_edit_name.startAnimation(fadeOut)
        startup_edit_name.visibility = INVISIBLE
        startup_text_name.visibility = INVISIBLE
        if (startup_button_continue.visibility == VISIBLE){
            startup_button_continue.startAnimation(fadeOut)
            startup_button_continue.visibility = INVISIBLE
        }
    }

    override fun onNavigationButtonClicked(view: View) {
        when (view.id) {
            R.id.startup_button_continue -> {
                checkInputAndNext()
            }
        }
    }

    private fun checkInputAndNext() {
        if (interrupted || closing){
            interrupted = false
            return
        }
        if (!StringHelper.hasText(startup_edit_name.text)) {
            notify("Please enter a Name")
            return
        }
        closing = true
        if (startup_edit_name.isEnabled){
            startup_edit_name.isEnabled = false
            DatabaseHelper.getInstance(applicationContext).write(Constants.USER_NAME, startup_edit_name.text.toString())
            fadeOut()
            Handler().postDelayed({startActivity(Intent(this, MainMenuActivity::class.java))},1000)
        }else{
            startActivity(Intent(this, MainMenuActivity::class.java))
        }
    }
}