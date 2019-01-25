package uzh.scenere.datamodel.trigger.communication

import android.content.Context
import uzh.scenere.datamodel.trigger.AbstractTrigger
import uzh.scenere.datamodel.trigger.ICommunicationTrigger

class NfcTrigger(id: String, previousId: String, pathId: String): AbstractTrigger(id, previousId, pathId), ICommunicationTrigger {
}