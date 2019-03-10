package uzh.scenere.helpers

import android.app.Activity
import android.database.Cursor
import android.widget.EditText
import android.widget.TextView
import uzh.scenere.const.Constants.Companion.NOTHING
import uzh.scenere.const.Constants.Companion.NULL_CLASS
import uzh.scenere.const.Constants.Companion.REFLECTION
import uzh.scenere.const.Constants.Companion.SPACE
import java.io.File
import kotlin.random.Random

inline fun <reified INNER> array2d(sizeOuter: Int, sizeInner: Int, noinline innerInit: (Int)->INNER): Array<Array<INNER>> = Array(sizeOuter) { Array<INNER>(sizeInner, innerInit) }
fun array2dOfInt(sizeOuter: Int, sizeInner: Int): Array<IntArray> = Array(sizeOuter) { IntArray(sizeInner) }
fun array2dOfFloat(sizeOuter: Int, sizeInner: Int): Array<FloatArray> = Array(sizeOuter) { FloatArray(sizeInner) }
fun array2dOfDouble(sizeOuter: Int, sizeInner: Int): Array<DoubleArray> = Array(sizeOuter) { DoubleArray(sizeInner) }
fun array2dOfLong(sizeOuter: Int, sizeInner: Int): Array<LongArray> = Array(sizeOuter) { LongArray(sizeInner) }
fun array2dOfByte(sizeOuter: Int, sizeInner: Int): Array<ByteArray> = Array(sizeOuter) { ByteArray(sizeInner) }
fun array2dOfChar(sizeOuter: Int, sizeInner: Int): Array<CharArray> = Array(sizeOuter) { CharArray(sizeInner) }
fun array2dOfBoolean(sizeOuter: Int, sizeInner: Int): Array<BooleanArray> = Array(sizeOuter) { BooleanArray(sizeInner) }
fun floor(value: Double, precision: Int):Double = Math.floor(precision*value)/precision.toDouble()
fun EditText.getStringValue(): String = text.toString()
fun TextView.getStringValue(): String = text.toString()
fun Random.nextSafeInt(range: Int): Int = if (range<=0) 0 else nextInt(range)
fun Any.className(): String {
    val splits = this::class.toString().replace(REFLECTION,NOTHING).split(".")
    val s = splits[splits.size - 1]
    if (s.startsWith(NULL_CLASS) && !this::class.supertypes.isEmpty()){
        return this::class.supertypes[0].className()
    }
    return s
}
fun Any.readableClassName(delimiter: String = SPACE): String {
    val className = className()
    var readableClassName = ""
    for (c in 0 until className.length){
        if (c>0 && className[c].isUpperCase()){
            readableClassName += delimiter
        }
        readableClassName += className[c]
    }
    return readableClassName
}
fun Cursor.getBoolean(columnIndex: Int): Boolean{
    return getInt(columnIndex) == 1
}
fun countNonNull(vararg args: Any?):Int {
    var count = 0
    for (arg in args){
        if (arg != null){
            count++
        }
    }
    return count
}

@Suppress("UNCHECKED_CAST")
fun addToArrayBefore(array: Array<String>, vararg args: String): Array<String>{
    val newArray = arrayOfNulls<String?>(array.size+args.size)
    var i = 0
    for (t in args){
        newArray[i] = t
        i++
    }
    for (t in array){
        newArray[i] = t
        i++
    }
    return newArray as Array<String>
}

@Suppress("UNCHECKED_CAST")
fun <T: Any> addToArrayAfter(array: Array<T>, vararg args: T): Array<T>{
    val newArray: Array<T> = arrayOfNulls<Any>(array.size+args.size) as Array<T>
    var i = 0
    for (t in array){
        newArray[i] = t
        i++
    }
    for (t in args){
        newArray[i] = t
        i++
    }
    return newArray
}

fun ArrayList<*>.toStringArray(): Array<String>{
    val list = ArrayList<String>()
    for (i in 0 until size){
        list.add(get(i).toString())
    }
    return list.toTypedArray()
}

fun List<*>.toStringArray(): Array<String>{
    val list = ArrayList<String>()
    for (i in 0 until size){
        list.add(get(i).toString())
    }
    return list.toTypedArray()
}

fun String.isContainedIn(str: String?):Boolean{
    if (str == null) return false
    return str.contains(this)
}
fun File.isFileType(type: String): Boolean{
    if (isDirectory) return false
    return name.endsWith(type)
}


fun Activity.getIdByString(str: String, type: String = "string") = resources.getIdentifier(str, type, packageName)

fun Activity.getStringByString(str: String, vararg formatArgs: String):String {
    var txt = getString(getIdByString(str))
    for (id in 0 until formatArgs.size){
        txt = txt.replace("%${id+1}\$s",formatArgs[id])
    }
    return txt
}

fun Activity.getGenericStringWithIdAndTemplate(id: Int, templateId: Int, vararg formatArgs: String):String {
    return getStringByString(getString(templateId,id),*formatArgs)
}
