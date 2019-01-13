package uzh.scenere.datamodel.database

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import uzh.scenere.const.Constants
import uzh.scenere.const.Constants.Companion.PROJECT_UID_IDENTIFIER
import uzh.scenere.datamodel.Project
import uzh.scenere.helpers.DataHelper
import uzh.scenere.helpers.PermissionHelper
import uzh.scenere.helpers.StringHelper
import kotlin.reflect.KClass

class DatabaseHelper private constructor(context: Context) {
    enum class DataMode{
        PREFERENCES, DATABASE
    }
    private var mode: DataMode = DataMode.PREFERENCES
    private var database: SreDatabase? = null
    private var sharedPreferences: SharedPreferences = context.getSharedPreferences(Constants.SHARED_PREFERENCES, Context.MODE_PRIVATE)

    init {
        if (PermissionHelper.check(context, PermissionHelper.Companion.PermissionGroups.STORAGE)){
            database = SreDatabase.getInstance(context)
            mode = DataMode.DATABASE
        }else{
            mode = DataMode.PREFERENCES
        }
    }

    companion object {
        // Volatile: writes to this field are immediately made visible to other threads.
        @Volatile
        private var instance: DatabaseHelper? = null

        fun getInstance(context: Context): DatabaseHelper {
            return when {
                instance != null -> instance!!
                else -> synchronized(this) {
                    if (instance == null) {
                        instance = DatabaseHelper(context)
                    }
                    instance!!
                }
            }
        }
    }

