package uzh.scenere.datamodel

class WhatIf private constructor(private val previousId: String, private val description: String) {

    class WhatIfBuilder(val previousId: String, val description: String){

        fun build(): WhatIf{
            val whatIf  = WhatIf(previousId, description)
            return whatIf
        }
    }

}