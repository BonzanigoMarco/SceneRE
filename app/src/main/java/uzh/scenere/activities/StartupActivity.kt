package uzh.scenere.activities

import android.os.Bundle
import uzh.scenere.R

class StartupActivity : AbstractBaseActivity() {
  override fun getConfiguredLayout(): Int {
    return R.layout.activity_startup
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
  }
}