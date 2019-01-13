package uzh.scenere.activities

import android.os.Bundle
import uzh.scenere.R

class ScenariosActivity : AbstractManagementActivity() {
  override fun getConfiguredLayout(): Int {
    return R.layout.activity_scenarios
  }
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
  }
}