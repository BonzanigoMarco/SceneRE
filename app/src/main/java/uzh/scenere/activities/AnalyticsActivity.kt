package uzh.scenere.activities

import android.os.Bundle
import uzh.scenere.R
import uzh.scenere.datamodel.Walkthrough
import uzh.scenere.helpers.DatabaseHelper
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

    val walkthroughs =  ArrayList<Walkthrough>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getInfoTitle().text = StringHelper.styleString(getSpannedStringFromId(getConfiguredInfoString()), fontAwesome)
        walkthroughs.addAll(DatabaseHelper.getInstance(applicationContext).readBulk(Walkthrough::class, null))
        val size = walkthroughs.size
        toast("$size Walkthroughs loaded!")
    }
}