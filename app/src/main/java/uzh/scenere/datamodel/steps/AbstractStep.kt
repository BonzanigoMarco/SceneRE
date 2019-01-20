package uzh.scenere.datamodel.steps

import uzh.scenere.datamodel.IElement

abstract class AbstractStep(val id: String, var originTriggerId: String?): IElement {
    var triggerId: String? = null
}