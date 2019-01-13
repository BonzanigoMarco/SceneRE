package uzh.scenere.datamodel.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns

abstract class AbstractSreDatabase {
    //Package Private
    protected val LIKE = " LIKE "
    protected val EQ = " = "
    protected val NEQ = " != "
    protected val ONE = " 1 "
    protected val MIN_ONE = " -1 "
    protected val ZERO = " 0 "
    protected val QUOTES = "'"
    protected val LIKE_WILDCARD = " LIKE ? "
    //Private
    protected val TEXT_TYPE = " TEXT "
    protected val NUMBER_TYPE = " INTEGER "
    protected val FLOATING_NUMBER_TYPE = " REAL "
    protected val DATA_TYPE = " BLOB "
    protected val KEY_TYPE = " PRIMARY KEY "
    protected val COMMA_SEP = ","
    protected val ALTER_TABLE = "ALTER TABLE "
    protected val RENAME_TO_TEMP = " RENAME TO 'TEMP_"
    protected val DROP_TABLE_IF_EXISTS = "DROP TABLE IF EXISTS "
    protected val INSERT_INTO = "INSERT INTO "
    protected val SUB_SELECT = " SELECT "
    protected val BRACES_OPEN = " ( "
    protected val BRACES_CLOSE = " ) "
    protected val FROM_TEMP = " FROM TEMP_"
    protected val CREATE_TABLE_IF_NOT_EXISTS = "CREATE TABLE IF NOT EXISTS "
    protected val DROP_TABLE_IF_EXISTS_TEMP = "DROP TABLE IF EXISTS TEMP_"

    //Creation
    private fun collectTableCreation(): Array<String> {
        return arrayOf(CREATE_TABLE_IF_NOT_EXISTS + NumberTableEntry.TABLE_NAME + BRACES_OPEN +
                NumberTableEntry._ID + NUMBER_TYPE + KEY_TYPE + COMMA_SEP +
                NumberTableEntry.COLUMN_NAME_KEY + TEXT_TYPE + COMMA_SEP +
                NumberTableEntry.COLUMN_NAME_VALUE + FLOATING_NUMBER_TYPE + COMMA_SEP +
                NumberTableEntry.COLUMN_NAME_TIMESTAMP + NUMBER_TYPE + BRACES_CLOSE,
                CREATE_TABLE_IF_NOT_EXISTS + TextTableEntry.TABLE_NAME + BRACES_OPEN +
                TextTableEntry._ID + NUMBER_TYPE + KEY_TYPE + COMMA_SEP +
                TextTableEntry.COLUMN_NAME_KEY + TEXT_TYPE + COMMA_SEP +
                TextTableEntry.COLUMN_NAME_VALUE + TEXT_TYPE + COMMA_SEP +
                TextTableEntry.COLUMN_NAME_TIMESTAMP + NUMBER_TYPE + BRACES_CLOSE,
                CREATE_TABLE_IF_NOT_EXISTS + DataTableEntry.TABLE_NAME + BRACES_OPEN +
                DataTableEntry._ID + NUMBER_TYPE + KEY_TYPE + COMMA_SEP +
                DataTableEntry.COLUMN_NAME_KEY + TEXT_TYPE + COMMA_SEP +
                DataTableEntry.COLUMN_NAME_VALUE + DATA_TYPE + COMMA_SEP +
                DataTableEntry.COLUMN_NAME_TIMESTAMP + NUMBER_TYPE + BRACES_CLOSE,
                CREATE_TABLE_IF_NOT_EXISTS + ProjectTableEntry.TABLE_NAME + BRACES_OPEN +
                        ProjectTableEntry._ID + NUMBER_TYPE + KEY_TYPE + COMMA_SEP +
                        ProjectTableEntry.ID + TEXT_TYPE + COMMA_SEP +
                        ProjectTableEntry.TITLE + TEXT_TYPE + COMMA_SEP +
                        ProjectTableEntry.DESCRIPTION + TEXT_TYPE + COMMA_SEP +
                        ProjectTableEntry.CREATOR + TEXT_TYPE + BRACES_CLOSE,
                CREATE_TABLE_IF_NOT_EXISTS + StakeholderTableEntry.TABLE_NAME + BRACES_OPEN +
                        StakeholderTableEntry._ID + NUMBER_TYPE + KEY_TYPE + COMMA_SEP +
                        StakeholderTableEntry.ID + TEXT_TYPE + COMMA_SEP +
                        StakeholderTableEntry.PROJECT_ID + TEXT_TYPE + COMMA_SEP +
                        StakeholderTableEntry.NAME + TEXT_TYPE + COMMA_SEP +
                        StakeholderTableEntry.DESCRIPTION + TEXT_TYPE + BRACES_CLOSE,
                CREATE_TABLE_IF_NOT_EXISTS + ObjectTableEntry.TABLE_NAME + BRACES_OPEN +
                        ObjectTableEntry._ID + NUMBER_TYPE + KEY_TYPE + COMMA_SEP +
                        ObjectTableEntry.ID + TEXT_TYPE + COMMA_SEP +
                        ObjectTableEntry.SCENARIO_ID + TEXT_TYPE + COMMA_SEP +
                        ObjectTableEntry.NAME + TEXT_TYPE + COMMA_SEP +
                        ObjectTableEntry.DESCRIPTION + TEXT_TYPE + BRACES_CLOSE,
                CREATE_TABLE_IF_NOT_EXISTS + AttributeTableEntry.TABLE_NAME + BRACES_OPEN +
                        AttributeTableEntry._ID + NUMBER_TYPE + KEY_TYPE + COMMA_SEP +
                        AttributeTableEntry.ID + TEXT_TYPE + COMMA_SEP +
                        AttributeTableEntry.REF_ID + TEXT_TYPE + COMMA_SEP +
                        AttributeTableEntry.KEY + TEXT_TYPE + COMMA_SEP +
                        AttributeTableEntry.VALUE + TEXT_TYPE + BRACES_CLOSE)
    }

