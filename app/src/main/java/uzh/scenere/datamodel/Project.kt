package uzh.scenere.datamodel

class Project private constructor(private val name: String, private val description: String) {
    var scenarios: List<Scenario> = ArrayList()

    class ProjectBuilder(val name: String, val description: String){
        private var scenarios: List<Scenario> = ArrayList()

        fun withScenarios(scenarios: List<Scenario>): ProjectBuilder{
            if (this.scenarios.isEmpty()){
                this.scenarios = scenarios
            }else{
                this.scenarios.plus(scenarios)
            }
            return this
        }
        fun addScenario(scenario: Scenario): ProjectBuilder{
            this.scenarios.plus(scenario)
            return this
        }
        fun build(): Project{
            val project = Project(name,description)
            project.scenarios = this.scenarios
            return project
        }
    }
}