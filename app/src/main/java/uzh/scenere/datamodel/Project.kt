package uzh.scenere.datamodel

import java.io.Serializable
import java.util.*

class Project private constructor(val id: String, val creator: String, val title: String, val description: String): Serializable {
    var scenarios: List<Scenario> = ArrayList()

    class ProjectBuilder(private val creator: String, private val  title: String, private val  description: String){

        constructor(id: String, creator: String, title: String, description: String) : this(creator, title, description) {
            this.id = id
        }

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
            val project = Project(id?: UUID.randomUUID().toString(),creator,title,description)
            project.scenarios = this.scenarios
            return project
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other is Project){
            return (id == other.id)
        }
        return false
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }
}