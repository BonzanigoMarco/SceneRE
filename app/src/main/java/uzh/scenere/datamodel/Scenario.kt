package uzh.scenere.datamodel

import uzh.scenere.datamodel.steps.AbstractStep
import java.io.Serializable
import java.util.*

class Scenario private constructor(val id: String, val projectId: String, val title: String, val intro: String, val outro: String): Serializable {
    private var startingPoint: AbstractStep? = null
    private val resources: List<Resource> = ArrayList()
    private val objects: List<Object> = ArrayList()
    private val paths: List<Path> = ArrayList()
    private val stakeholders: List<Stakeholder> = ArrayList()
    private val walkthroughs: List<Walkthrough> = ArrayList()
    private val whatIfs: List<WhatIf> = ArrayList()

    class ScenarioBuilder(private val projectId: String, private val title: String, private val intro: String, private val outro: String){

        constructor(project: Project, title: String, intro: String, outro: String): this(project.id,title,intro,outro)

        constructor(id: String, project: Project, title: String, intro: String, outro: String): this(project,title,intro,outro){
            this.id = id
        }

        constructor(id: String, projectId: String, title: String, intro: String, outro: String): this(projectId,title,intro,outro){
            this.id = id
        }

        private var id: String? = null
        private val resources: List<Resource> = ArrayList()
        private val objects: List<Object> = ArrayList()
        private val paths: List<Path> = ArrayList()
        private val stakeholders: List<Stakeholder> = ArrayList()
        private val walkthroughs: List<Walkthrough> = ArrayList()
        private val whatIfs: List<WhatIf> = ArrayList()

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
        fun copyId(scenario: Scenario) {
            this.id = scenario.id
        }

        fun build(): Scenario{
            val scenario  = Scenario(id?: UUID.randomUUID().toString(),projectId, title, intro, outro)
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