package uzh.scenere.helpers

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import uzh.scenere.const.Constants
import uzh.scenere.const.Constants.Companion.ATTRIBUTE_UID_IDENTIFIER
import uzh.scenere.const.Constants.Companion.ELEMENT_UID_IDENTIFIER
import uzh.scenere.const.Constants.Companion.OBJECT_UID_IDENTIFIER
import uzh.scenere.const.Constants.Companion.PATH_UID_IDENTIFIER
import uzh.scenere.const.Constants.Companion.PROJECT_UID_IDENTIFIER
import uzh.scenere.const.Constants.Companion.SCENARIO_UID_IDENTIFIER
import uzh.scenere.const.Constants.Companion.STAKEHOLDER_UID_IDENTIFIER
import uzh.scenere.datamodel.*
import uzh.scenere.datamodel.database.SreDatabase
import uzh.scenere.views.Element
import java.io.Serializable
import kotlin.reflect.KClass

class DatabaseHelper private constructor(context: Context) {
    enum class DataMode {
        PREFERENCES, DATABASE
    }

    private var mode: DataMode = DataMode.PREFERENCES
    private var database: SreDatabase? = null
    private var sharedPreferences: SharedPreferences = context.getSharedPreferences(Constants.SHARED_PREFERENCES, Context.MODE_PRIVATE)

