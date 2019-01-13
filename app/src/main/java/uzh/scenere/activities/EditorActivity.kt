package uzh.scenere.activities

import android.os.Bundle
import uzh.scenere.R

class EditorActivity : AbstractManagementActivity() {
  override fun getConfiguredLayout(): Int {
    return R.layout.activity_editor
  }
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
  }
}