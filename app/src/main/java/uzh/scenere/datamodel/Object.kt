package uzh.scenere.datamodel

import java.io.Serializable
import java.util.*

open class Object private constructor(val id: String, val scenarioId: String, val name: String, val description: String) : Serializable {
    var attributes: List<Attribute> = ArrayList()

    class ObjectBuilder(private val scenarioId: String, private val name: String, private val description: String) {

        constructor(scenario: Scenario, name: String, description: String) : this(scenario.id, name, description)

        constructor(id: String, scenario: Scenario, name: String, description: String) : this(scenario.id, name, description) {
            this.id = id
        }

        constructor(id: String, scenarioId: String, name: String, description: String) : this(scenarioId, name, description) {
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
            val obj = Object(id ?: UUID.randomUUID().toString(), scenarioId, name, description)
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

    class NullObject(): Object("","","","") {}
}