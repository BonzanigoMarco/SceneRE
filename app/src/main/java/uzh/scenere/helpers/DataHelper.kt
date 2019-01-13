package uzh.scenere.helpers

import java.io.*
import java.lang.Exception
import kotlin.reflect.KClass

class DataHelper{
    companion object {
        fun toByteArray(serializable: Serializable): ByteArray{
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
        fun <T : Any> toObject(byteArray: ByteArray, clz: KClass<T>): T?{
            val bis = ByteArrayInputStream(byteArray)
            return try {
                val ois =  ObjectInputStream(bis)
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
    }
}