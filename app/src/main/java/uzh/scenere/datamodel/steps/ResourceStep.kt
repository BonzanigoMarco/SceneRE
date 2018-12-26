package uzh.scenere.datamodel.steps

abstract class ResourceStep(id: String): AbstractStep(id) {
    enum class ResourceMode{
        CHECK, SUBTRACTION, ADDITION
    }
}