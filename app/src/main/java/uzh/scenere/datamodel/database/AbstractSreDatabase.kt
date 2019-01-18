package uzh.scenere.datamodel.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns
import uzh.scenere.helpers.StringHelper

abstract class AbstractSreDatabase {
    val tableCreations = ArrayList<String>()
    val tableBackups = ArrayList<String>()
    val tableDeletions = ArrayList<String>()
    val tableRestorations = ArrayList<String>()
    val tableCleanup = ArrayList<String>()

    companion object {
        //Package Private
        const val EMPTY = ""
        const val SPACE = " "
        const val LIKE = " LIKE "
        const val EQ = " = "
        const val NEQ = " != "
        const val ONE = " 1 "
        const val MIN_ONE = " -1 "
        const val ZERO = " 0 "
        const val QUOTES = "'"
        const val LIKE_WILDCARD = " LIKE ? "
        //Private
        const val TEXT_TYPE = " TEXT "
        const val NUMBER_TYPE = " INTEGER "
        const val FLOATING_NUMBER_TYPE = " REAL "
        const val DATA_TYPE = " BLOB "
        const val KEY_TYPE = " PRIMARY KEY "
        const val COMMA_SEP = ","
        const val ALTER_TABLE = "ALTER TABLE "
        const val RENAME_TO_TEMP = " RENAME TO 'TEMP_"
        const val DROP_TABLE_IF_EXISTS = "DROP TABLE IF EXISTS "
        const val INSERT_INTO = "INSERT INTO "
        const val SUB_SELECT = " SELECT "
        const val BRACES_OPEN = " ( "
        const val BRACES_CLOSE = " ) "
        const val FROM_TEMP = " FROM TEMP_"
        const val CREATE_TABLE_IF_NOT_EXISTS = "CREATE TABLE IF NOT EXISTS "
        const val DROP_TABLE_IF_EXISTS_TEMP = "DROP TABLE IF EXISTS TEMP_"
        //Reflection
        const val BASE_COLUMNS_ID = "android.provider.BaseColumns"
        val COLUMN_TYPE = "COLUMN_TYPE_"
        val TABLE_NAME = "TABLE_NAME"
        val ID_COLUMN = "_ID"
    }

    init {
        collectTables()
    }

    //Creation
    private fun collectTables() {
        tableCreations.clear()
        for (declaredClass in AbstractSreDatabase::class.java.declaredClasses) {
            var create = false
            for (interfaces in declaredClass.interfaces) {
                if (interfaces.name == BASE_COLUMNS_ID) create = true
            }
            if (!create) continue
            val columns = HashMap<String, String>()
            var tableName: String? = null
            var idColumnExists: Boolean = false
            for (field in declaredClass.fields) {
                if (field.declaringClass.name == BASE_COLUMNS_ID) {
                    when (ID_COLUMN) {
                        field.name -> idColumnExists = true
                    }
                    continue
                }
                when {
                    field.name.contains(TABLE_NAME) -> tableName = (field.get(declaredClass) as String).replace(" ", "")
                    field.name.contains(COLUMN_TYPE) -> columns[StringHelper.extractNameFromClassString(field.name).replace(COLUMN_TYPE, SPACE).plus(SPACE)] = field.get(declaredClass) as String
                    field.name == ID_COLUMN -> columns[" " + StringHelper.extractNameFromClassString(field.name).toLowerCase().plus(" ")] = NUMBER_TYPE + KEY_TYPE
                }
            }
            tableCreations.add(composeCreationStatement(tableName, columns, idColumnExists))
            tableBackups.add(ALTER_TABLE + tableName + RENAME_TO_TEMP + tableName + QUOTES)
            tableDeletions.add(DROP_TABLE_IF_EXISTS + tableName)
            tableRestorations.add(composeRestorationStatement(tableName, columns, idColumnExists))
            tableCleanup.add(DROP_TABLE_IF_EXISTS_TEMP + tableName)
        }
    }

    private fun composeCreationStatement(tableName: String?, columns: HashMap<String, String>, idColumnExists: Boolean): String {
        if (tableName == null || columns.isEmpty()) {
            return ""
        }
        var statement: String = CREATE_TABLE_IF_NOT_EXISTS.plus(tableName).plus(BRACES_OPEN)
        statement = concatenateColumns(idColumnExists, statement, columns) + BRACES_CLOSE
        return statement
    }

    private fun composeRestorationStatement(tableName: String?, columns: HashMap<String, String>, idColumnExists: Boolean): String {
        if (tableName == null || columns.isEmpty()) {
            return ""
        }
        var statement: String = INSERT_INTO.plus(tableName).plus(BRACES_OPEN)
        statement = concatenateColumns(idColumnExists, statement, columns, false) + BRACES_CLOSE + SUB_SELECT
        statement = concatenateColumns(idColumnExists, statement, columns, false) + FROM_TEMP + tableName

        return statement
    }

    private fun concatenateColumns(idColumnExists: Boolean, statement: String, columns: HashMap<String, String>, withDataType: Boolean = true): String {
        var statement1 = statement
        if (idColumnExists) {
            statement1 = statement1.plus(SPACE + ID_COLUMN + SPACE).plus(if (withDataType) (NUMBER_TYPE + KEY_TYPE) else EMPTY).plus(COMMA_SEP)
        }
        for (entry in columns) {
            statement1 = statement1.plus(entry.key).plus(if (withDataType) entry.value else EMPTY).plus(COMMA_SEP)
        }
        statement1 = statement1.substring(0, statement1.length - COMMA_SEP.length)
        return statement1
    }

