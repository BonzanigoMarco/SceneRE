package uzh.scenere.datamodel.trigger.indirect

import android.content.Context
import uzh.scenere.datamodel.trigger.AbstractTrigger
import uzh.scenere.datamodel.trigger.IIndirectTrigger

class SoundTrigger(id: String, ownerId: String): AbstractTrigger(id, ownerId), IIndirectTrigger {
    enum class SoundMode{
        LOUD, QUIET
    }
}