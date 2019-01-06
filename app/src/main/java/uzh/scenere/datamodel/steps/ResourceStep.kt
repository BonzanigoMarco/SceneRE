package uzh.scenere.datamodel.steps

abstract class ResourceStep(id: String, originTriggerId: String?): AbstractStep(id, originTriggerId) {
    enum class ResourceMode{
        CHECK, SUBTRACTION, ADDITION
    }
}