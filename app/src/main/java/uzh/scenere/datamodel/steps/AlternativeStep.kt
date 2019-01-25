package uzh.scenere.datamodel.steps

class AlternativeStep(id: String, previousId: String?, pathId: String): AbstractStep(id, previousId, pathId) {
    enum class AlternativeMode{
        SELECTABLE, RANDOM
    }
}