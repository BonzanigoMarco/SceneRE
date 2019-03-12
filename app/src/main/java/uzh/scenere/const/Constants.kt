package uzh.scenere.const

class Constants {
    companion object {
        const val APPLICATION_ID: String = "uzh.scenere.SceneRe"
        //Kotlin
        const val REFLECTION: String = " (Kotlin reflection is not available)"
        //Storage
        const val SHARED_PREFERENCES: String = "scenereSharedPreferences"
        const val USER_NAME: String = "scenereUserName"
        const val USER_ID: String = "scenereUserId"
        const val IMPORT_FOLDER: String = "scenereImportFolder"
        const val ANONYMOUS: String = "John Doe"
        //NUMBERS
        val COORDINATES_PATTERN: Regex = "-?[1]?[0-9]{1,2}\\.[0-9]{1,6},-?[1]?[0-9]{1,2}\\.[0-9]{1,6}".toRegex()
        const val EARTH_RADIUS_M: Double = 6371000.0
        const val MILLION_D: Double = 1000000.0
        const val MILLION: Int = 1000000
        const val FIVE_MIN_MS: Long = 300000
        const val TEN_SEC_MS: Long = 10000
        const val TWO_SEC_MS: Long = 2000
        const val THOUSAND: Double = 1000.0
        const val HUNDRED: Double = 100.0
        const val FRACTION: Float = 0.1f
        const val ZERO_F: Float = 0.0f
        const val ZERO_L: Long = 0
        const val ZERO: Int = 0
        //PERMISSIONS
        const val PERMISSION_REQUEST_ALL: Int = 888
        const val PERMISSION_REQUEST_GPS: Int = 666
        //REQUESTS
        const val IMPORT_DATA_FILE: Int = 111
        const val IMPORT_DATA_FOLDER: Int = 222
        //BUNDLE
        const val BUNDLE_GLOSSARY_TOPIC: String = "sreGlossaryTopic"
        //VALIDATION
        const val NOT_VALIDATED: Int = 0
        const val VALIDATION_OK: Int = 1
        const val VALIDATION_EMPTY: Int = 2
        const val VALIDATION_FAILED: Int = 3
        const val VALIDATION_INVALID: Int = 4
        const val VALIDATION_NO_DATA: Int = 5
        //TAGS
        const val GENERAL_TAG: String = "SRE-TAG"
        const val BUNDLE_GLOSSARY_ADDITIONAL_TOPICS: String = "sreGlossaryAdditionalTopics"
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
        const val WALKTHROUGH_UID_IDENTIFIER: String = "walkthrough_"
        const val TUTORIAL_UID_IDENTIFIER: String = "tutorial_"
        const val HASH_MAP_UID_IDENTIFIER: String = "hash_map_"
        const val HASH_MAP_OPTIONS_IDENTIFIER: String = "hash_map_options_"
        const val HASH_MAP_LINK_IDENTIFIER: String = "hash_map_link_"
        const val ARRAY_LIST_WHAT_IF_IDENTIFIER: String = "array_list_what_if_"
        const val VERSIONING_IDENTIFIER: String = "version_"
        const val MIN_IDENTIFIER: String = "min_"
        const val MAX_IDENTIFIER: String = "max_"
        const val INIT_IDENTIFIER: String = "init_"
        //GENERAL
        const val NEW_LINE_C: Char = '\n'
        const val SPACE_C: Char = ' '
        const val EQUALS: String = "="
        const val NEW_LINE: String = "\n"
        const val PERCENT: String = "%"
        const val SPACE: String = " "
        const val COMMA: String = ","
        const val SEMI_COLON: String = ";"
        const val DOLLAR_STRING: Char = '$'
        const val NOTHING: String = ""
        const val DASH: String = "-"
        const val SLASH: String = "/"
        const val BOLD_START: String = "<b>"
        const val BOLD_END: String = "</b>"
        const val BREAK: String = "<br>"
        const val ARROW_RIGHT: String = "->"
        const val ARROW_LEFT: String = "<-"
        const val NONE: String = "None"
        const val NO_DATA: String = "No Data"
        const val NOT_CONSULTED: String = "Not consulted"
        //XML
        const val EMPTY_LIST: String = "EmptyList"
        const val NULL: String = "NULL"
        const val NULL_CLASS: String = "Null"
        const val HYPHEN: String = "-"
        const val STRING: String = "String"
        const val INT: String = "Int"
        const val FLOAT: String = "Float"
        const val LONG: String = "Long"
        const val DOUBLE: String = "Double"
        const val BOOLEAN: String = "Boolean"
        const val HASH_MAP_ENTRY: String = "HashMap\$HashMapEntry"
        //INPUT CONFIG
        const val SINGLE_SELECT: String = "singleSelect"
        const val SINGLE_SELECT_WITH_PRESET_POSITION: String = "singleSelect="
        const val READ_ONLY: String = "readOnly"
        const val COMPLETE_REMOVAL_DISABLED: String = "completeRemovalDisabled"
        const val SIMPLE_LOOKUP: String = "simpleLookup"
        //FILE FORMATS
        const val PNG_FILE: String = ".png"
        const val SRE_FILE: String = ".sre"
        //ELEMENTS
        const val STARTING_POINT: String = "starting_point"
        //ATTRIBUTE TYPES
        const val TYPE_OBJECT: String = "Object"
        const val TYPE_STANDARD_STEP: String = "StandardStep"
        const val TYPE_INPUT_STEP: String = "InputStep"
        const val TYPE_LOOPBACK_STEP: String = "LoopbackStep"
        const val TYPE_MERGE_STEP: String = "MergeStep"
        const val TYPE_RESOURCE_STEP: String = "ResourceStep"
        const val TYPE_BUTTON_TRIGGER: String = "ButtonTrigger"
        const val TYPE_IF_ELSE_TRIGGER: String = "IfElseTrigger"
        const val TYPE_STAKEHOLDER_INTERACTION_TRIGGER: String = "StakeholderInteractionTrigger"
        const val TYPE_TIME_TRIGGER: String = "TimeTrigger"
        const val TYPE_INPUT_TRIGGER: String = "InputTrigger"
        const val TYPE_SOUND_TRIGGER: String = "SoundTrigger"
        const val TYPE_BLUETOOTH_TRIGGER: String = "BluetoothTrigger"
        const val TYPE_GPS_TRIGGER: String = "GpsTrigger"
        const val TYPE_MOBILE_NETWORK_TRIGGER: String = "MobileNetworkTrigger"
        const val TYPE_NFC_TRIGGER: String = "NfcTrigger"
        const val TYPE_WIFI_TRIGGER: String = "WifiTrigger"
        const val TYPE_CALL_TRIGGER: String = "CallTrigger"
        const val TYPE_SMS_TRIGGER: String = "SmsTrigger"
        const val TYPE_ACCELERATION_TRIGGER: String = "AccelerationTrigger"
        const val TYPE_GYROSCOPE_TRIGGER: String = "GyroscopeTrigger"
        const val TYPE_LIGHT_TRIGGER: String = "LightTrigger"
        const val TYPE_MAGNETOMETER_TRIGGER: String = "MagnetometerTrigger"
        //COLORS
        const val CRIMSON: String = "#DC143C"
        const val DARK_RED: String = "#8B0000"
        const val GOLD: String = "#FED73B"
        const val MATERIAL_100_RED: String = "#F8BBD0"
        const val MATERIAL_100_VIOLET: String = "#E1BEE7"
        const val MATERIAL_100_BLUE: String = "#C5CAE9"
        const val MATERIAL_100_TURQUOISE: String = "#BBDEFB"
        const val MATERIAL_100_GREEN: String = "#C8E6C9"
        const val MATERIAL_100_LIME: String = "#F0F4C3"
        const val MATERIAL_100_YELLOW: String = "#FFF9C4"
        const val MATERIAL_100_ORANGE: String = "#FFCCBC"

        const val MATERIAL_700_RED: String = "#D32F2F"
        const val MATERIAL_700_VIOLET: String = "#7B1FA2"
        const val MATERIAL_700_BLUE: String = "#303F9F"
        const val MATERIAL_700_TURQUOISE: String = "#0097A7"
        const val MATERIAL_700_GREEN: String = "#388E3C"
        const val MATERIAL_700_LIME: String = "#AFB42B"
        const val MATERIAL_700_YELLOW: String = "#FBC02D"
        const val MATERIAL_700_ORANGE: String = "#E64A19"
    }
}