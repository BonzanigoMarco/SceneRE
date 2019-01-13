package uzh.scenere.datamodel

import uzh.scenere.datamodel.steps.AbstractStep
import java.io.Serializable
import java.util.*

class Scenario private constructor(val id: String, val title: String): Serializable {
    private var intro: String? = null
    private var outro: String? = null
    private var startingPoint: AbstractStep? = null
    private val resources: List<Resource> = ArrayList()
    private val objects: List<Object> = ArrayList()
    private val paths: List<Path> = ArrayList()
    private val stakeholders: List<Stakeholder> = ArrayList()
    private val walkthroughs: List<Walkthrough> = ArrayList()
    private val whatIfs: List<WhatIf> = ArrayList()


    class ScenarioBuilder(val title: String){

        constructor(id: String, title: String): this(title){
            this.id = id
        }

        private var id: String? = null
        private var intro: String? = null
        private var outro: String? = null
        private val resources: List<Resource> = ArrayList()
        private val objects: List<Object> = ArrayList()
        private val paths: List<Path> = ArrayList()
        private val stakeholders: List<Stakeholder> = ArrayList()
        private val walkthroughs: List<Walkthrough> = ArrayList()
        private val whatIfs: List<WhatIf> = ArrayList()

        fun withIntro(intro: String): ScenarioBuilder{
            this.intro = intro
            return this
        }
        fun withOutro(outro: String): ScenarioBuilder{
            this.outro = outro
            return this
        }
        fun addResource(resource: Resource): ScenarioBuilder{
            this.resources.plus(resource)
            return this
        }
        fun addObject(obj: Object): ScenarioBuilder{
            this.objects.plus(obj)
            return this
        }
        fun addPath(path: Path): ScenarioBuilder{
            this.paths.plus(path)
            return this
        }
        fun addStakeholder(stakeholder: Stakeholder): ScenarioBuilder{
            this.stakeholders.plus(stakeholder)
            return this
        }
        fun addWalkthrough(walkthrough: Walkthrough): ScenarioBuilder{
            this.walkthroughs.plus(walkthrough)
            return this
        }
        fun addWhatIf(whatIf: WhatIf): ScenarioBuilder{
            this.whatIfs.plus(whatIf)
            return this
        }

        fun build(): Scenario{
            val scenario  = Scenario(UUID.randomUUID().toString(),title)
            scenario.intro = this.intro
            scenario.outro = this.outro
            scenario.resources.plus(this.resources)
            scenario.objects.plus(this.objects)
            scenario.paths.plus(this.paths)
            scenario.stakeholders.plus(this.stakeholders)
            scenario.walkthroughs.plus(this.walkthroughs)
            scenario.whatIfs.plus(this.whatIfs)
            scenario.startingPoint = getStartingPoint()
            return scenario
        }
        private fun getStartingPoint(): AbstractStep?{
            for (path in paths){
                val step = path.getStartingPoint()
                if (step != null) return step
            }
            return null
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other is Scenario){
            return (id == other.id)
        }
        return false
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }
}