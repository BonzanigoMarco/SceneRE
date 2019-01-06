package uzh.scenere.datamodel.trigger

abstract class AbstractTrigger(val id: String, var ownerStepId: String) : ITrigger {
    private val nextStepsIds: List<String> = ArrayList()

    fun hasSingleTransition(): Boolean{
        return nextStepsIds.size==1
    }

    fun getTransition(): String{
        return ""
    }
}