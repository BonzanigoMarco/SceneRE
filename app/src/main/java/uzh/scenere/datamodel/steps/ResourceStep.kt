package uzh.scenere.datamodel.steps

import uzh.scenere.datamodel.Resource
import java.util.*

abstract class ResourceStep(id: String?, previousId: String?, pathId: String): AbstractStep(id ?: UUID.randomUUID().toString(), previousId, pathId) {
    enum class ResourceMode{
        CHECK, SUBTRACTION, ADDITION, INPUT
    }
    var resource: Resource? = null
    var mode: ResourceMode = ResourceMode.CHECK
}