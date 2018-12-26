package uzh.scenere.datamodel.trigger

abstract class AbstractTrigger(val id: String, val ownerId: String) : ITrigger {
    private val steps: List<String> = ArrayList()

    fun hasSingleTransition(): Boolean{
        return steps.size==1
    }

    fun getTransition(): String{
        return ""
    }
}