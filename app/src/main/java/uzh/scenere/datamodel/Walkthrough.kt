package uzh.scenere.datamodel

import android.annotation.SuppressLint
import android.content.Context
import uzh.scenere.R
import uzh.scenere.const.Constants.Companion.NEW_LINE_C
import uzh.scenere.const.Constants.Companion.NO_DATA
import uzh.scenere.datamodel.Walkthrough.WalkthroughProperty.*
import uzh.scenere.datamodel.Walkthrough.WalkthroughStepProperty.*
import uzh.scenere.datamodel.steps.AbstractStep
import uzh.scenere.datamodel.trigger.AbstractTrigger
import uzh.scenere.helpers.DataHelper
import uzh.scenere.helpers.NullHelper
import uzh.scenere.helpers.StringHelper
import uzh.scenere.helpers.className
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.reflect.KClass


open class Walkthrough private constructor(val id: String, val owner: String, val scenarioId: String, val stakeholderId: String) : Serializable, IVersionItem {
    override var changeTimeMs: Long = 0

    private val localPropertiesMap = HashMap<WalkthroughProperty, Any>()
    private val localStepPropertiesMap = HashMap<String, HashMap<WalkthroughStepProperty, Any>>()
    private var xmlRepresentation: String? = null

    init {
        clearWalkthrough()
        TIMESTAMP.set(System.currentTimeMillis(), Long::class)
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
    enum class WalkthroughProperty(val label: String, val type: KClass<out Serializable>, private val valueIfNull: Any, val isStatisticalValue: Boolean, val multivalued: Boolean = false) {
        WT_ID("Walkthrough-ID", String::class, "", false),
        WT_OWNER("User", String::class, "", true),
        SCENARIO_ID("Scenario-ID", String::class, "", false),
        STAKEHOLDER_ID("Stakeholder-ID", String::class, "", false),
        INTRO_TIME("Intro Time", Long::class, 0L, true),
        COMPLETION_TIME("Completion Time", Long::class, 0L, true),
        TIMESTAMP("Timestamp", Long::class, 0L, true),
        INFO_TIME("Info Time", Long::class, 0L, true),
        WHAT_IF_TIME("What-If Time", Long::class, 0L, true),
        INPUT_TIME("Input Time", Long::class, 0L, true),
        INFO_OBJECT("Info Object(s)", String::class, "", true, true),
        INFO_ATTRIBUTE("Info Attribute(s)", String::class, "", true, true),
        STEP_ID_LIST("Step ID(s)", String::class, "", false, true);

        @SuppressLint("SimpleDateFormat")
        fun getDisplayText(): String {
            val value = get(type)
            when (this) {
                INFO_TIME, INTRO_TIME, INPUT_TIME, WHAT_IF_TIME, COMPLETION_TIME -> {
                    return ((value as Long) ).toString() + " Second(s)"
                }
                TIMESTAMP -> {
                    if (value != valueIfNull) {
                        val currentDate = Date(value as Long)
                        val date = SimpleDateFormat("dd.MM.yyyy")
                        val time = SimpleDateFormat("HH:mm:ss")
                        val d = date.format(currentDate)
                        val t = time.format(currentDate)
                        return "Date: $d\nTime: $t"
                    }
                }
                INFO_OBJECT, INFO_ATTRIBUTE -> {
                    if (value != valueIfNull) {
                        return StringHelper.concatTokens(", ", getAll(type))
                    }
                }
                else -> return value.toString()
            }
            return NO_DATA
        }

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
    enum class WalkthroughStepProperty(val label: String, val type: KClass<out Serializable>, private val valueIfNull: Any, val isStatisticalValue: Boolean, val multivalued: Boolean = false) {
        STEP_ID("Step-ID", String::class, "", false),
        STEP_TIME("Step Time", Long::class, 0L, true),
        STEP_NUMBER("Step Number", Int::class, 0, true),
        STEP_TEXT("Step Text", String::class, "", true),
        STEP_TITLE("Step Title", String::class, "", true),
        STEP_TYPE("Step Type", String::class, "", true),
        STEP_COMMENTS("Step Comments", String::class, "", true,true),
        TRIGGER_INFO("Trigger Info", String::class, "", true);

        @SuppressLint("SimpleDateFormat")
        fun getDisplayText(stepId: String): String {
            val value = get(stepId, type)
            when (this) {
                STEP_TIME -> {
                    if (value != valueIfNull) {
                        return ((value as Long) ).toString() + " Second(s)"
                    }
                }
                else -> return value.toString()
            }
            return NO_DATA
        }

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
            STEP_TEXT.set(step.id, step.text)
            STEP_TYPE.set(step.id, step.className())
            for (comment in step.comments){
                STEP_COMMENTS.set(step.id, comment)
            }
        }
    }

    fun addTriggerInfo(step: AbstractStep?, info: String, trigger: AbstractTrigger? = null){
        if (step != null) {
            TRIGGER_INFO.set(step.id,info)
        }
    }

    fun printStatistics(): String {
        var total = INTRO_TIME.get(Long::class)
        for (stepId in STEP_ID_LIST.getAll(String::class)) {
            total += STEP_TIME.get(stepId, Long::class)
        }
        var text = "<br><b>Statistics</b><br>Your last Walkthrough took <b>$total</b> seconds to complete!<br>You spent "
        for (stepId in STEP_ID_LIST.getAll(String::class)) {
            text += "<b>" + STEP_TIME.get(stepId, Long::class) + "</b> seconds in \"" + STEP_TITLE.get(stepId, String::class) + "\", <br>"
        }
        text = text.substring(0, text.length - ", <br>".length).plus(".<br>")
        text += "Additionally, you read the Introduction for <b>" + INTRO_TIME.get(Long::class)  + "</b> seconds and<br>"
        text += "browsed the Information and What-Ifs for an additional <b>" + INFO_TIME.get(Long::class)  + "</b> and " + WHAT_IF_TIME.get(Long::class)  + " seconds.<br>" +
                "Consulting the Comment Panel took you  <b>"+INPUT_TIME.get(Long::class) +"</b> seconds."
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

    private fun calculateCompletionTime() {
        var total = INTRO_TIME.get(Long::class)
        for (stepId in STEP_ID_LIST.getAll(String::class)) {
            total += STEP_TIME.get(stepId, Long::class)
        }
        COMPLETION_TIME.set(total)
    }

    @Suppress("UNCHECKED_CAST")
    fun toXml(context: Context) {
        calculateCompletionTime()
        var xml = context.resources.getString(R.string.xml_declaration).plus(NEW_LINE_C)
        for (property in WalkthroughProperty.values()) {
            if (property.multivalued) {
                var counter = 0
                for (entry in property.getAllCount(property.type)) {
                    xml += context.resources.getString(R.string.xml_enclosing, property.toString().plus(counter), entry.key.className(), entry.key).plus(NEW_LINE_C)
                    counter++
                }
            } else {
                xml += context.resources.getString(R.string.xml_enclosing, property.toString(), property.get(property.type).className(), property.get(property.type)).plus(NEW_LINE_C)
            }
        }
        for (stepId in STEP_ID_LIST.getAll(STEP_ID_LIST.type) as List<String>) {
            xml += context.resources.getString(R.string.xml_begin_tag_id, STEP_TYPE.get(stepId, String::class), stepId).plus(NEW_LINE_C)
            for (property in WalkthroughStepProperty.values()) {
                if (property.multivalued) {
                    var counter = 0
                    for (entry in property.getAllCount(stepId, property.type)) {
                        xml += context.resources.getString(R.string.xml_enclosing, property.toString().plus(counter), entry.className(), entry).plus(NEW_LINE_C)
                        counter++
                    }
                } else {
                    xml += context.resources.getString(R.string.xml_enclosing, property.toString(), property.get(stepId, property.type).className(), property.get(stepId, property.type)).plus(NEW_LINE_C)
                }
            }
            xml += context.resources.getString(R.string.xml_end_tag, STEP_TYPE.get(stepId, String::class)).plus(NEW_LINE_C)
        }
        xmlRepresentation = xml
    }

    fun fromXml() {
        val lines = xmlRepresentation?.split(NEW_LINE_C)
        if (xmlRepresentation == null || lines == null) {
            return
        }
        clearWalkthrough()
        var stepId = ""
        for (line in lines) {
            if (line.startsWith("<?")) {
                continue;
            } else if (line.contains(" type=")) {
                val enumString = line.substring(1, line.indexOf(" type=")).replace("[0-9]+".toRegex(), "")
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
        if (COMPLETION_TIME.get(Long::class) == 0L) {
            calculateCompletionTime()
        }
        copy()
        clearWalkthrough()
    }

    fun getXml(): String? {
        return xmlRepresentation
    }

    class NullWalkthrough() : Walkthrough("", "", "", "") {}
}