    //Upgrade
    private fun collectTableBackup(): Array<String> {
        return arrayOf(ALTER_TABLE + NumberTableEntry.TABLE_NAME + RENAME_TO_TEMP + NumberTableEntry.TABLE_NAME + "'",
                ALTER_TABLE + TextTableEntry.TABLE_NAME + RENAME_TO_TEMP + TextTableEntry.TABLE_NAME + "'",
                ALTER_TABLE + DataTableEntry.TABLE_NAME + RENAME_TO_TEMP + DataTableEntry.TABLE_NAME + "'",
                ALTER_TABLE + ProjectTableEntry.TABLE_NAME + RENAME_TO_TEMP + ProjectTableEntry.TABLE_NAME + "'",
                ALTER_TABLE + StakeholderTableEntry.TABLE_NAME + RENAME_TO_TEMP + StakeholderTableEntry.TABLE_NAME + "'",
                ALTER_TABLE + ObjectTableEntry.TABLE_NAME + RENAME_TO_TEMP + ObjectTableEntry.TABLE_NAME + "'",
                ALTER_TABLE + AttributeTableEntry.TABLE_NAME + RENAME_TO_TEMP + AttributeTableEntry.TABLE_NAME + "'")
    }

    private fun collectTableDeletion(): Array<String> {
        return arrayOf(DROP_TABLE_IF_EXISTS + NumberTableEntry.TABLE_NAME,
                DROP_TABLE_IF_EXISTS + TextTableEntry.TABLE_NAME,
                DROP_TABLE_IF_EXISTS + DataTableEntry.TABLE_NAME,
                DROP_TABLE_IF_EXISTS + ProjectTableEntry.TABLE_NAME,
                DROP_TABLE_IF_EXISTS + StakeholderTableEntry.TABLE_NAME,
                DROP_TABLE_IF_EXISTS + ObjectTableEntry.TABLE_NAME,
                DROP_TABLE_IF_EXISTS + AttributeTableEntry.TABLE_NAME)
    }

