package uzh.scenere.datamodel

import java.util.*

class Stakeholder private constructor(val id: String,val projectId: String,val name: String, val introduction: String) {

    class StakeholderBuilder(private val project: Project,private val name: String, private val introduction: String){

        constructor(id: String, project: Project, name: String, introduction: String): this(project,name,introduction){
            this.id = id
        }

        private var id: String? = null

        fun build(): Stakeholder{
            return Stakeholder(id?: UUID.randomUUID().toString(),project.id,name,introduction)
        }

        fun copyId(stakeholder: Stakeholder) {
            this.id = stakeholder.id
        }
    }

}