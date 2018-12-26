package uzh.scenere.datamodel

class Stakeholder private constructor(private val name: String, private val introduction: String) {

    class StakeholderBuilder(val name: String, val introduction: String){

        fun build(): Stakeholder{
            val stakeholder  = Stakeholder(name,introduction)
            return stakeholder
        }
    }

}