    public fun write(key: String, obj: Any, internalMode: DataMode = mode){
        when(internalMode){
            DataMode.PREFERENCES -> {
                if (obj is Boolean) sharedPreferences.edit().putBoolean(key,obj).apply()
                if (obj is String)  sharedPreferences.edit().putString(key,obj).apply()
                if (obj is ByteArray)  sharedPreferences.edit().putString(key, Base64.encodeToString(obj, Base64.DEFAULT)).apply()
                if (obj is Short)  sharedPreferences.edit().putInt(key,obj.toInt()).apply()
                if (obj is Int)  sharedPreferences.edit().putInt(key,obj).apply()
                if (obj is Long)  sharedPreferences.edit().putLong(key,obj).apply()
                if (obj is Float)  sharedPreferences.edit().putFloat(key,obj).apply()
                if (obj is Double)  sharedPreferences.edit().putLong(key, java.lang.Double.doubleToLongBits(obj)).apply()
                if (obj is Project)  write(PROJECT_UID_IDENTIFIER+key,DataHelper.toByteArray(obj))
            }
            DataMode.DATABASE -> {
                if (obj is Boolean) database!!.writeBoolean(key,obj)
                if (obj is String)  database!!.writeString(key,obj)
                if (obj is ByteArray)  database!!.writeByteArray(key, obj)
                if (obj is Short)  database!!.writeShort(key,obj)
                if (obj is Int)  database!!.writeInt(key,obj)
                if (obj is Long)  database!!.writeLong(key,obj)
                if (obj is Float)  database!!.writeFloat(key,obj)
                if (obj is Double)  database!!.writeDouble(key,obj)
                if (obj is Project) database!!.writeProject(obj)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    public fun <T : Any> read(key: String, clz: KClass<T>, valIfNull: T, internalMode: DataMode = mode): T? {
        when(internalMode){
            DataMode.PREFERENCES -> {
                if (String::class == clz) return sharedPreferences.getString(key,valIfNull as String) as T?
                if (ByteArray::class == clz){
                    val byteArrayString: String = sharedPreferences.getString(key, "")
                    if (StringHelper.hasText(byteArrayString)){
                        return  Base64.decode(byteArrayString, Base64.DEFAULT) as T?
                    }
                    return valIfNull
                }
                if (Boolean::class == clz) return sharedPreferences.getBoolean(key,valIfNull as Boolean) as T?
                if (Short::class == clz) {
                    val intValue = sharedPreferences.getInt(key, 0)
                    return if (intValue == 0) valIfNull else intValue.toShort() as T?
                }
                if (Int::class == clz) return sharedPreferences.getInt(key,valIfNull as Int) as T?
                if (Long::class == clz) return sharedPreferences.getLong(key,valIfNull as Long) as T?
                if (Float::class == clz) return sharedPreferences.getFloat(key,valIfNull as Float) as T?
                if (Double::class == clz){
                    val rawLongBits: Long = sharedPreferences.getLong(key, 0L)
                    return if (rawLongBits == 0L) valIfNull else java.lang.Double.longBitsToDouble(rawLongBits) as T?
                }
                if (Project::class == clz){
                    val bytes = read(PROJECT_UID_IDENTIFIER+key, ByteArray::class, ByteArray(0))
                    if (bytes != null && bytes.isNotEmpty()){
                        val project = DataHelper.toObject(bytes, Project::class)
                        return project as T ?: valIfNull
                    }
                    return valIfNull
                }
            }
            DataMode.DATABASE -> {
                if (Boolean::class == clz) return database!!.readBoolean(key, valIfNull as Boolean) as T?
                if (String::class == clz) return database!!.readString(key,valIfNull as String) as T?
                if (ByteArray::class == clz) return database!!.readByteArray(key, valIfNull as ByteArray) as T?
                if (Short::class == clz) return database!!.readShort(key,valIfNull as Short) as T?
                if (Int::class == clz) return database!!.readInt(key,valIfNull as Int) as T?
                if (Long::class == clz) return database!!.readLong(key,valIfNull as Long) as T?
                if (Float::class == clz) return database!!.readFloat(key,valIfNull as Float) as T?
                if (Double::class == clz) return database!!.readDouble(key,valIfNull as Double) as T?
                if (Project::class == clz) return database!!.readProject(key,valIfNull as Project) as T?
            }
        }
        return null
    }

    public fun <T : Any> readAndMigrate(key: String, clz: KClass<T>, valIfNull: T, deleteInPrefs: Boolean = true): T? {
        when(mode){
            DataMode.PREFERENCES -> {
                return read(key,clz,valIfNull)
            }
            DataMode.DATABASE -> {
                val readPref = read(key, clz, valIfNull,DataMode.PREFERENCES)
                val readDb = read(key, clz, valIfNull,DataMode.DATABASE)
                if (readPref == null || readPref == valIfNull){
                    //Val was not saved, return DB value
                    return readDb
                }
                //Delete from Prefs if not specified otherwise
                if (deleteInPrefs){
                    delete(key,DataMode.PREFERENCES)
                }
                //Value was saved, check DB value
                if (readDb == null ||readDb == valIfNull){
                    //DB value does'nt exist, write to DB, return
                    write(key, readPref)
                    return readPref
                }
                //DB value did exist, return
                return readDb
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    public fun <T : Any> readBulk(clz: KClass<T>, internalMode: DataMode = mode): List<T> {
        when(internalMode){
            DataMode.PREFERENCES -> {
                if (Project::class == clz){
                    val list = ArrayList<Project>()
                    for (entry in sharedPreferences.all){
                        if (entry.key.contains(PROJECT_UID_IDENTIFIER)){
                            val bytes = Base64.decode((entry.value as String), Base64.DEFAULT)
                            list.add(DataHelper.toObject(bytes, Project::class)!!)
                        }
                    }
                    return list as List<T>
                }
            }
            DataMode.DATABASE -> {
                if (Project::class == clz) return database!!.readProjects() as List<T>
            }
        }
        return emptyList()
    }

    public fun delete(key: String, internalMode: DataMode = mode) {
        when(internalMode){
            DataMode.PREFERENCES -> {
                sharedPreferences.edit().remove(key).apply()
                sharedPreferences.edit().remove(PROJECT_UID_IDENTIFIER+key).apply()
            }
            DataMode.DATABASE -> {
                database!!.deleteData(key)
                database!!.deleteNumber(key)
                database!!.deleteString(key)
                database!!.deleteProject(key)
            }
        }
    }

    fun clear() {
        when(mode){
            DataMode.PREFERENCES -> {
                sharedPreferences.edit().clear().apply()
            }
            DataMode.DATABASE -> {
                database!!.truncateData()
                database!!.truncateNumbers()
                database!!.truncateStrings()
                database!!.truncateProjects()
            }
        }
    }
}