package uzh.scenere.datamodel.trigger.communication

import android.content.Context
import uzh.scenere.datamodel.trigger.AbstractTrigger
import uzh.scenere.datamodel.trigger.ICommunicationTrigger

class NfcTrigger(id: String, ownerId: String): AbstractTrigger(id, ownerId), ICommunicationTrigger {
}