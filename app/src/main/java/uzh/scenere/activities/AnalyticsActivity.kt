package uzh.scenere.activities

import android.os.Bundle
import kotlinx.android.synthetic.main.scroll_holder.*
import uzh.scenere.R
import uzh.scenere.helpers.StringHelper

class AnalyticsActivity : AbstractManagementActivity() {
  override fun isInEditMode(): Boolean {
    return false
  }

  override fun isInViewMode(): Boolean {
      return true
  }

  override fun resetEditMode() {
      //NOP
  }

  override fun createEntity() {
      //NOP
  }

  override fun getConfiguredInfoString(): Int {
      return R.string.icon_explain_analytics
  }

  override fun getConfiguredLayout(): Int {
    return R.layout.activity_analytics
  }
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
      getInfoTitle().text = StringHelper.styleString(getSpannedStringFromId(getConfiguredInfoString()), fontAwesome)
  }
}