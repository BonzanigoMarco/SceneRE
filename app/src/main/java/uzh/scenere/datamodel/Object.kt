package uzh.scenere.datamodel

import uzh.scenere.helpers.StringHelper
import java.io.Serializable
import java.util.*

open class Object private constructor(val id: String, val scenarioId: String, val name: String, val description: String, val isResource: Boolean) : Serializable {
    var attributes: List<Attribute> = ArrayList()

    fun getAttributeNames(vararg additionalName: String): Array<String> {
        val list = ArrayList<String>()
        list.addAll(additionalName)
        for (attribute in attributes) {
            if (StringHelper.hasText(attribute.key)) {
                list.add(attribute.key!!)
            }
        }
        return list.toTypedArray()
    }

    fun getAttributeByName(name: String?): Attribute? {
        for (attribute in attributes) {
            if (attribute.key == name) {
                return attribute
            }
        }
        return null
    }

    class ObjectBuilder(private val scenarioId: String, private val name: String, private val description: String, private val isResource: Boolean) {

        constructor(scenario: Scenario, name: String, description: String, isResource: Boolean) : this(scenario.id, name, description, isResource)

        constructor(id: String, scenario: Scenario, name: String, description: String, isResource: Boolean) : this(scenario.id, name, description, isResource) {
            this.id = id
        }

        constructor(id: String, scenarioId: String, name: String, description: String, isResource: Boolean) : this(scenarioId, name, description, isResource) {
            this.id = id
        }

        private var id: String? = null
        private var attributes: List<Attribute> = ArrayList()

        fun addAttributes(vararg attributes: Attribute): ObjectBuilder {
            this.attributes = this.attributes.plus(attributes)
            return this
        }

        fun copyId(obj: Object): ObjectBuilder {
            this.id = obj.id
            return this
        }

        fun build(): Object {
            val obj = Object(id
                    ?: UUID.randomUUID().toString(), scenarioId, name, description, isResource)
            obj.attributes = this.attributes
            return obj
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other is Object) {
            return (id == other.id)
        }
        return false
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }

    override fun toString(): String {
        return name
    }

    class NullObject() : Object("", "", "", "", false) {}
}