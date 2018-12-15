package uzh.scenere.datamodel.transitions.direct

import android.content.Context
import uzh.scenere.datamodel.transitions.AbstractTrigger
import uzh.scenere.datamodel.transitions.IDirectTrigger

class TimeTrigger(context: Context): AbstractTrigger(context), IDirectTrigger {
    enum class TimeMode{
        FIXED_COUNTDOWN, RANDOM_COUNTDOWN, FIXED_TIME, RANDOM_TIME
    }
}