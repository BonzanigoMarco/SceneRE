package uzh.scenere.activities

import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_experimental.*
import uzh.scenere.R
import uzh.scenere.views.SwipeButton


class ExperimentalActivity : AbstractBaseActivity() {
  override fun getConfiguredLayout(): Int {
    return R.layout.activity_experimental
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_experimental)
    root.setBackgroundColor( ContextCompat.getColor(this, R.color.srePrimaryDisabledDark))
    swipe_button_holder.addView(SwipeButton(this,"Project LOCUST"))
    swipe_button_holder.addView(SwipeButton(this,"Project SIPHON"))
    swipe_button_holder.addView(SwipeButton(this,"Project BARRIER"))
    swipe_button_holder.addView(SwipeButton(this,"Project SPOTLIGHT"))
    swipe_button_holder.addView(SwipeButton(this,"Project PULSAR"))
    // Example of a call to a native method
    sample_text.text = stringFromJNI()
  }

  private fun addExecutable(swipeButton: SwipeButton?) {
    swipeButton?.setExecutable(object : SwipeButton.SwipeButtonExecution{
      override fun execLeft() {
        Toast.makeText(swipeButton.context,"Left",Toast.LENGTH_SHORT).show()
      }
      override fun execRight() {
        Toast.makeText(swipeButton.context,"Right",Toast.LENGTH_SHORT).show()
      }
      override fun execUp() {
        Toast.makeText(swipeButton.context,"Up",Toast.LENGTH_SHORT).show()
      }
      override fun execDown() {
        Toast.makeText(swipeButton.context,"Down",Toast.LENGTH_SHORT).show()
      }
      override fun execReset() {
//                val intent = Intent(swipe_btn1.context, MainMenuActivity::class.java)
//                startActivity(intent)
      }

    })
  }

  /**
   * A native method that is implemented by the 'native-lib' native library,
   * which is packaged with this application.
   */
  external fun stringFromJNI(): String

  companion object {

    // Used to load the 'native-lib' library on application startup.
    init {
      System.loadLibrary("native-lib")
    }
  }
}