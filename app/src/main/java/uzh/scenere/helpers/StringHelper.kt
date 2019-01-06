package uzh.scenere.helpers

import android.content.Context

class StringHelper{
    companion object { //Static Reference
        fun <T : String> nvl(value: T?, valueIfNull: T): T {
            return value ?: valueIfNull; // Elvis Expression of Java number==null?valueIfNull:number
        }

        fun lookupOrEmpty(id: Int?, applicationContext: Context?): CharSequence? {
            return if (id==null) "" else applicationContext?.resources?.getString(id)
        }
    }
}