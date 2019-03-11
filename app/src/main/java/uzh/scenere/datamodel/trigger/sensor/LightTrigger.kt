package uzh.scenere.datamodel.trigger.sensor

import android.content.Context
import uzh.scenere.datamodel.trigger.AbstractTrigger
import uzh.scenere.datamodel.trigger.ISensorTrigger
import java.util.*

class LightTrigger(id: String?, previousId: String?, pathId: String): AbstractTrigger(id ?: UUID.randomUUID().toString(), previousId, pathId), ISensorTrigger {
}