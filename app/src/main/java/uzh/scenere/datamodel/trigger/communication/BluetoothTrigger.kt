package uzh.scenere.datamodel.trigger.communication

import android.content.Context
import uzh.scenere.datamodel.trigger.AbstractTrigger
import uzh.scenere.datamodel.trigger.ICommunicationTrigger
import java.util.*

class BluetoothTrigger(id: String?, previousId: String?, pathId: String): AbstractTrigger(id ?: UUID.randomUUID().toString(), previousId, pathId), ICommunicationTrigger {

    var text: String? = null
    var deviceId: String? = null

    fun withText(text: String?): BluetoothTrigger {
        this.text = text
        return this
    }

    fun withDeviceId(deviceId: String?): BluetoothTrigger {
        this.deviceId = deviceId
        return this
    }
}