    private fun collectTableRestoration(): Array<String> {
        return arrayOf(
                INSERT_INTO + NumberTableEntry.TABLE_NAME + BRACES_OPEN +
                        NumberTableEntry._ID + COMMA_SEP +
                        NumberTableEntry.COLUMN_NAME_KEY + COMMA_SEP +
                        NumberTableEntry.COLUMN_NAME_VALUE + COMMA_SEP +
                        NumberTableEntry.COLUMN_NAME_TIMESTAMP +
                        BRACES_CLOSE + SUB_SELECT +
                        NumberTableEntry._ID + COMMA_SEP +
                        NumberTableEntry.COLUMN_NAME_KEY + COMMA_SEP +
                        NumberTableEntry.COLUMN_NAME_VALUE + COMMA_SEP +
                        NumberTableEntry.COLUMN_NAME_TIMESTAMP + FROM_TEMP + NumberTableEntry.TABLE_NAME,
                INSERT_INTO + TextTableEntry.TABLE_NAME + BRACES_OPEN +
                        TextTableEntry._ID + COMMA_SEP +
                        TextTableEntry.COLUMN_NAME_KEY + COMMA_SEP +
                        TextTableEntry.COLUMN_NAME_VALUE + COMMA_SEP +
                        TextTableEntry.COLUMN_NAME_TIMESTAMP +
                        BRACES_CLOSE + SUB_SELECT +
                        TextTableEntry._ID + COMMA_SEP +
                        TextTableEntry.COLUMN_NAME_KEY + COMMA_SEP +
                        TextTableEntry.COLUMN_NAME_VALUE + COMMA_SEP +
                        TextTableEntry.COLUMN_NAME_TIMESTAMP + FROM_TEMP + TextTableEntry.TABLE_NAME,
                INSERT_INTO + DataTableEntry.TABLE_NAME + BRACES_OPEN +
                        DataTableEntry._ID + COMMA_SEP +
                        DataTableEntry.COLUMN_NAME_KEY + COMMA_SEP +
                        DataTableEntry.COLUMN_NAME_VALUE + COMMA_SEP +
                        DataTableEntry.COLUMN_NAME_TIMESTAMP +
                        BRACES_CLOSE + SUB_SELECT +
                        DataTableEntry._ID + COMMA_SEP +
                        DataTableEntry.COLUMN_NAME_KEY + COMMA_SEP +
                        DataTableEntry.COLUMN_NAME_VALUE + COMMA_SEP +
                        DataTableEntry.COLUMN_NAME_TIMESTAMP + FROM_TEMP + DataTableEntry.TABLE_NAME,
                INSERT_INTO + ProjectTableEntry.TABLE_NAME + BRACES_OPEN +
                        ProjectTableEntry._ID + COMMA_SEP +
                        ProjectTableEntry.ID + COMMA_SEP +
                        ProjectTableEntry.TITLE + COMMA_SEP +
                        ProjectTableEntry.DESCRIPTION + COMMA_SEP +
                        ProjectTableEntry.CREATOR +
                        BRACES_CLOSE + SUB_SELECT +
                        ProjectTableEntry._ID + COMMA_SEP +
                        ProjectTableEntry.ID + COMMA_SEP +
                        ProjectTableEntry.TITLE + COMMA_SEP +
                        ProjectTableEntry.DESCRIPTION + COMMA_SEP +
                        ProjectTableEntry.CREATOR + FROM_TEMP + ProjectTableEntry.TABLE_NAME,
                INSERT_INTO + StakeholderTableEntry.TABLE_NAME + BRACES_OPEN +
                        StakeholderTableEntry._ID + COMMA_SEP +
                        StakeholderTableEntry.ID + COMMA_SEP +
                        StakeholderTableEntry.PROJECT_ID + COMMA_SEP +
                        StakeholderTableEntry.NAME + COMMA_SEP +
                        StakeholderTableEntry.DESCRIPTION +
                        BRACES_CLOSE + SUB_SELECT +
                        StakeholderTableEntry._ID + COMMA_SEP +
                        StakeholderTableEntry.ID + COMMA_SEP +
                        StakeholderTableEntry.PROJECT_ID + COMMA_SEP +
                        StakeholderTableEntry.NAME + COMMA_SEP +
                        StakeholderTableEntry.DESCRIPTION + FROM_TEMP + StakeholderTableEntry.TABLE_NAME,
                INSERT_INTO + ObjectTableEntry.TABLE_NAME + BRACES_OPEN +
                        ObjectTableEntry._ID + COMMA_SEP +
                        ObjectTableEntry.ID + COMMA_SEP +
                        ObjectTableEntry.SCENARIO_ID + COMMA_SEP +
                        ObjectTableEntry.NAME + COMMA_SEP +
                        ObjectTableEntry.DESCRIPTION +
                        BRACES_CLOSE + SUB_SELECT +
                        ObjectTableEntry._ID + COMMA_SEP +
                        ObjectTableEntry.ID + COMMA_SEP +
                        ObjectTableEntry.SCENARIO_ID + COMMA_SEP +
                        ObjectTableEntry.NAME + COMMA_SEP +
                        ObjectTableEntry.DESCRIPTION + FROM_TEMP + ObjectTableEntry.TABLE_NAME,
                INSERT_INTO + AttributeTableEntry.TABLE_NAME + BRACES_OPEN +
                        AttributeTableEntry._ID + COMMA_SEP +
                        AttributeTableEntry.ID + COMMA_SEP +
                        AttributeTableEntry.REF_ID + COMMA_SEP +
                        AttributeTableEntry.KEY + COMMA_SEP +
                        AttributeTableEntry.VALUE +
                        BRACES_CLOSE + SUB_SELECT +
                        AttributeTableEntry._ID + COMMA_SEP +
                        AttributeTableEntry.ID + COMMA_SEP +
                        AttributeTableEntry.REF_ID + COMMA_SEP +
                        AttributeTableEntry.KEY + COMMA_SEP +
                        AttributeTableEntry.VALUE + FROM_TEMP + AttributeTableEntry.TABLE_NAME)
    }

