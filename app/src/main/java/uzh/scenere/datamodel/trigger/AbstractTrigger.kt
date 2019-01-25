package uzh.scenere.datamodel.trigger

import uzh.scenere.datamodel.IElement

abstract class AbstractTrigger(val id: String, var previousId: String?, val pathId: String) : IElement {
    private val nextStepsIds: List<String> = ArrayList()

    fun hasSingleTransition(): Boolean{
        return nextStepsIds.size==1
    }

    fun getTransition(): String{
        return ""
    }

    override fun getElementId(): String {
        return id
    }

    override fun getPreviousElementId(): String? {
        return previousId
    }

    override fun getElementPathId(): String {
        return pathId
    }

    override fun setPreviousElementId(id: String): IElement {
        previousId = id
        return this
    }
}