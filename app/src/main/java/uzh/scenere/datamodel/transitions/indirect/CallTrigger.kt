package uzh.scenere.datamodel.transitions.indirect

import android.content.Context
import uzh.scenere.datamodel.transitions.AbstractTrigger
import uzh.scenere.datamodel.transitions.IIndirectTrigger

class CallTrigger(context: Context): AbstractTrigger(context), IIndirectTrigger {

    enum class CallMode{
        FIXED, RANDOM
    }
}