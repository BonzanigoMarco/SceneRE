package uzh.scenere.helpers

class CollectionsHelper private constructor (){
  companion object { //Static Reference
    fun <T : Any> nvl(value: T?, valueIfNull: T): T {
      return value ?: valueIfNull; // Elvis Expression of Java number==null?valueIfNull:number
    }
  }
}