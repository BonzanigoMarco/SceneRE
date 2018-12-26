package uzh.scenere.datamodel

class Scenario private constructor(private val title: String) {
    private var intro: String? = null
    private var outro: String? = null
    private val resources: List<Resource> = ArrayList()
    private val paths: List<Path> = ArrayList()
    private val stakeholders: List<Stakeholder> = ArrayList()
    private val walkthroughs: List<Walkthrough> = ArrayList()
    private val whatIfs: List<WhatIf> = ArrayList()

    class ScenarioBuilder(val title: String){
        private var intro: String? = null
        private var outro: String? = null
        private val resources: List<Resource> = ArrayList()
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
            val scenario  = Scenario(title)
            scenario.intro = this.intro
            scenario.outro = this.outro
            scenario.resources.plus(this.resources)
            scenario.paths.plus(this.paths)
            scenario.stakeholders.plus(this.stakeholders)
            scenario.walkthroughs.plus(this.walkthroughs)
            scenario.whatIfs.plus(this.whatIfs)
            return scenario
        }
    }

}