    //Table for Numbers
    protected class NumberTableEntry private constructor() : BaseColumns {
        companion object {
            const val TABLE_NAME = " NUMBER_TABLE "
            const val COLUMN_TYPE_KEY = TEXT_TYPE
            const val COLUMN_TYPE_VALUE = FLOATING_NUMBER_TYPE
            const val COLUMN_TYPE_TIMESTAMP = NUMBER_TYPE
            const val KEY = " KEY "
            const val VALUE = " VALUE "
            const val TIMESTAMP = " TIMESTAMP "
        }
    }

    //Table for Texts
    protected class TextTableEntry private constructor() : BaseColumns {
        companion object {
            const val TABLE_NAME = " TEXT_TABLE "
            const val COLUMN_TYPE_KEY = TEXT_TYPE
            const val COLUMN_TYPE_VALUE = TEXT_TYPE
            const val COLUMN_TYPE_TIMESTAMP = NUMBER_TYPE
            const val KEY = " KEY "
            const val VALUE = " VALUE "
            const val TIMESTAMP = " TIMESTAMP "
        }
    }

    //Table for Data
    protected class DataTableEntry private constructor() : BaseColumns {
        companion object {
            const val TABLE_NAME = " DATA_TABLE "
            const val COLUMN_TYPE_KEY = TEXT_TYPE
            const val COLUMN_TYPE_VALUE = DATA_TYPE
            const val COLUMN_TYPE_TIMESTAMP = NUMBER_TYPE
            const val KEY = " KEY "
            const val VALUE = " VALUE "
            const val TIMESTAMP = " TIMESTAMP "
        }
    }

    //Table for Projects
    protected class ProjectTableEntry private constructor() : BaseColumns {
        companion object {
            const val TABLE_NAME = " PROJECT_TABLE "
            const val COLUMN_TYPE_ID = TEXT_TYPE
            const val COLUMN_TYPE_CREATOR = TEXT_TYPE
            const val COLUMN_TYPE_TITLE = TEXT_TYPE
            const val COLUMN_TYPE_DESCRIPTION = TEXT_TYPE
            const val ID = " ID "
            const val CREATOR = " CREATOR "
            const val TITLE = " TITLE "
            const val DESCRIPTION = " DESCRIPTION "
        }
    }

    //Table for Stakeholder
    protected class StakeholderTableEntry private constructor() : BaseColumns {
        companion object {
            const val TABLE_NAME = " STAKEHOLDER_TABLE "
            const val COLUMN_TYPE_ID = TEXT_TYPE
            const val COLUMN_TYPE_PROJECT_ID = TEXT_TYPE
            const val COLUMN_TYPE_NAME = TEXT_TYPE
            const val COLUMN_TYPE_DESCRIPTION = TEXT_TYPE
            const val ID = " ID "
            const val PROJECT_ID = " PROJECT_ID "
            const val NAME = " NAME "
            const val DESCRIPTION = " DESCRIPTION "
        }
    }

    //Table for Objects
    protected class ObjectTableEntry private constructor() : BaseColumns {
        companion object {
            const val TABLE_NAME = " OBJECT_TABLE "
            const val COLUMN_TYPE_ID = TEXT_TYPE
            const val COLUMN_TYPE_SCENARIO_ID = TEXT_TYPE
            const val COLUMN_TYPE_NAME = TEXT_TYPE
            const val COLUMN_TYPE_DESCRIPTION = TEXT_TYPE
            const val ID = " ID "
            const val SCENARIO_ID = " SCENARIO_ID "
            const val NAME = " NAME "
            const val DESCRIPTION = " DESCRIPTION "
        }
    }

    //Table for Scenarios
    protected class ScenarioTableEntry private constructor() : BaseColumns {
        companion object {
            const val TABLE_NAME = " SCENARIO_TABLE "
            const val COLUMN_TYPE_ID = TEXT_TYPE
            const val COLUMN_TYPE_PROJECT_ID = TEXT_TYPE
            const val COLUMN_TYPE_TITLE = TEXT_TYPE
            const val COLUMN_TYPE_INTRO = TEXT_TYPE
            const val COLUMN_TYPE_OUTRO = TEXT_TYPE
            const val ID = " ID "
            const val PROJECT_ID = " PROJECT_ID "
            const val TITLE = " TITLE "
            const val INTRO = " INTRO "
            const val OUTRO = " OUTRO "
        }
    }

    //Table for Attributes
    protected class AttributeTableEntry private constructor() : BaseColumns {
        companion object {
            const val TABLE_NAME = " ATTRIBUTE_TABLE "
            const val COLUMN_TYPE_ID = TEXT_TYPE
            const val COLUMN_TYPE_REF_ID = TEXT_TYPE
            const val COLUMN_TYPE_KEY = TEXT_TYPE
            const val COLUMN_TYPE_VALUE = TEXT_TYPE
            const val ID = " ID "
            const val REF_ID = " REF_ID "
            const val KEY = " KEY "
            const val VALUE = " VALUE "
        }
    }

    protected inner class DbHelper(context: Context,
                                   private val DATABASE_VERSION: Int = 1,
                                   private val DATABASE_NAME: String = "SreDatabase.Db",
                                   private val DATABASE_ENDING: String = ".db",
                                   private val FILE_DIR: String = "SRE"
    ) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

        init {
            onCreate(writableDatabase)
        }

        override fun onCreate(db: SQLiteDatabase) {
            for (creation in tableCreations) {
                db.execSQL(creation)
            }
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            for (backup in tableBackups) {
                db.execSQL(backup)
            }
            for (deletion in tableDeletions) {
                db.execSQL(deletion)
            }
            onCreate(db)
            for (restoration in tableRestorations) {
                db.execSQL(restoration)
            }
            for (cleanup in tableCleanup) {
                db.execSQL(cleanup)
            }
        }

        override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            onUpgrade(db, oldVersion, newVersion)
        }
    }
}