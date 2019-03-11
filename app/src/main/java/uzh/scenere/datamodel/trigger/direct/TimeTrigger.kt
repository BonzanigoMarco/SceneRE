package uzh.scenere.datamodel.trigger.direct

import uzh.scenere.datamodel.trigger.AbstractTrigger
import uzh.scenere.datamodel.trigger.IDirectTrigger
import uzh.scenere.helpers.NumberHelper
import java.util.*

class TimeTrigger(id: String?, previousId: String?, pathId: String): AbstractTrigger(id ?: UUID.randomUUID().toString(), previousId, pathId), IDirectTrigger {
    enum class TimeMode{
        FIXED_COUNTDOWN, RANDOM_COUNTDOWN, FIXED_TIME, RANDOM_TIME
    }

    val countdown = TimeMode.FIXED_COUNTDOWN

    var text: String? = null
    var timeMs: Long? = null

    fun withText(text: String?): TimeTrigger {
        this.text = text
        return this
    }

    fun withTime(timeMs: Long?): TimeTrigger {
        this.timeMs = timeMs
        return this
    }

    fun withTimeSecond(time: String?): TimeTrigger {
        this.timeMs = NumberHelper.safeToNumber(time,0)*1000
        return this
    }

    fun withTimeMillisecondSecond(time: String?): TimeTrigger {
        this.timeMs = NumberHelper.safeToNumber(time,0)
        return this
    }
}