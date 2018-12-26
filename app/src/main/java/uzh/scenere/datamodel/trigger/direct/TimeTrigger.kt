package uzh.scenere.datamodel.trigger.direct

import android.content.Context
import uzh.scenere.datamodel.trigger.AbstractTrigger
import uzh.scenere.datamodel.trigger.IDirectTrigger

class TimeTrigger(id: String, ownerId: String): AbstractTrigger(id, ownerId), IDirectTrigger {
    enum class TimeMode{
        FIXED_COUNTDOWN, RANDOM_COUNTDOWN, FIXED_TIME, RANDOM_TIME
    }
}