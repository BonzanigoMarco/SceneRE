package uzh.scenere.datamodel.trigger

import uzh.scenere.datamodel.IElement

abstract class AbstractTrigger(val id: String, var ownerStepId: String) : IElement {
    private val nextStepsIds: List<String> = ArrayList()

    fun hasSingleTransition(): Boolean{
        return nextStepsIds.size==1
    }

    fun getTransition(): String{
        return ""
    }
}