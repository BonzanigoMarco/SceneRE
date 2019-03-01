package uzh.scenere.datamodel.trigger.direct

import android.content.Context
import uzh.scenere.datamodel.IElement
import uzh.scenere.datamodel.trigger.AbstractTrigger
import uzh.scenere.datamodel.trigger.IDirectTrigger
import java.util.*

class ButtonTrigger(id: String?, previousId: String?, pathId: String): AbstractTrigger(id ?: UUID.randomUUID().toString(), previousId, pathId), IDirectTrigger {
    var buttonLabel: String? = null
    enum class ButtonMode{
        SINGLE, MULTIPLE
    }

    fun withButtonLabel(label: String?): ButtonTrigger{
        buttonLabel = label
        return this
    }
}