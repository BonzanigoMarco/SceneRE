package uzh.scenere.datamodel

import uzh.scenere.datamodel.steps.AbstractStep
import uzh.scenere.datamodel.Walkthrough.WalkthroughProperty.*
import uzh.scenere.datamodel.Walkthrough.WalkthroughStepProperty.*
import uzh.scenere.helpers.CollectionsHelper
import uzh.scenere.helpers.NullHelper
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.reflect.KClass

class Walkthrough private constructor(val id: String, val owner: String, val scenarioId: String, val stakeholderId: String) {
    init {
        propertiesMap.clear()
        stepPropertiesMap.clear()
        WT_ID.set(id,String::class)
        WT_OWNER.set(owner,String::class)
        SCENARIO_ID.set(scenarioId,String::class)
        STAKEHOLDER_ID.set(stakeholderId,String::class)
    }

    //Data can be collected globaly due to a single entry point
    @Suppress("UNCHECKED_CAST")
    enum class WalkthroughProperty(val type: KClass<out Any>, private val valueIfNull: Any, private val multivalued: Boolean = false) {
        WT_ID(String::class, ""),
        WT_OWNER(String::class, ""),
        SCENARIO_ID(String::class, ""),
        STAKEHOLDER_ID(String::class, ""),
        INTRO_TIME(Long::class, 0),
        INFO_TIME(Long::class, 0),
        INFO_OBJECT(String::class, 0, true),
        STEP_LIST(AbstractStep::class, NullHelper.get(AbstractStep::class), true);

        fun <T: Any> get(clazz: KClass<T>): T {
            if (clazz != type){
                return valueIfNull as T
            }
            if (this.multivalued && propertiesMap[this] != null){
                return (propertiesMap[this] as List<T>)[0]
            }
            return (propertiesMap[this] ?: valueIfNull) as T
        }

        fun <T: Any> getAll(clazz: KClass<T>): List<T> {
            if (this.multivalued){
                return if (propertiesMap[this] == null) Collections.emptyList() else propertiesMap[this] as List<T>
            }
            return listOf(get(clazz))
        }

        fun <T : Any> set(property: T?, type: KClass<T> = this.type as KClass<T>){
            if (property == null) {
                return
            }
            if (property::class != type ){
                var found = false
                for (clz in property::class.supertypes) {
                    if (type.toString().contains(clz.toString())){
                        found = true
                    }
                }
                if (!found){
                    return
                }
            }
            if (this.multivalued){
                if ( propertiesMap[this] != null){
                    propertiesMap[this] = (propertiesMap[this] as List<T>).plus(property)
                }else{
                    propertiesMap[this] = listOf(property)
                }
            }else{
                propertiesMap[this] = property
            }
        }
    }
    
    @Suppress("UNCHECKED_CAST")
    enum class WalkthroughStepProperty(val type: KClass<out Any>, private val valueIfNull: Any, private val multivalued: Boolean = false) {
        STEP_ID(String::class, ""),
        STEP_TIME(Long::class, 0),
        STEP_NUMBER(Long::class, 0),
        STEP_TITLE(String::class, "");

        fun <T: Any> get(stepId: String, clazz: KClass<T>): T {
            if (clazz != type){
                return valueIfNull as T
            }
            if (this.multivalued && stepPropertiesMap[stepId]?.get(this) != null){
                return (stepPropertiesMap[stepId]!![this] as List<T>)[0]
            }
            return (stepPropertiesMap[stepId]!![this] ?: valueIfNull) as T
        }

        fun <T: Any> getAll(stepId: String, clazz: KClass<T>): List<T> {
            if (this.multivalued){
                return if (stepPropertiesMap[stepId]?.get(this) == null) Collections.emptyList() else stepPropertiesMap[stepId]!![this] as List<T>
            }
            return listOf(get(stepId, clazz))
        }

        fun <T : Any> set(stepId: String, property: T?, type: KClass<T> = this.type as KClass<T>){
            if (property == null) {
                return
            }
            if (property::class != type ){
                var found = false
                for (clz in property::class.supertypes) {
                    if (type.toString().contains(clz.toString())){
                        found = true
                    }
                }
                if (!found){
                    return
                }
            }
            if (stepPropertiesMap[stepId] == null){
                stepPropertiesMap[stepId] = HashMap()
            }
            if (this.multivalued){
                if (stepPropertiesMap[stepId]?.get(this) != null){
                    stepPropertiesMap[stepId]!![this] = (stepPropertiesMap[stepId]!![this] as List<T>).plus(property)
                }else{
                    stepPropertiesMap[stepId]!![this] = listOf(property)
                }
            }else{
                stepPropertiesMap[stepId]!![this] = property
            }
        }
    }

    companion object {
        private val propertiesMap = HashMap<WalkthroughProperty,Any>()
        private val stepPropertiesMap = HashMap<String,HashMap<WalkthroughStepProperty,Any>>()
    }

    fun addStep(step: AbstractStep?){
        if (step != null){
            STEP_LIST.set(step)
            STEP_ID.set(step.id,step.id)
            STEP_TIME.set(step.id,step.time)
            STEP_NUMBER.set(step.id,STEP_LIST.getAll(AbstractStep::class).size)
            STEP_TITLE.set(step.id,step.title)
        }
    }

    fun printStatistics(): String{
        var total = INTRO_TIME.get(Long::class)
        for (step in STEP_LIST.getAll(AbstractStep::class)){
            total += step.time
        }
        total /= 1000
        var text = "\nStatistics:\nYour last Walkthrough took $total seconds to complete!\n"
        for (step in STEP_LIST.getAll(AbstractStep::class)){
            text += "You spent "+step.time/1000+" seconds in \""+step.title+"\",\n"
        }
        text = text.substring(0,text.length-2).plus(".\n")
        text += "Additionally, you spent "+ INTRO_TIME.get(Long::class)/1000+" seconds in the Introduction and\n"
        text += ""+ INFO_TIME.get(Long::class)/1000+" seconds browsing the Information."
        return text
    }

    class WalkthroughBuilder(private val owner: String, private val scenarioId: String, private val stakeholderId: String) {

        constructor(id: String, owner: String, scenarioId: String, stakeholderId: String) : this(owner, scenarioId, stakeholderId) {
            this.id = id
        }

        private var id: String? = null

        fun build(): Walkthrough {
            return Walkthrough(id ?: UUID.randomUUID().toString(), owner, scenarioId, stakeholderId)
        }
    }
}