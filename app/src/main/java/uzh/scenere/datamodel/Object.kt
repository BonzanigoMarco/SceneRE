package uzh.scenere.datamodel

import uzh.scenere.helpers.StringHelper
import java.io.Serializable
import java.util.*

abstract class AbstractObject internal constructor(val id: String, val scenarioId: String, val name: String, val description: String, val isResource: Boolean) : Serializable {
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

    abstract class AbstractObjectBuilder(private val scenarioId: String, private val name: String, private val description: String) {

        protected var id: String? = null
        protected var attributes: List<Attribute> = ArrayList()

        fun addAttributes(vararg attributes: Attribute): AbstractObjectBuilder {
            this.attributes = this.attributes.plus(attributes)
            return this
        }

        fun copyId(obj: AbstractObject): AbstractObjectBuilder {
            this.id = obj.id
            return this
        }

        abstract fun build(): AbstractObject
    }

    override fun equals(other: Any?): Boolean {
        if (other is AbstractObject) {
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
}