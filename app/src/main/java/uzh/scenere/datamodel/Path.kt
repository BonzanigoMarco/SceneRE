package uzh.scenere.datamodel

import uzh.scenere.datamodel.steps.AbstractStep
import uzh.scenere.datamodel.trigger.AbstractTrigger

class Path private constructor(private val name: String) {
    private val steps: HashMap<String,AbstractStep> = HashMap()
    private val triggers: HashMap<String,AbstractTrigger> = HashMap()

    fun getStartingPoint(): AbstractStep?{
        for (entry in steps.entries){
            if (entry.value.originTriggerId==null) return entry.value
        }
        return null;
    }

    class PathBuilder(val name: String) {
        private val steps: HashMap<String,AbstractStep> = HashMap()
        private val triggers: HashMap<String,AbstractTrigger> = HashMap()

        fun addStep(step: AbstractStep): PathBuilder {
            steps[step.id] = step
            return this
        }
        fun addTrigger(trigger: AbstractTrigger): PathBuilder {
            triggers[trigger.id] = trigger
            return this
        }
        fun build(): Path {
            val path = Path(name)
            /**Building Process TODO**/
            path.steps.plus(this.steps)
            path.triggers.plus(this.triggers)
            return path
        }
    }

}