package uzh.scenere.datamodel

import uzh.scenere.const.Constants.Companion.NEW_LINE
import uzh.scenere.const.Constants.Companion.NOTHING
import uzh.scenere.const.Constants.Companion.PERCENT
import uzh.scenere.const.Constants.Companion.SPACE
import uzh.scenere.helpers.NumberHelper
import kotlin.reflect.full.isSubclassOf

class StatisticArrayList<E> : ArrayList<E>() {
    private val stats = HashMap<E,Float>()

    override fun add(element: E): Boolean {
        stats[element] = (NumberHelper.nvl(stats[element],0f)+1f)
        return super.add(element)
    }

    fun getStatistics(): String{
        val part = 100f/size
        var statistics = NOTHING
        for (entry in stats.entries){
            statistics += "".plus(entry.key).plus(SPACE).plus(NumberHelper.floor(part*entry.value,2)).plus(PERCENT).plus(NEW_LINE)
        }
        return if (statistics.length > NEW_LINE.length) statistics.substring(0,statistics.length- NEW_LINE.length) else statistics
    }

    @Suppress("UNCHECKED_CAST")
    fun total(): E?{
        if (!isEmpty() && get(0) is Number){
            var total: Number = 0.0
            when(get(0)){
                is Double -> {
                    total = total.toDouble()
                    for (element in this){
                        total += element as Double
                    }
                }
                is Long -> {
                    total = total.toLong()
                    for (element in this){
                        total += element as Long
                    }
                }
                is Float -> {
                    total = total.toFloat()
                    for (element in this){
                        total += element as Float
                    }
                }
                is Int -> {
                    total = total.toInt()
                    for (element in this){
                        total += element as Int
                    }
                }
                else -> return null
            }
            return total as E
        }
        return null
    }

    @Suppress("UNCHECKED_CAST")
    fun avg(): E? {
        val total = total()
        if (total != null){
            when (total){
                is Double ->  return (total as Double/size) as E
                is Long ->  return (total as Long/size) as E
                is Float ->  return (total as Float/size) as E
                is Int ->  return (total as Int/size) as E
            }
        }
        return null
    }
}