package uzh.scenere.helpers

class StringHelper{
    companion object { //Static Reference
        fun <T : String> nvl(value: T?, valueIfNull: T): T {
            return value ?: valueIfNull; // Elvis Expression of Java number==null?valueIfNull:number
        }
    }
}