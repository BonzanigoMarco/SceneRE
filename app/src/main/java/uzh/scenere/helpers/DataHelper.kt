package uzh.scenere.helpers

import uzh.scenere.const.Constants.Companion.BOOLEAN
import uzh.scenere.const.Constants.Companion.DOUBLE
import uzh.scenere.const.Constants.Companion.FLOAT
import uzh.scenere.const.Constants.Companion.INT
import uzh.scenere.const.Constants.Companion.LONG
import uzh.scenere.const.Constants.Companion.STRING
import java.io.*
import kotlin.reflect.KClass

class DataHelper {
    companion object {
        fun toByteArray(serializable: Serializable): ByteArray {
            val bos = ByteArrayOutputStream()
            return try {
                val oos = ObjectOutputStream(bos)
                oos.writeObject(serializable)
                oos.flush()
                val objectBytes: ByteArray = bos.toByteArray()
                bos.close()
                objectBytes
            } catch (e: Exception) {
                try {
                    bos.close()
                } catch (e: IOException) {
                    //NOP
                }
                ByteArray(0)
            }
        }

        @Suppress("UNCHECKED_CAST")
        fun <T : Any> toObject(byteArray: ByteArray, clz: KClass<T>): T? {
            val bis = ByteArrayInputStream(byteArray)
            return try {
                val ois = ObjectInputStream(bis)
                val obj = ois.readObject()
                obj as T
            } catch (e: Exception) {
                try {
                    bis.close()
                } catch (e: IOException) {
                    //NOP
                }
                return null
            }
        }

        fun parseString(stringValue: String, clazz: String): Any? {
            if (StringHelper.hasText(stringValue) && StringHelper.hasText(clazz)) {
                when (clazz) {
                    STRING -> return stringValue
                    INT -> return stringValue.toInt()
                    FLOAT -> return stringValue.toFloat()
                    DOUBLE -> return stringValue.toDouble()
                    BOOLEAN -> return stringValue.toBoolean()
                    LONG -> return stringValue.toLong()
                }
            }
            return null
        }
    }
}