package uzh.scenere.datamodel

import android.content.Context
import uzh.scenere.R
import uzh.scenere.const.Constants.Companion.NEW_LINE
import uzh.scenere.datamodel.Walkthrough.WalkthroughProperty.*
import uzh.scenere.datamodel.Walkthrough.WalkthroughStepProperty.*
import uzh.scenere.datamodel.steps.AbstractStep
import uzh.scenere.helpers.DataHelper
import uzh.scenere.helpers.NullHelper
import uzh.scenere.helpers.StringHelper
import uzh.scenere.helpers.className
import java.io.Serializable
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.reflect.KClass

open class Walkthrough private constructor(val id: String, val owner: String, val scenarioId: String, val stakeholderId: String): Serializable {
    private val localPropertiesMap = HashMap<WalkthroughProperty, Any>()
    private val localStepPropertiesMap = HashMap<String, HashMap<WalkthroughStepProperty, Any>>()
    private var xmlRepresentation: String? = null

    init {
        clearWalkthrough()
        WT_ID.set(id, String::class)
        WT_OWNER.set(owner, String::class)
        SCENARIO_ID.set(scenarioId, String::class)
        STAKEHOLDER_ID.set(stakeholderId, String::class)
    }

    fun copy() {
        localPropertiesMap.clear()
        localPropertiesMap.putAll(propertiesMap)
        localStepPropertiesMap.clear()
        localStepPropertiesMap.putAll(stepPropertiesMap)
    }

    fun load() {
        propertiesMap.clear()
        propertiesMap.putAll(localPropertiesMap)
        stepPropertiesMap.clear()
        stepPropertiesMap.putAll(localStepPropertiesMap)
    }

    private fun clearWalkthrough() {
        propertiesMap.clear()
        stepPropertiesMap.clear()
    }


    //Data can be collected globally due to a single entry point
    @Suppress("UNCHECKED_CAST")
    enum class WalkthroughProperty(val type: KClass<out Serializable>, private val valueIfNull: Any, val multivalued: Boolean = false) {
        WT_ID(String::class, ""),
        WT_OWNER(String::class, ""),
        SCENARIO_ID(String::class, ""),
        STAKEHOLDER_ID(String::class, ""),
        INTRO_TIME(Long::class, 0),
        INFO_TIME(Long::class, 0),
        INFO_OBJECT(String::class, 0, true),
        INFO_ATTRIBUTE(String::class, 0, true),
        STEP_ID_LIST(String::class, "", true);

        fun getPropertiesMap(map: HashMap<WalkthroughProperty, Any>?): HashMap<WalkthroughProperty, Any> {
            return map ?: propertiesMap
        }

        fun <T : Serializable> get(clazz: KClass<T>, map: HashMap<WalkthroughProperty, Any>? = null): T {
            if (clazz != type) {
                return NullHelper.get(clazz)
            }
            if (this.multivalued && getPropertiesMap(map)[this] != null) {
                val m = (getPropertiesMap(map)[this] as HashMap<T, Int>)
                for (entry in m.entries) {
                    return entry.key
                }
                return valueIfNull as T
            }
            return (getPropertiesMap(map)[this] ?: valueIfNull) as T
        }

        fun <T : Serializable> getAll(clazz: KClass<T>, map: HashMap<WalkthroughProperty, Any>? = null): List<T> {
            if (this.multivalued) {
                val list = ArrayList<T>()
                if (getPropertiesMap(map)[this] == null) {
                    return list
                }
                val m = (getPropertiesMap(map)[this] as HashMap<T, Int>)
                for (entry in m.entries) {
                    list.add(entry.key)
                }
                return list
            }
            return listOf(get(clazz))
        }

        fun <T : Serializable> getAllCount(clazz: KClass<T>, map: HashMap<WalkthroughProperty, Any>? = null): HashMap<T, Int> {
            if (this.multivalued) {
                return if (getPropertiesMap(map)[this] == null) HashMap<T, Int>() else getPropertiesMap(map)[this] as HashMap<T, Int>
            }
            return hashMapOf(get(clazz) to 1)
        }

