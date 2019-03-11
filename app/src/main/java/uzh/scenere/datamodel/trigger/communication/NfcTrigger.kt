package uzh.scenere.datamodel.trigger.communication

import uzh.scenere.const.Constants.Companion.NOTHING
import uzh.scenere.datamodel.trigger.AbstractTrigger
import uzh.scenere.datamodel.trigger.ICommunicationTrigger
import uzh.scenere.helpers.StringHelper
import java.util.*

class NfcTrigger(id: String?, previousId: String?, pathId: String): AbstractTrigger(id?: UUID.randomUUID().toString(), previousId, pathId), ICommunicationTrigger {

    var text: String? = null
    var message: String? = null

    fun withText(text: String?): NfcTrigger {
        this.text = text
        return this
    }

    fun withMessage(message: String?): NfcTrigger {
        this.message = message
        return this
    }
}