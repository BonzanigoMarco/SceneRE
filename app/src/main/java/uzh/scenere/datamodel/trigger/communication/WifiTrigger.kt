package uzh.scenere.datamodel.trigger.communication

import uzh.scenere.datamodel.trigger.AbstractTrigger
import uzh.scenere.datamodel.trigger.ICommunicationTrigger
import java.util.*

class WifiTrigger(id: String?, previousId: String?, pathId: String): AbstractTrigger(id?: UUID.randomUUID().toString(), previousId, pathId), ICommunicationTrigger {

    var text: String? = null
    var ssid: String? = null

    fun withText(text: String?): WifiTrigger {
        this.text = text
        return this
    }

    fun withSsid(ssid: String?): WifiTrigger {
        this.ssid = ssid
        return this
    }

}