    private fun collectTableCleanup(): Array<String> {
        return arrayOf(DROP_TABLE_IF_EXISTS_TEMP + NumberTableEntry.TABLE_NAME,
                DROP_TABLE_IF_EXISTS_TEMP + TextTableEntry.TABLE_NAME,
                DROP_TABLE_IF_EXISTS_TEMP + DataTableEntry.TABLE_NAME,
                DROP_TABLE_IF_EXISTS_TEMP + ProjectTableEntry.TABLE_NAME,
                DROP_TABLE_IF_EXISTS_TEMP + StakeholderTableEntry.TABLE_NAME,
                DROP_TABLE_IF_EXISTS_TEMP + ObjectTableEntry.TABLE_NAME,
                DROP_TABLE_IF_EXISTS_TEMP + AttributeTableEntry.TABLE_NAME)
    }

    protected interface HashMapDatabase{
        companion object {
            const val COLUMN_NAME_KEY = " KEY "
            const val COLUMN_NAME_VALUE = " VALUE "
            const val COLUMN_NAME_TIMESTAMP = " TIMESTAMP "
        }
    }

    //Table for Numbers
    protected class NumberTableEntry private constructor() : BaseColumns, HashMapDatabase {
        companion object {
            const val _ID = BaseColumns._ID
            const val TABLE_NAME = " NUMBER_TABLE "
            const val COLUMN_NAME_KEY = HashMapDatabase.COLUMN_NAME_KEY
            const val COLUMN_NAME_VALUE = HashMapDatabase.COLUMN_NAME_VALUE
            const val COLUMN_NAME_TIMESTAMP = HashMapDatabase.COLUMN_NAME_TIMESTAMP
        }
    }

    //Table for Texts
    protected class TextTableEntry private constructor() : BaseColumns {
        companion object {
            const val _ID = BaseColumns._ID
            const val TABLE_NAME = " TEXT_TABLE "
            const val COLUMN_NAME_KEY = HashMapDatabase.COLUMN_NAME_KEY
            const val COLUMN_NAME_VALUE = HashMapDatabase.COLUMN_NAME_VALUE
            const val COLUMN_NAME_TIMESTAMP = HashMapDatabase.COLUMN_NAME_TIMESTAMP
        }
    }

    //Table for Data
    protected class DataTableEntry private constructor() : BaseColumns {
        companion object {
            const val _ID = BaseColumns._ID
            const val TABLE_NAME = " DATA_TABLE "
            const val COLUMN_NAME_KEY = HashMapDatabase.COLUMN_NAME_KEY
            const val COLUMN_NAME_VALUE = HashMapDatabase.COLUMN_NAME_VALUE
            const val COLUMN_NAME_TIMESTAMP = HashMapDatabase.COLUMN_NAME_TIMESTAMP
        }
    }

    //Table for Projects
    protected class ProjectTableEntry private constructor() : BaseColumns {
        companion object {
            const val _ID = BaseColumns._ID
            const val TABLE_NAME = " PROJECT_TABLE "
            const val ID = " ID "
            const val CREATOR = " CREATOR "
            const val TITLE = " TITLE "
            const val DESCRIPTION = " DESCRIPTION "
        }
    }
    //Table for Stakeholder
    protected class StakeholderTableEntry private constructor() : BaseColumns {
        companion object {
            const val _ID = BaseColumns._ID
            const val TABLE_NAME = " STAKEHOLDER_TABLE "
            const val ID = " ID "
            const val PROJECT_ID = " PROJECT_ID "
            const val NAME = " NAME "
            const val DESCRIPTION = " DESCRIPTION "
        }
    }

    //Table for Objects
    protected class ObjectTableEntry private constructor() : BaseColumns {
        companion object {
            const val _ID = BaseColumns._ID
            const val TABLE_NAME = " OBJECT_TABLE "
            const val ID = " ID "
            const val SCENARIO_ID = " SCENARIO_ID "
            const val NAME = " NAME "
            const val DESCRIPTION = " DESCRIPTION "
        }
    }

    //Table for Attributes
    protected class AttributeTableEntry private constructor() : BaseColumns {
        companion object {
            const val _ID = BaseColumns._ID
            const val TABLE_NAME = " ATTRIBUTE_TABLE "
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
            for (creation in collectTableCreation()) {
                db.execSQL(creation)
            }
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            for (backup in collectTableBackup()) {
                db.execSQL(backup)
            }
            for (deletion in collectTableDeletion()) {
                db.execSQL(deletion)
            }
            onCreate(db)
            for (restoration in collectTableRestoration()) {
                db.execSQL(restoration)
            }
            for (cleanup in collectTableCleanup()) {
                db.execSQL(cleanup)
            }
        }

        override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            onUpgrade(db, oldVersion, newVersion)
        }


    }

}