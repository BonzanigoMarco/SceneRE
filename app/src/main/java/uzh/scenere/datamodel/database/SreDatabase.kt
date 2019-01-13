package uzh.scenere.datamodel.database

import android.content.ContentValues
import android.content.Context
import com.google.android.gms.common.util.NumberUtils
import uzh.scenere.datamodel.Project
import uzh.scenere.helpers.NumberHelper
import uzh.scenere.helpers.ObjectHelper
import uzh.scenere.helpers.PermissionHelper

class SreDatabase private constructor(context: Context) : AbstractSreDatabase() {
    private val dbHelper: DbHelper = DbHelper(context)

    companion object {
        // Volatile: writes to this field are immediately made visible to other threads.
        @Volatile
        private var instance: SreDatabase? = null

        fun getInstance(context: Context): SreDatabase {
            return when {
                instance != null -> instance!!
                else -> synchronized(this) {
                    if (instance == null) {
                        instance = SreDatabase(context)
                    }
                    instance!!
                }
            }
        }
    }

    /** WRITE    **
     ** TO       **
     ** DATABASE **/
    fun writeByteArray(key: String, value: ByteArray): Long {
        val values = ContentValues()
        values.put(DataTableEntry.COLUMN_NAME_KEY, key)
        values.put(DataTableEntry.COLUMN_NAME_VALUE, value)
        return insert(DataTableEntry.TABLE_NAME, DataTableEntry.COLUMN_NAME_KEY, key, values)
    }
    fun writeString(key: String, value: String): Long {
        val values = ContentValues()
        values.put(TextTableEntry.COLUMN_NAME_KEY, key)
        values.put(TextTableEntry.COLUMN_NAME_VALUE, value)
        return insert(TextTableEntry.TABLE_NAME, TextTableEntry.COLUMN_NAME_KEY, key, values)
    }
    fun writeBoolean(key: String, value: Boolean): Long {
        val values = ContentValues()
        values.put(DataTableEntry.COLUMN_NAME_KEY, key)
        values.put(DataTableEntry.COLUMN_NAME_VALUE, value)
        return insert(NumberTableEntry.TABLE_NAME, NumberTableEntry.COLUMN_NAME_KEY, key, values)
    }
    fun writeShort(key: String, value: Short): Long {
        val values = ContentValues()
        values.put(NumberTableEntry.COLUMN_NAME_KEY, key)
        values.put(NumberTableEntry.COLUMN_NAME_VALUE, value)
        return insert(NumberTableEntry.TABLE_NAME, NumberTableEntry.COLUMN_NAME_KEY, key, values)
    }
    fun writeInt(key: String, value: Int): Long {
        val values = ContentValues()
        values.put(NumberTableEntry.COLUMN_NAME_KEY, key)
        values.put(NumberTableEntry.COLUMN_NAME_VALUE, value)
        return insert(NumberTableEntry.TABLE_NAME, NumberTableEntry.COLUMN_NAME_KEY, key, values)
    }
    fun writeLong(key: String, value: Long): Long {
        val values = ContentValues()
        values.put(NumberTableEntry.COLUMN_NAME_KEY, key)
        values.put(NumberTableEntry.COLUMN_NAME_VALUE, value)
        return insert(NumberTableEntry.TABLE_NAME, NumberTableEntry.COLUMN_NAME_KEY, key, values)
    }
    fun writeFloat(key: String, value: Float): Long {
        val values = ContentValues()
        values.put(NumberTableEntry.COLUMN_NAME_KEY, key)
        values.put(NumberTableEntry.COLUMN_NAME_VALUE, value)
        return insert(NumberTableEntry.TABLE_NAME, NumberTableEntry.COLUMN_NAME_KEY, key, values)
    }
    fun writeDouble(key: String, value: Double): Long {
        val values = ContentValues()
        values.put(NumberTableEntry.COLUMN_NAME_KEY, key)
        values.put(NumberTableEntry.COLUMN_NAME_VALUE, value)
        return insert(NumberTableEntry.TABLE_NAME, NumberTableEntry.COLUMN_NAME_KEY, key, values)
    }
    fun writeProject(project: Project): Long {
        val values = ContentValues()
        values.put(ProjectTableEntry.ID, project.id)
        values.put(ProjectTableEntry.CREATOR, project.creator)
        values.put(ProjectTableEntry.TITLE, project.title)
        values.put(ProjectTableEntry.DESCRIPTION, project.description)
        return insert(ProjectTableEntry.TABLE_NAME, ProjectTableEntry.ID, project.id, values)
    }

