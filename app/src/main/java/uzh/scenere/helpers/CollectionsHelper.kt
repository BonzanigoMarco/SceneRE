package uzh.scenere.helpers

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
    }
}