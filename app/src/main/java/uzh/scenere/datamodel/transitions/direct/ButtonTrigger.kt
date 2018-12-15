package uzh.scenere.datamodel.transitions.direct

import android.content.Context
import uzh.scenere.datamodel.transitions.AbstractTrigger
import uzh.scenere.datamodel.transitions.IDirectTrigger

class ButtonTrigger(context: Context): AbstractTrigger(context), IDirectTrigger {
    enum class ButtonMode{
        SINGLE, MULTIPLE
    }
}