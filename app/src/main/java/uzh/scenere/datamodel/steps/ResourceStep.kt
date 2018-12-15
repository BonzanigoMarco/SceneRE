package uzh.scenere.datamodel.steps

abstract class ResourceStep: AbstractStep() {
    enum class ResourceMode{
        CHECK, SUBTRACTION, ADDITION
    }
}