package uzh.scenere.const

import android.util.TypedValue

class Constants {
    companion object {
        //Kotlin
        const val REFLECTION: String = " (Kotlin reflection is not available)"
        const val NOTHING: String = ""
        //Storage
        const val SHARED_PREFERENCES: String = "scenereSharedPreferences"
        const val USER_NAME: String = "scenereUserName"
        const val ANONYMOUS: String = "John Doe"
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
        const val BUNDLE_OBJECT: String = "sreBundleObject"
        //UID-IDENTIFIERS
        const val PROJECT_UID_IDENTIFIER: String = "project_"
        const val STAKEHOLDER_UID_IDENTIFIER: String = "stakeholder_"
        const val OBJECT_UID_IDENTIFIER: String = "object_"
        const val SCENARIO_UID_IDENTIFIER: String = "scenario_"
        const val ATTRIBUTE_UID_IDENTIFIER: String = "attribute_"
        const val PATH_UID_IDENTIFIER: String = "path_"
        const val ELEMENT_UID_IDENTIFIER: String = "element_"
        //GENERAL
        const val NEW_LINE: String = "\n"
        const val DOLLAR_STRING: String = "$"
        //XML
        const val EMPTY_LIST: String = "EmptyList"
        const val NULL: String = "NULL"
        const val STRING: String = "String"
        const val INT: String = "Int"
        const val LONG: String = "Long"
        const val DOUBLE: String = "Double"
        const val BOOLEAN: String = "Boolean"
        //ELEMENTS
        const val STARTING_POINT: String = "starting_point"
        //ATTRIBUTE TYPES
        const val TYPE_OBJECT: String = "Object"
        const val TYPE_STANDARD_STEP: String = "StandardStep"
        const val TYPE_BUTTON_TRIGGER: String = "ButtonTrigger"
    }
}