    init {
        if (PermissionHelper.check(context, PermissionHelper.Companion.PermissionGroups.STORAGE)) {
            database = SreDatabase.getInstance(context)
            mode = DataMode.DATABASE
        } else {
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

    public fun write(key: String, obj: Any, internalMode: DataMode = mode): Boolean {
        when (internalMode) {
            DataMode.PREFERENCES -> {
                if (obj is Boolean) sharedPreferences.edit().putBoolean(key, obj).apply()
                if (obj is String) sharedPreferences.edit().putString(key, obj).apply()
                if (obj is ByteArray) sharedPreferences.edit().putString(key, Base64.encodeToString(obj, Base64.DEFAULT)).apply()
                if (obj is Short) sharedPreferences.edit().putInt(key, obj.toInt()).apply()
                if (obj is Int) sharedPreferences.edit().putInt(key, obj).apply()
                if (obj is Long) sharedPreferences.edit().putLong(key, obj).apply()
                if (obj is Float) sharedPreferences.edit().putFloat(key, obj).apply()
                if (obj is Double) sharedPreferences.edit().putLong(key, java.lang.Double.doubleToLongBits(obj)).apply()
                if (obj is Project) write(PROJECT_UID_IDENTIFIER + key, DataHelper.toByteArray(obj))
                if (obj is Stakeholder) write(STAKEHOLDER_UID_IDENTIFIER + key, DataHelper.toByteArray(obj))
                if (obj is Object) write(OBJECT_UID_IDENTIFIER + key, DataHelper.toByteArray(obj))
                if (obj is Attribute) write(ATTRIBUTE_UID_IDENTIFIER + key, DataHelper.toByteArray(obj))
                if (obj is Scenario) write(SCENARIO_UID_IDENTIFIER + key, DataHelper.toByteArray(obj))
                if (obj is Path) write(PATH_UID_IDENTIFIER + key, DataHelper.toByteArray(obj))
                if (obj is IElement) write(ELEMENT_UID_IDENTIFIER + key, DataHelper.toByteArray(obj))
            }
            DataMode.DATABASE -> {
                if (obj is Boolean) database!!.writeBoolean(key, obj)
                if (obj is String) database!!.writeString(key, obj)
                if (obj is ByteArray) database!!.writeByteArray(key, obj)
                if (obj is Short) database!!.writeShort(key, obj)
                if (obj is Int) database!!.writeInt(key, obj)
                if (obj is Long) database!!.writeLong(key, obj)
                if (obj is Float) database!!.writeFloat(key, obj)
                if (obj is Double) database!!.writeDouble(key, obj)
                if (obj is Project) database!!.writeProject(obj)
                if (obj is Stakeholder) database!!.writeStakeholder(obj)
                if (obj is Object) database!!.writeObject(obj)
                if (obj is Attribute) database!!.writeAttribute(obj)
                if (obj is Scenario) database!!.writeScenario(obj)
                if (obj is IElement) database!!.writeElement(obj)
                if (obj is Path) database!!.writePath(obj)
            }
        }
        return true
    }

    @Suppress("UNCHECKED_CAST")
    public fun <T : Serializable> read(key: String, clz: KClass<T>, valIfNull: T, internalMode: DataMode = mode): T {
        try {
            when (internalMode) {
                DataMode.PREFERENCES -> {
                    if (String::class == clz) return sharedPreferences.getString(key, valIfNull as String) as T
                    if (ByteArray::class == clz) {
                        val byteArrayString: String = sharedPreferences.getString(key, "")
                        if (StringHelper.hasText(byteArrayString)) {
                            return Base64.decode(byteArrayString, Base64.DEFAULT) as T
                        }
                        return valIfNull
                    }
                    if (Boolean::class == clz) return sharedPreferences.getBoolean(key, valIfNull as Boolean) as T
                    if (Short::class == clz) {
                        val intValue = sharedPreferences.getInt(key, 0)
                        return if (intValue == 0) valIfNull else intValue.toShort() as T
                    }
                    if (Int::class == clz) return sharedPreferences.getInt(key, valIfNull as Int) as T
                    if (Long::class == clz) return sharedPreferences.getLong(key, valIfNull as Long) as T
                    if (Float::class == clz) return sharedPreferences.getFloat(key, valIfNull as Float) as T
                    if (Double::class == clz) {
                        val rawLongBits: Long = sharedPreferences.getLong(key, 0L)
                        return if (rawLongBits == 0L) valIfNull else java.lang.Double.longBitsToDouble(rawLongBits) as T
                    }
                    if (Project::class == clz) return readBinary(key, clz, PROJECT_UID_IDENTIFIER, valIfNull)
                    if (Stakeholder::class == clz) return readBinary(key, clz, STAKEHOLDER_UID_IDENTIFIER, valIfNull)
                    if (Object::class == clz) return readBinary(key, clz, OBJECT_UID_IDENTIFIER, valIfNull)
                    if (Attribute::class == clz) return readBinary(key, clz, ATTRIBUTE_UID_IDENTIFIER, valIfNull)
                    if (Scenario::class == clz) return readBinary(key, clz, SCENARIO_UID_IDENTIFIER, valIfNull)
                    if (Path::class == clz) return readBinary(key, clz, PATH_UID_IDENTIFIER, NullHelper.get(clz))
                    if (IElement::class == clz) return readBinary(key, clz, ELEMENT_UID_IDENTIFIER, NullHelper.get(clz))
                }
                DataMode.DATABASE -> {
                    if (Boolean::class == clz) return database!!.readBoolean(key, valIfNull as Boolean) as T
                    if (String::class == clz) return database!!.readString(key, valIfNull as String) as T
                    if (ByteArray::class == clz) return database!!.readByteArray(key, valIfNull as ByteArray) as T
                    if (Short::class == clz) return database!!.readShort(key, valIfNull as Short) as T
                    if (Int::class == clz) return database!!.readInt(key, valIfNull as Int) as T
                    if (Long::class == clz) return database!!.readLong(key, valIfNull as Long) as T
                    if (Float::class == clz) return database!!.readFloat(key, valIfNull as Float) as T
                    if (Double::class == clz) return database!!.readDouble(key, valIfNull as Double) as T
                    if (Project::class == clz) return database!!.readProject(key, valIfNull as Project) as T
                    if (Stakeholder::class == clz) return database!!.readStakeholders(key, valIfNull as Stakeholder) as T
                    if (Object::class == clz) return database!!.readObject(key, valIfNull as Object) as T
                    if (Attribute::class == clz) return database!!.readAttribute(key, valIfNull as Attribute) as T
                    if (Scenario::class == clz) return database!!.readScenario(key, valIfNull as Scenario) as T
                    if (Path::class == clz) return database!!.readPath(key, valIfNull as Path) as T
                    if (IElement::class == clz) return valIfNull
                }
            }
        } catch (e: Exception) {
        }
        return valIfNull
    }

    private fun <T : Serializable> readBinary(key: String, clz: KClass<T>, identifier: String, valIfNull: T): T {
        val bytes = read(identifier + key, ByteArray::class, ByteArray(0))
        if (bytes.isNotEmpty()) {
            val project = DataHelper.toObject(bytes, clz)
            return project ?: valIfNull
        }
        return valIfNull
    }

    @Suppress("UNCHECKED_CAST")
    public fun <T : Serializable> readFull(key: String, clz: KClass<T>, internalMode: DataMode = mode): T? {
        when (internalMode) {
            DataMode.PREFERENCES -> {
                if (Project::class == clz) return readBinary(key, clz, PROJECT_UID_IDENTIFIER, NullHelper.get(clz))
                if (Object::class == clz) return readBinary(key, clz, OBJECT_UID_IDENTIFIER, NullHelper.get(clz))
                if (Scenario::class == clz) return readBinary(key, clz, SCENARIO_UID_IDENTIFIER, NullHelper.get(clz))
                if (Path::class == clz) return readBinary(key, clz, PATH_UID_IDENTIFIER, NullHelper.get(clz))
                if (IElement::class == clz) return readBinary(key, clz, ELEMENT_UID_IDENTIFIER, NullHelper.get(clz))
            }
            DataMode.DATABASE -> {
                if (Project::class == clz) return database!!.readProject(key, NullHelper.get(Project::class), true) as T?
                if (Object::class == clz) return database!!.readObject(key, NullHelper.get(Object::class), true) as T?
                if (Scenario::class == clz) return database!!.readScenario(key, NullHelper.get(Scenario::class), true) as T?
                if (Path::class == clz) return database!!.readPath(key, NullHelper.get(Path::class),true) as T?
                if (IElement::class == clz) return null
            }
        }
        return null
    }

    public fun <T : Serializable> readAndMigrate(key: String, clz: KClass<T>, valIfNull: T, deleteInPrefs: Boolean = true): T? {
        when (mode) {
            DataMode.PREFERENCES -> {
                return read(key, clz, valIfNull)
            }
            DataMode.DATABASE -> {
                val readPref = read(key, clz, valIfNull, DataMode.PREFERENCES)
                val readDb = read(key, clz, valIfNull, DataMode.DATABASE)
                if (readPref == valIfNull) {
                    //Val was not saved, return DB value
                    return readDb
                }
                //Delete from Prefs if not specified otherwise
                if (deleteInPrefs) {
                    delete(key, clz, DataMode.PREFERENCES)
                }
                //Value was saved, check DB value
                if (readDb == valIfNull) {
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
    public fun <T : Serializable> readBulk(clz: KClass<T>, key: Any?, fullLoad: Boolean = false, internalMode: DataMode = mode): List<T> {
        when (internalMode) {
            DataMode.PREFERENCES -> {
                if (Project::class == clz) return readBulkInternal(clz, PROJECT_UID_IDENTIFIER)
                if (Stakeholder::class == clz) {
                    val stakeholders = readBulkInternal(clz, STAKEHOLDER_UID_IDENTIFIER)
                    val list = ArrayList<Serializable>()
                    for (stakeholder in stakeholders) {
                        if ((stakeholder as Stakeholder).projectId == ((key as Project).id)) {
                            list.add(stakeholder)
                        }
                    }
                    return list as List<T>
                }
                if (Object::class == clz) {
                    val objects = readBulkInternal(clz, OBJECT_UID_IDENTIFIER)
                    val list = ArrayList<Serializable>()
                    for (obj in objects) {
                        if ((obj as Object).scenarioId == ((key as Scenario).id)) {
                            list.add(obj)
                        }
                    }
                    return list as List<T>
                }
                if (Attribute::class == clz) {
                    val attributes = readBulkInternal(clz, ATTRIBUTE_UID_IDENTIFIER)
                    val list = ArrayList<Serializable>()
                    for (attribute in attributes) {
                        if ((attribute as Attribute).refId == (key as String)) {
                            list.add(attribute)
                        }
                    }
                    return list as List<T>
                }
                if (Scenario::class == clz) {
                    val scenarios = readBulkInternal(clz, SCENARIO_UID_IDENTIFIER)
                    val list = ArrayList<Serializable>()
                    for (scenario in scenarios) {
                        if ((scenario as Scenario).projectId == ((key as Project).id)) {
                            list.add(scenario)
                        }
                    }
                    return list as List<T>
                }
                if (Path::class == clz) return emptyList()
                if (IElement::class == clz) return emptyList()
            }
            DataMode.DATABASE -> {
                if (Project::class == clz) return database!!.readProjects() as List<T>
                if (Stakeholder::class == clz && key is Project) return database!!.readStakeholders(key) as List<T>
                if (Object::class == clz && key is Scenario) return database!!.readObjects(key, fullLoad) as List<T>
                if (Attribute::class == clz && key is String) return database!!.readAttributes(key) as List<T>
                if (Scenario::class == clz && key is Project) return database!!.readScenarios(key, fullLoad) as List<T>
                if (Path::class == clz && key is Scenario) return database!!.readPaths(key, fullLoad) as List<T>
                if (IElement::class == clz && key is Path) return database!!.readElements(key, fullLoad) as List<T>
            }
        }
        return emptyList()
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Serializable> readBulkInternal(clz: KClass<T>, identifier: String): List<T> {
        val list = ArrayList<Serializable>()
        for (entry in sharedPreferences.all) {
            if (entry.key.contains(identifier)) {
                val bytes = Base64.decode((entry.value as String), Base64.DEFAULT)
                val element = DataHelper.toObject(bytes, clz)
                if (element != null) {
                    list.add(element)
                }
            }
        }
        return list as List<T>
    }

    public fun <T : Serializable> delete(key: String, clz: KClass<T>, internalMode: DataMode = mode) {
        when (internalMode) {
            DataMode.PREFERENCES -> {
                sharedPreferences.edit().remove(key).apply()
                sharedPreferences.edit().remove(PROJECT_UID_IDENTIFIER + key).apply()
            }
            DataMode.DATABASE -> {
                if (Project::class == clz) {
                    val project = readFull(key, Project::class)
                    database!!.deleteProject(key)
                    if (project != null) {
                        for (stakeholder in project.stakeholders) {
                            delete(stakeholder.id, Stakeholder::class)
                        }
                        for (scenario in project.scenarios) {
                            delete(scenario.id, Scenario::class)
                        }
                    }
                }
                if (Stakeholder::class == clz) {
                    database!!.deleteStakeholder(key)
                }
                if (Object::class == clz) {
                    val obj = readFull(key, Object::class)
                    database!!.deleteObject(key)
                    if (obj != null) {
                        for (attribute in obj.attributes) {
                            delete(attribute.id, Attribute::class)
                        }
                    }
                }
                if (Attribute::class == clz) {
                    database!!.deleteAttribute(key)
                }
                if (Scenario::class == clz) {
                    val scenario = readFull(key, Scenario::class)
                    database!!.deleteScenario(key)
                    if (scenario != null) {
                        for (obj in scenario.objects) {
                            delete(obj.id, Object::class)
                        }
                        for (path in scenario.getAllPaths()) {
                            delete(path.id, Path::class)
                        }
                    }
                }
                if (Path::class == clz) {
                    val path = readFull(key, Path::class)
                    database!!.deletePath(key)
                    if (path != null) {
                        for (element in path.elements) {
                            delete(element.value.getElementId(), IElement::class)
                        }
                    }
                }
                if (IElement::class == clz) {
                    database!!.deleteElement(key)
                }
                if (CollectionsHelper.oneOf(clz, Boolean::class, Short::class, Int::class, Long::class, Float::class, Double::class)) {
                    database!!.deleteNumber(key)
                }
                if (String::class == clz) {
                    database!!.deleteString(key)
                }
                if (ByteArray::class == clz) {
                    database!!.deleteData(key)
                }
            }
        }
    }

    fun clear() {
        when (mode) {
            DataMode.PREFERENCES -> {
                sharedPreferences.edit().clear().apply()
            }
            DataMode.DATABASE -> {
                database!!.truncateData()
                database!!.truncateNumbers()
                database!!.truncateStrings()
                database!!.truncateProjects()
                database!!.truncateStakeholders()
                database!!.truncateObjects()
                database!!.truncateAttributes()
                database!!.truncateScenarios()
            }
        }
    }

    fun <T : Serializable> dropAndRecreate(kClass: KClass<T>) {
        if (kClass == Attribute::class) {
            database!!.dropAndRecreateTable("ATTRIBUTE_TABLE")
        }
        if (kClass == Path::class) {
            database!!.dropAndRecreateTable("PATH_TABLE")
        }
        if (kClass == Element::class) {
            database!!.dropAndRecreateTable("ELEMENT_TABLE")
        }
    }
}