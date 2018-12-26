package uzh.scenere.datamodel.trigger.direct

import android.content.Context
import uzh.scenere.datamodel.trigger.AbstractTrigger
import uzh.scenere.datamodel.trigger.IDirectTrigger

class ButtonTrigger(id: String, ownerId: String): AbstractTrigger(id, ownerId), IDirectTrigger {
    enum class ButtonMode{
        SINGLE, MULTIPLE
    }
}