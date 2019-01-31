package uzh.scenere.activities

import android.os.Bundle
import uzh.scenere.R

class MainMenuActivity : AbstractBaseActivity() {
    override fun getConfiguredLayout(): Int {
        return R.layout.activity_main_menu
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onBackPressed() {
        this.finishAffinity();
    }
}