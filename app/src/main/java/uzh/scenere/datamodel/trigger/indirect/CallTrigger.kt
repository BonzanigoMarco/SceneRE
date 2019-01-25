package uzh.scenere.datamodel.trigger.indirect

import android.content.Context
import uzh.scenere.datamodel.trigger.AbstractTrigger
import uzh.scenere.datamodel.trigger.IIndirectTrigger

class CallTrigger(id: String, previousId: String, pathId: String): AbstractTrigger(id, previousId, pathId), IIndirectTrigger {

    enum class CallMode{
        FIXED, RANDOM
    }
}