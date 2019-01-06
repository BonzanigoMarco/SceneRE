package uzh.scenere.datamodel.steps

abstract class AbstractStep(val id: String, var originTriggerId: String?) {
    var triggerId: String? = null
}