package uzh.scenere.const

class Constants {
    companion object {
        const val SHARED_PREFERENCES: String = "scenereSharedPreferences"
        //NUMBERS
        const val MILLION: Double = 1000000.0
        const val THOUSAND: Double = 1000.0
        const val HUNDRED: Double = 100.0
        //PERMISSIONS
        const val PERMISSION_REQUEST_ALL: Int = 888
        const val PERMISSION_REQUEST_GPS: Int = 666
        //TAGS
        const val GENERAL_TAG: String = "SRE-TAG"
        //BUNDLE
        const val BUNDLE_PROJECT: String = "sreBundleProject"
        const val BUNDLE_SCENARIO: String = "sreBundleScenario"
        //UID-IDENTIFIERS
        const val PROJECT_UID_IDENTIFIER: String = "project_"
        const val OBJECT_UID_IDENTIFIER: String = "object_"
        const val STAKEHOLDER_UID_IDENTIFIER: String = "stakeholder_"
    }
}