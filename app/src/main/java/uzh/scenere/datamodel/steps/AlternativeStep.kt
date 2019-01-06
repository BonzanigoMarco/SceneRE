package uzh.scenere.datamodel.steps

class AlternativeStep(id: String, originTriggerId: String?): AbstractStep(id, originTriggerId) {
    enum class AlternativeMode{
        SELECTABLE, RANDOM
    }
}