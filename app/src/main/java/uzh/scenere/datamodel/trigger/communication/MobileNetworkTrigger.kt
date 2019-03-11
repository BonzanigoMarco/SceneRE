package uzh.scenere.datamodel.trigger.communication

import android.content.Context
import uzh.scenere.datamodel.trigger.AbstractTrigger
import uzh.scenere.datamodel.trigger.ICommunicationTrigger
import java.util.*


@Deprecated("No Application yet")
class MobileNetworkTrigger(id: String?, previousId: String?, pathId: String): AbstractTrigger(id ?: UUID.randomUUID().toString(), previousId, pathId), ICommunicationTrigger {
}