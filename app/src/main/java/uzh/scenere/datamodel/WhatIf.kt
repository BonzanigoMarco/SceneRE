package uzh.scenere.datamodel

class WhatIf private constructor(private val ownerStepId: String, private val description: String) {

    class WhatIfBuilder(val ownerStepId: String, val description: String){

        fun build(): WhatIf{
            val whatIf  = WhatIf(ownerStepId, description)
            return whatIf
        }
    }

}