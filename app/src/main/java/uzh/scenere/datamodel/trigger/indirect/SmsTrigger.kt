package uzh.scenere.datamodel.trigger.indirect

import android.content.Context
import uzh.scenere.datamodel.trigger.AbstractTrigger
import uzh.scenere.datamodel.trigger.IIndirectTrigger

class SmsTrigger(id: String, ownerId: String): AbstractTrigger(id, ownerId), IIndirectTrigger {
    enum class SmsMode{
        FIXED, RANDOM
    }
}