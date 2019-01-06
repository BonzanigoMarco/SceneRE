package uzh.scenere.helpers

import android.app.Activity

class NumberHelper{
    companion object { //Static Reference
        fun <T : Number> nvl(value: T?, valueIfNull: T): T {
            return value ?: valueIfNull; // Elvis Expression of Java number==null?valueIfNull:number
        }
    }
}