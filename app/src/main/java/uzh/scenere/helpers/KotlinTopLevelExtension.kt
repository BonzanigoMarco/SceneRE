package uzh.scenere.helpers

import android.widget.EditText
import android.widget.TextView
import uzh.scenere.const.Constants.Companion.NOTHING
import uzh.scenere.const.Constants.Companion.REFLECTION
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
    if (s.startsWith("Null") && !this::class.supertypes.isEmpty()){
        return this::class.supertypes[0].className()
    }
    return s
}