package uzh.scenere.datamodel

import java.util.*

class Project private constructor(val name: String, val description: String, val id: String) {
    var scenarios: List<Scenario> = ArrayList()

    class ProjectBuilder(val name: String, val description: String){
        private var scenarios: List<Scenario> = ArrayList()
        private var id: String? = null

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
        fun copyId(project: Project): ProjectBuilder{
            this.id = project.id
            return this
        }
        fun build(): Project{
            val project = Project(name,description,id?: UUID.randomUUID().toString())
            project.scenarios = this.scenarios
            return project
        }
    }
}