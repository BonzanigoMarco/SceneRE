package uzh.scenere.activities

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_startup.*
import uzh.scenere.R
import uzh.scenere.views.SwipeButton

class StartupActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_startup)
        addExecutable(swipe_btn)

        // Example of a call to a native method
        sample_text.text = stringFromJNI()
    }

    private fun addExecutable(swipeButton: SwipeButton?) {
        swipeButton?.setExecution(object : SwipeButton.SwipeButtonExecution{
            override fun execLeft() {
                Toast.makeText(swipe_btn.context,"Left",Toast.LENGTH_SHORT).show()
            }
            override fun execRight() {
                Toast.makeText(swipe_btn.context,"Right",Toast.LENGTH_SHORT).show()
            }
            override fun execUp() {
                Toast.makeText(swipe_btn.context,"Up",Toast.LENGTH_SHORT).show()
            }
            override fun execDown() {
                Toast.makeText(swipe_btn.context,"Down",Toast.LENGTH_SHORT).show()
            }
            override fun execReset() {
//                val intent = Intent(swipe_btn.context, MainActivity::class.java)
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
