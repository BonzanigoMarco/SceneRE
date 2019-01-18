package uzh.scenere.activities

import android.os.Bundle
import uzh.scenere.R

class ResourcesActivity : AbstractBaseActivity() {
  override fun getConfiguredLayout(): Int {
    return R.layout.activity_resources
  }
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
  }
}