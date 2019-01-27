package uzh.scenere.datamodel

import uzh.scenere.datamodel.steps.AbstractStep
import uzh.scenere.helpers.NumberHelper
import java.util.*
import kotlin.collections.ArrayList

class Walkthrough private constructor(val id: String, val owner: String, val scenarioId: String, val stakeholderId: String) {
    private var introTime: Long = 0;
    private var infoTime: Long = 0;
    private val stepList: ArrayList<AbstractStep> = ArrayList()

    fun setIntroTime(l: Long) {
        introTime = l
    }
    fun setInfoTime(l: Long) {
        infoTime += l
    }

    fun addStep(step: AbstractStep?){
        if (step != null){
            stepList.add(step)
        }
    }

    fun printStatistics(): String{
        var total: Long = introTime
        for (step in stepList){
            total+= step.time
        }
        total /= 1000
        var text = "\nStatistics:\nYour last Walkthrough took $total seconds to complete!\n"
        for (step in stepList){
            text += "You spent "+step.time/1000+" seconds in \""+step.title+"\",\n"
        }
        text = text.substring(0,text.length-2).plus(".\n")
        text += "Additionally, you spent "+introTime/1000+" seconds in the Introduction and\n"
        text += ""+infoTime/1000+" seconds browsing the Information."
        return text
    }

    class WalkthroughBuilder(private val owner: String, private val scenarioId: String, private val stakeholderId: String) {

        constructor(id: String, owner: String, scenarioId: String, stakeholderId: String) : this(owner, scenarioId, stakeholderId) {
            this.id = id
        }

        private var id: String? = null

        fun build(): Walkthrough {
            return Walkthrough(id ?: UUID.randomUUID().toString(), owner, scenarioId, stakeholderId)
        }
    }
}