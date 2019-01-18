package uzh.scenere.datamodel

import java.io.Serializable
import java.util.*

class Object private constructor(val id: String, val scenarioId: String, val name: String, val description: String, val attributes: List<Attribute>) : Serializable {

    class ObjectBuilder(private val scenarioId: String, private val name: String, private val description: String) {

        constructor(scenario: Scenario, name: String, description: String) : this(scenario.id, name, description)

        constructor(id: String, scenario: Scenario, name: String, description: String) : this(scenario.id, name, description) {
            this.id = id
        }

        constructor(id: String, scenarioId: String, name: String, description: String) : this(scenarioId, name, description) {
            this.id = id
        }

        private var id: String? = null
        private val attributes = ArrayList<Attribute>()

        fun addAttribute(attribute: Attribute): ObjectBuilder {
            attributes.add(attribute)
            return this
        }

        fun copyId(obj: Object): ObjectBuilder {
            this.id = obj.id
            return this
        }

        fun build(): Object {
            return Object(id
                    ?: UUID.randomUUID().toString(), scenarioId, name, description, attributes)
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
}