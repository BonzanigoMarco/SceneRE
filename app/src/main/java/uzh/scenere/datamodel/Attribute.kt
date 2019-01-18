package uzh.scenere.datamodel

import java.io.Serializable
import java.util.*

class Attribute private constructor(val id: String, val refId: String, val key: String, val value: String) : Serializable {

    class AttributeBuilder(private val refId: String, private val key: String, private val value: String) {

        constructor(id: String, refId: String, key: String, value: String) : this(refId, key, value) {
            this.id = id
        }

        private var id: String? = null

        fun copyId(attribute: Attribute): AttributeBuilder {
            this.id = attribute.id
            return this
        }

        fun build(): Attribute {
            return Attribute(id ?: UUID.randomUUID().toString(), refId, key, value)
        }

    }

    override fun equals(other: Any?): Boolean {
        if (other is Attribute) {
            return (id == other.id)
        }
        return false
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }
}