        fun <T : Any> set(property: T?, type: KClass<T> = this.type as KClass<T>) {
            if (property == null) {
                return
            }
            if (property::class != type) {
                var found = false
                for (clz in property::class.supertypes) {
                    if (type.toString().contains(clz.toString())) {
                        found = true
                    }
                }
                if (!found) {
                    return
                }
            }
            if (this.multivalued) {
                val entry = propertiesMap[this]
                if (entry != null) {
                    val count = (propertiesMap[this] as HashMap<T, Int>)[property]
                    (propertiesMap[this] as HashMap<T, Int>)[property] = (if (count == null) 1 else (count + 1))
                } else {
                    propertiesMap[this] = hashMapOf(property to 1)
                }
            } else {
                propertiesMap[this] = property
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    enum class WalkthroughStepProperty(val type: KClass<out Serializable>, private val valueIfNull: Any, val multivalued: Boolean = false) {
        STEP_ID(String::class, ""),
        STEP_TIME(Long::class, 0),
        STEP_NUMBER(Int::class, 0),
        STEP_TITLE(String::class, ""),
        STEP_TYPE(String::class, "");

        fun getStepPropertiesMap(map: HashMap<String, HashMap<WalkthroughStepProperty, Any>>?): HashMap<String, HashMap<WalkthroughStepProperty, Any>> {
            return map ?: stepPropertiesMap
        }

        fun <T : Serializable> get(stepId: String, clazz: KClass<T>, map: HashMap<String, HashMap<WalkthroughStepProperty, Any>>? = null): T {
            if (clazz != type) {
                return NullHelper.get(clazz)
            }
            if (this.multivalued && getStepPropertiesMap(map)[stepId]?.get(this) != null) {
                val m = (getStepPropertiesMap(map)[stepId]?.get(this) as HashMap<T, Int>)
                for (entry in m.entries) {
                    return entry.key
                }
                return valueIfNull as T
            }
            return (getStepPropertiesMap(map)[stepId]!![this] ?: valueIfNull) as T
        }

        fun <T : Serializable> getAll(stepId: String, clazz: KClass<T>, map: HashMap<String, HashMap<WalkthroughStepProperty, Any>>? = null): List<T> {
            if (this.multivalued) {
                val list = ArrayList<T>()
                if (getStepPropertiesMap(map)[stepId]?.get(this) == null) {
                    return list
                }
                val m = (getStepPropertiesMap(map)[stepId]!![this] as HashMap<T, Int>)
                for (entry in m.entries) {
                    list.add(entry.key)
                }
                return list
            }
            return listOf(get(stepId, clazz))
        }


        fun <T : Serializable> getAllCount(stepId: String, clazz: KClass<T>, map: HashMap<String, HashMap<WalkthroughStepProperty, Any>>? = null): HashMap<T, Int> {
            if (this.multivalued) {
                return if (getStepPropertiesMap(map)[stepId]?.get(this) == null) HashMap<T, Int>() else getStepPropertiesMap(map)[stepId]?.get(this) as HashMap<T, Int>
            }
            return hashMapOf(get(stepId, clazz) to 1)
        }

        fun <T : Any> set(stepId: String, property: T?, type: KClass<T> = this.type as KClass<T>) {
            if (property == null) {
                return
            }
            if (property::class != type) {
                var found = false
                for (clz in property::class.supertypes) {
                    if (type.toString().contains(clz.toString())) {
                        found = true
                    }
                }
                if (!found) {
                    return
                }
            }
            if (stepPropertiesMap[stepId] == null) {
                stepPropertiesMap[stepId] = HashMap()
            }
            if (this.multivalued) {
                if (stepPropertiesMap[stepId]?.get(this) != null) {
                    val m = stepPropertiesMap[stepId]!![this] as HashMap<T, Int>
                    val count = m[property]
                    (stepPropertiesMap[stepId]!![this] as HashMap<T, Int>)[property] = if (count == null) 1 else (count + 1)
                } else {
                    stepPropertiesMap[stepId]!![this] = hashMapOf(property to 1)
                }
            } else {
                stepPropertiesMap[stepId]!![this] = property
            }
        }
    }

    companion object {
        private val propertiesMap = HashMap<WalkthroughProperty, Any>()
        private val stepPropertiesMap = HashMap<String, HashMap<WalkthroughStepProperty, Any>>()
    }

    fun addStep(step: AbstractStep?) {
        if (step != null) {
            STEP_ID_LIST.set(step.id)
            STEP_ID.set(step.id, step.id)
            STEP_TIME.set(step.id, step.time)
            STEP_NUMBER.set(step.id, STEP_ID_LIST.getAll(String::class).size)
            STEP_TITLE.set(step.id, step.title)
            STEP_TYPE.set(step.id, step.className())
        }
    }

    fun printStatistics(): String {
        var total = INTRO_TIME.get(Long::class)
        for (stepId in STEP_ID_LIST.getAll(String::class)) {
            total += STEP_TIME.get(stepId, Long::class)
        }
        total /= 1000
        var text = "\nStatistics:\nYour last Walkthrough took $total seconds to complete!\n"
        for (stepId in STEP_ID_LIST.getAll(String::class)) {
            text += "You spent " + STEP_TIME.get(stepId, Long::class) / 1000 + " seconds in \"" + STEP_TITLE.get(stepId, String::class) + "\",\n"
        }
        text = text.substring(0, text.length - 2).plus(".\n")
        text += "Additionally, you spent " + INTRO_TIME.get(Long::class) / 1000 + " seconds in the Introduction and\n"
        text += "" + INFO_TIME.get(Long::class) / 1000 + " seconds browsing the Information."
        return text
    }

    class WalkthroughBuilder(private val owner: String, private val scenarioId: String, private val stakeholderId: String) {

        constructor(id: String, owner: String, scenarioId: String, stakeholderId: String) : this(owner, scenarioId, stakeholderId) {
            this.id = id
        }

        private var id: String? = null
        private var xmlRepresentation: String? = null

        fun withXml(xml: String): WalkthroughBuilder {
            this.xmlRepresentation = xml
            return this
        }

        fun build(): Walkthrough {
            val walkthrough = Walkthrough(id
                    ?: UUID.randomUUID().toString(), owner, scenarioId, stakeholderId)
            if (this.xmlRepresentation != null) {
                walkthrough.xmlRepresentation = this.xmlRepresentation
                walkthrough.fromXml()
            }
            return walkthrough
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun toXml(context: Context) {
        var xml = context.resources.getString(R.string.xml_declaration).plus(NEW_LINE)
        for (property in WalkthroughProperty.values()) {
            if (property.multivalued) {
                var counter = 0
                for (entry in property.getAllCount(property.type)) {
                    xml += context.resources.getString(R.string.xml_enclosing, property.toString().plus(counter), entry.key.className(), entry.key).plus(NEW_LINE)
                    counter++
                }
            } else {
                xml += context.resources.getString(R.string.xml_enclosing, property.toString(), property.get(property.type).className(), property.get(property.type)).plus(NEW_LINE)
            }
        }
        for (stepId in STEP_ID_LIST.getAll(STEP_ID_LIST.type) as List<String>) {
            xml += context.resources.getString(R.string.xml_begin_tag_id, STEP_TYPE.get(stepId, String::class), stepId).plus(NEW_LINE)
            for (property in WalkthroughStepProperty.values()) {
                if (property.multivalued) {
                    var counter = 0
                    for (entry in property.getAllCount(stepId, property.type)) {
                        xml += context.resources.getString(R.string.xml_enclosing, property.toString().plus(counter), entry.className(), entry).plus(NEW_LINE)
                        counter++
                    }
                } else {
                    xml += context.resources.getString(R.string.xml_enclosing, property.toString(), property.get(stepId, property.type).className(), property.get(stepId, property.type)).plus(NEW_LINE)
                }
            }
            xml += context.resources.getString(R.string.xml_end_tag, STEP_TYPE.get(stepId, String::class)).plus(NEW_LINE)
        }
        xmlRepresentation = xml
    }

    fun fromXml() {
        val lines = xmlRepresentation?.split(NEW_LINE)
        if (xmlRepresentation == null || lines == null) {
            return
        }
        clearWalkthrough()
        var stepId = ""
        for (line in lines) {
            if (line.startsWith("<?")) {
                continue;
            } else if (line.contains(" type=")) {
                val enumString = line.substring(1, line.indexOf(" type="))
                if (StringHelper.hasText(stepId)) {
                    var enum: WalkthroughStepProperty? = null
                    try {
                        enum = WalkthroughStepProperty.valueOf(enumString)
                    } catch (e: Exception) {
                    }
                    if (enum != null) {
                        val value = line.replace("<(.*?)>".toRegex(), "")
                        val type = line.replace("<(.*?)type=\"".toRegex(), "").replace("\">(.*?)>".toRegex(), "")
                        enum.set(stepId, DataHelper.parseString(value, type))
                    }
                } else {
                    var enum: WalkthroughProperty? = null
                    try {
                        enum = WalkthroughProperty.valueOf(enumString)
                    } catch (e: Exception) {
                    }
                    if (enum != null) {
                        val value = line.replace("<(.*?)>".toRegex(), "")
                        val type = line.replace("<(.*?)type=\"".toRegex(), "").replace("\">(.*?)>".toRegex(), "")
                        enum.set(DataHelper.parseString(value, type))
                    }
                }
            } else if (line.contains(" id=")) { //Step Begin
                stepId = line.substring(line.indexOf(" id=") + " id=".length + 1, line.length - 2)
            } else if (line.startsWith("</ ")) { //Step End
                stepId = ""
            }
        }
        copy()
        clearWalkthrough()
    }

    fun getXml(): String? {
        return xmlRepresentation
    }

    class NullWalkthrough() : Walkthrough("", "", "", "") {}
}