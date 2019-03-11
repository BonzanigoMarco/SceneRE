package uzh.scenere.datamodel.trigger.sensor

import uzh.scenere.datamodel.trigger.AbstractTrigger
import uzh.scenere.datamodel.trigger.ISensorTrigger
import java.util.*

@Deprecated("Most Devices don't support this Sensor")
class PressureTrigger(id: String?, previousId: String?, pathId: String): AbstractTrigger(id ?: UUID.randomUUID().toString(), previousId, pathId), ISensorTrigger {
}