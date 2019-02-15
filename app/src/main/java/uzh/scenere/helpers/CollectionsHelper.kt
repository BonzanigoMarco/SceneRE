package uzh.scenere.helpers

import java.lang.reflect.Array

class CollectionsHelper private constructor() {
    companion object { //Static Reference
        fun <T : Any> oneOf(value: T?, vararg values: T): Boolean {
            if (value == null || values.isEmpty()){
                return false;
            }
            for (v in values){
                if (value == v){
                    return true
                }
            }
            return false
        }

        @Suppress("UNCHECKED_CAST")
        fun <T> subArray(clazz: Class<T>, source: List<T>, begin: Int, end: Int): kotlin.Array<T> {
            if (source.size < end || end <= begin) {
                return Array.newInstance(clazz, 0) as kotlin.Array<T>
            }
            val array: kotlin.Array<T> = Array.newInstance(clazz, end - begin) as kotlin.Array<T>
            for (i in begin until end) {
                array[i] = source[i]
            }
            return array
        }
    }
}