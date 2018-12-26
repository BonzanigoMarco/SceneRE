package uzh.scenere.datamodel

import java.util.*

class Walkthrough private constructor(private val owner: String, private val id: String) {

    class WalkthroughBuilder(val owner: String, val id: String){

        fun build(): Walkthrough{
            val walkthrough  = Walkthrough(owner, UUID.randomUUID().toString())
            return walkthrough
        }
    }

}