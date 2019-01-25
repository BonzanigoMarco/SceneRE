package uzh.scenere.datamodel.steps

abstract class ResourceStep(id: String, previousId: String?, pathId: String): AbstractStep(id, previousId, pathId) {
    enum class ResourceMode{
        CHECK, SUBTRACTION, ADDITION
    }
}