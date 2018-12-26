package uzh.scenere.datamodel.trigger.sensor

import android.content.Context
import uzh.scenere.datamodel.trigger.AbstractTrigger
import uzh.scenere.datamodel.trigger.ISensorTrigger

class TemperatureTrigger(id: String, ownerId: String): AbstractTrigger(id, ownerId), ISensorTrigger {
}