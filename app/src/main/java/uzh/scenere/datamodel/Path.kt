package uzh.scenere.datamodel

import uzh.scenere.const.Constants.Companion.STARTING_POINT
import java.io.Serializable
import java.util.*

class Path private constructor(val id: String, val scenarioId: String, val stakeholder: Stakeholder, val layer: Int): Serializable {
    private val elements: HashMap<String,IElement> = HashMap()
    private val previousElements: HashMap<String,IElement> = HashMap()

    fun getStartingPoint(): IElement?{
        return previousElements[STARTING_POINT]
    }

    fun getEndPoint(): IElement? {
        var element = getStartingPoint()
        do{
            val nextElement = getNextElement(element?.getElementId())
            if (nextElement != null){
                element = nextElement
            }
        }while(nextElement != null)
        return element
    }

    fun getNextElement(currentElementId: String?): IElement?{
        return previousElements[currentElementId]
    }

    fun add(element: IElement) {
        elements[element.getElementId()] = element
        if (element.getPreviousElementId() == null){
            previousElements[STARTING_POINT] = element.setPreviousElementId(STARTING_POINT)
        }else{
            previousElements[element.getPreviousElementId()!!] = element
        }
    }

    class PathBuilder(private val scenarioId: String, private val stakeholder: Stakeholder, private val layer: Int) {

        constructor(id: String, scenarioId: String, stakeholder: Stakeholder, layer: Int): this(scenarioId, stakeholder, layer){
            this.id = id
        }

        private var id: String? = null

        fun build(): Path {
            return Path(id ?: UUID.randomUUID().toString(),scenarioId, stakeholder, layer)
        }
    }

}