package uzh.scenere.datamodel

import java.util.*

class Object private constructor(private val name: String, private val id: String) {

    class ObjectBuilder(val name: String, val id: String){

        fun build(): Object{
            val obj  = Object(name, UUID.randomUUID().toString())
            return obj
        }
    }

}