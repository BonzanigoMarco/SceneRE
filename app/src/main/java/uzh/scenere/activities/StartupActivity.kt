package uzh.scenere.activities

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import uzh.scenere.R

class StartupActivity : AbstractBaseActivity() {
    override fun getConfiguredLayout(): Int {
        return R.layout.activity_startup
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }
}