    /** READ     **
     ** FROM     **
     ** DATABASE **/
    fun readByteArray(key: String, valueIfNull: ByteArray): ByteArray {
        val db = dbHelper.readableDatabase
        val cursor = db.query(DataTableEntry.TABLE_NAME, arrayOf(DataTableEntry.COLUMN_NAME_VALUE), DataTableEntry.COLUMN_NAME_KEY + LIKE + QUOTES + key + QUOTES, null, null, null, null, null)
        var bytes: ByteArray? = null
        if (cursor.moveToFirst()) {
            bytes = cursor.getBlob(0)
        }
        cursor.close()
        return ObjectHelper.nvl(bytes, valueIfNull)
    }
    fun readString(key: String, valueIfNull: String): String {
        val db = dbHelper.readableDatabase
        val cursor = db.query(TextTableEntry.TABLE_NAME, arrayOf(TextTableEntry.COLUMN_NAME_VALUE), TextTableEntry.COLUMN_NAME_KEY + LIKE + QUOTES + key + QUOTES, null, null, null, null, null)
        var d: String? = null
        if (cursor.moveToFirst()) {
            d = cursor.getString(0)
        }
        cursor.close()
        return ObjectHelper.nvl(d, valueIfNull)
    }
    fun readBoolean(key: String, valueIfNull: Boolean): Boolean {
        val db = dbHelper.readableDatabase
        val cursor = db.query(NumberTableEntry.TABLE_NAME, arrayOf(NumberTableEntry.COLUMN_NAME_VALUE), NumberTableEntry.COLUMN_NAME_KEY + LIKE + QUOTES + key + QUOTES, null, null, null, null, null)
        var b: Int? = null
        if (cursor.moveToFirst()) {
            b = cursor.getInt(0)
        }
        cursor.close()
        return ObjectHelper.nvl(NumberHelper.nvl(b,0)==1, valueIfNull)
    }
    fun readShort(key: String, valueIfNull: Short): Short {
        val db = dbHelper.readableDatabase
        val cursor = db.query(NumberTableEntry.TABLE_NAME, arrayOf(NumberTableEntry.COLUMN_NAME_VALUE), NumberTableEntry.COLUMN_NAME_KEY + LIKE + QUOTES + key + QUOTES, null, null, null, null, null)
        var s: Short? = null
        if (cursor.moveToFirst()) {
            s = cursor.getShort(0)
        }
        cursor.close()
        return NumberHelper.nvl(s, valueIfNull)
    }
    fun readInt(key: String, valueIfNull: Int): Int {
        val db = dbHelper.readableDatabase
        val cursor = db.query(NumberTableEntry.TABLE_NAME, arrayOf(NumberTableEntry.COLUMN_NAME_VALUE), NumberTableEntry.COLUMN_NAME_KEY + LIKE + QUOTES + key + QUOTES, null, null, null, null, null)
        var i: Int? = null
        if (cursor.moveToFirst()) {
            i = cursor.getInt(0)
        }
        cursor.close()
        return NumberHelper.nvl(i, valueIfNull)
    }
    fun readLong(key: String, valueIfNull: Long): Long {
        val db = dbHelper.readableDatabase
        val cursor = db.query(NumberTableEntry.TABLE_NAME, arrayOf(NumberTableEntry.COLUMN_NAME_VALUE), NumberTableEntry.COLUMN_NAME_KEY + LIKE + QUOTES + key + QUOTES, null, null, null, null, null)
        var l: Long? = null
        if (cursor.moveToFirst()) {
            l = cursor.getLong(0)
        }
        cursor.close()
        return NumberHelper.nvl(l, valueIfNull)
    }
    fun readFloat(key: String, valueIfNull: Float): Float {
        val db = dbHelper.readableDatabase
        val cursor = db.query(NumberTableEntry.TABLE_NAME, arrayOf(NumberTableEntry.COLUMN_NAME_VALUE), NumberTableEntry.COLUMN_NAME_KEY + LIKE + QUOTES + key + QUOTES, null, null, null, null, null)
        var f: Float? = null
        if (cursor.moveToFirst()) {
            f = cursor.getFloat(0)
        }
        cursor.close()
        return NumberHelper.nvl(f, valueIfNull)
    }
    fun readDouble(key: String, valueIfNull: Double): Double {
        val db = dbHelper.readableDatabase
        val cursor = db.query(NumberTableEntry.TABLE_NAME, arrayOf(NumberTableEntry.COLUMN_NAME_VALUE), NumberTableEntry.COLUMN_NAME_KEY + LIKE + QUOTES + key + QUOTES, null, null, null, null, null)
        var d: Double? = null
        if (cursor.moveToFirst()) {
            d = cursor.getDouble(0)
        }
        cursor.close()
        return NumberHelper.nvl(d, valueIfNull)
    }
    fun readProjects(): List<Project> {
        val db = dbHelper.readableDatabase
        val cursor = db.query(ProjectTableEntry.TABLE_NAME, arrayOf(ProjectTableEntry.ID,ProjectTableEntry.CREATOR,ProjectTableEntry.TITLE,ProjectTableEntry.DESCRIPTION), ONE+EQ+ONE, null, null, null, null, null)
        val projects = ArrayList<Project>()
        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getString(0)
                val creator = cursor.getString(1)
                val title = cursor.getString(2)
                val description = cursor.getString(3)
                projects.add(Project.ProjectBuilder(id, creator, title, description).build())
            }while(cursor.moveToNext())
        }
        cursor.close()
        return projects
    }
    fun readProject(key: String, valueIfNull: Project): Project {
        val db = dbHelper.readableDatabase
        val cursor = db.query(ProjectTableEntry.TABLE_NAME, arrayOf(ProjectTableEntry.ID,ProjectTableEntry.CREATOR,ProjectTableEntry.TITLE,ProjectTableEntry.DESCRIPTION), ProjectTableEntry.ID+ LIKE + QUOTES + key + QUOTES, null, null, null, null, null)
        if (cursor.moveToFirst()) {
            val id = cursor.getString(0)
            val creator = cursor.getString(1)
            val title = cursor.getString(2)
            val description = cursor.getString(3)
            return Project.ProjectBuilder(id, creator, title, description).build()
        }
        cursor.close()
        return valueIfNull
    }

    /** DELETE     **
     ** FROM     **
     ** DATABASE **/
    fun deleteNumber(key: String) {
        delete(NumberTableEntry.TABLE_NAME, NumberTableEntry.COLUMN_NAME_KEY, key)
    }
    fun truncateNumbers(){
        truncate(NumberTableEntry.TABLE_NAME)
    }

    fun deleteString(key: String) {
        delete(TextTableEntry.TABLE_NAME, TextTableEntry.COLUMN_NAME_KEY, key)
    }
    fun truncateStrings(){
        truncate(TextTableEntry.TABLE_NAME)
    }

    fun deleteData(key: String) {
        delete(DataTableEntry.TABLE_NAME, DataTableEntry.COLUMN_NAME_KEY, key)
    }
    fun truncateData(){
        truncate(DataTableEntry.TABLE_NAME)
    }

    fun deleteProject(key: String) {
        delete(ProjectTableEntry.TABLE_NAME, ProjectTableEntry.ID, key)
    }
    fun truncateProjects(){
        truncate(ProjectTableEntry.TABLE_NAME)
    }

    /** INTERNAL **/
    private fun insert(tableName: String, keyColumn: String, key: String, values: ContentValues): Long {
        delete(tableName, keyColumn, key)
        val db = dbHelper.writableDatabase
        val newRowId: Long
        newRowId = db.insert(tableName, "null", values)
        db.close()
        return newRowId
    }

    private fun delete(tableName: String, keyColumn: String, key: String) {
        val db = dbHelper.writableDatabase
        val selection = keyColumn + LIKE_WILDCARD
        db.delete(tableName, selection, arrayOf(key))
        db.close()
    }

    private fun truncate(tableName: String){
        val db = dbHelper.writableDatabase
        db.delete(tableName, null, null)
        db.close()
    }
}