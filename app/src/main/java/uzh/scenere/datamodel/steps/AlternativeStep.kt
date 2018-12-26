package uzh.scenere.datamodel.steps

class AlternativeStep(id: String): AbstractStep(id) {
    enum class AlternativeMode{
        SELECTABLE, RANDOM
    }
}