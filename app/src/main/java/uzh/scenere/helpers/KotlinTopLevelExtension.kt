package uzh.scenere.helpers

import android.database.Cursor
import android.widget.EditText
import android.widget.TextView
import uzh.scenere.const.Constants.Companion.NOTHING
import uzh.scenere.const.Constants.Companion.NULL_CLASS
import uzh.scenere.const.Constants.Companion.REFLECTION
import uzh.scenere.const.Constants.Companion.SPACE
import kotlin.random.Random

public inline fun <reified INNER> array2d(sizeOuter: Int, sizeInner: Int, noinline innerInit: (Int)->INNER): Array<Array<INNER>> = Array(sizeOuter) { Array<INNER>(sizeInner, innerInit) }
public fun array2dOfInt(sizeOuter: Int, sizeInner: Int): Array<IntArray> = Array(sizeOuter) { IntArray(sizeInner) }
public fun array2dOfFloat(sizeOuter: Int, sizeInner: Int): Array<FloatArray> = Array(sizeOuter) { FloatArray(sizeInner) }
public fun array2dOfDouble(sizeOuter: Int, sizeInner: Int): Array<DoubleArray> = Array(sizeOuter) { DoubleArray(sizeInner) }
public fun array2dOfLong(sizeOuter: Int, sizeInner: Int): Array<LongArray> = Array(sizeOuter) { LongArray(sizeInner) }
public fun array2dOfByte(sizeOuter: Int, sizeInner: Int): Array<ByteArray> = Array(sizeOuter) { ByteArray(sizeInner) }
public fun array2dOfChar(sizeOuter: Int, sizeInner: Int): Array<CharArray> = Array(sizeOuter) { CharArray(sizeInner) }
public fun array2dOfBoolean(sizeOuter: Int, sizeInner: Int): Array<BooleanArray> = Array(sizeOuter) { BooleanArray(sizeInner) }
public fun floor(value: Double, precision: Int):Double = Math.floor(precision*value)/precision.toDouble()
public fun EditText.getStringValue(): String = text.toString()
public fun TextView.getStringValue(): String = text.toString()
public fun Random.nextSafeInt(range: Int): Int = if (range<=0) 0 else nextInt(range)
public fun Any.className(): String {
    val splits = this::class.toString().replace(REFLECTION,NOTHING).split(".")
    val s = splits[splits.size - 1]
    if (s.startsWith(NULL_CLASS) && !this::class.supertypes.isEmpty()){
        return this::class.supertypes[0].className()
    }
    return s
}
public fun Any.readableClassName(delimiter: String = SPACE): String {
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
public fun Cursor.getBoolean(columnIndex: Int): Boolean{
    return getInt(columnIndex) == 1
}
public fun countNonNull(vararg args: Any?):Int {
    var count = 0
    for (arg in args){
        if (arg != null){
            count++
        }
    }
    return count
}

@Suppress("UNCHECKED_CAST")
public fun addToArrayBefore(array: Array<String>, vararg args: String): Array<String>{
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
public fun <T: Any> addToArrayAfter(array: Array<T>, vararg args: T): Array<T>{
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