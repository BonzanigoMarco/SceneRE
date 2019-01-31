package uzh.scenere.datamodel.steps

import uzh.scenere.datamodel.IElement
import uzh.scenere.datamodel.Object
import uzh.scenere.datamodel.Path
import uzh.scenere.datamodel.Stakeholder
import uzh.scenere.helpers.NullHelper
import kotlin.collections.ArrayList

abstract class AbstractStep(val id: String, var previousId: String?, val pathId: String): IElement {
    var objects: ArrayList<Object> =  ArrayList<Object>()
    var title: String? = null
    var text: String? = null
    var time: Long = 0

    fun withTitle(title: String): AbstractStep {
        this.title = title
        return this
    }
    fun withText(text: String): AbstractStep {
        this.text = text
        return this
    }
    fun withTime(time: Long): AbstractStep {
        this.time = time
        return this
    }

    fun withObjects(objectList: ArrayList<Object>): IElement{
        this.objects.addAll(objectList)
        return this
    }

    fun withObject(obj: Object): IElement{
        this.objects.plus(obj)
        return this
    }

    override fun getElementId(): String {
        return id
    }

    override fun getElementPathId(): String {
        return pathId
    }


    override fun getPreviousElementId(): String? {
        return previousId
    }

    override fun setPreviousElementId(id: String): IElement {
        previousId = id
        return this
    }

    class NullStep(): AbstractStep("","","") {}
}