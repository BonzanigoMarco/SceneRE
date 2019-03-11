package uzh.scenere.datamodel.trigger.indirect

import android.content.Context
import uzh.scenere.datamodel.trigger.AbstractTrigger
import uzh.scenere.datamodel.trigger.IIndirectTrigger
import uzh.scenere.datamodel.trigger.direct.InputTrigger
import uzh.scenere.helpers.NumberHelper
import java.util.*

class SoundTrigger(id: String?, previousId: String?, pathId: String): AbstractTrigger(id ?: UUID.randomUUID().toString(), previousId, pathId), IIndirectTrigger {
    enum class SoundMode{
        LOUD, QUIET
    }
    val soundMode = SoundMode.LOUD

    var text: String? = null
    var dB: Long? = null

    fun withText(text: String?): SoundTrigger {
        this.text = text
        return this
    }

    fun withDb(dB: Long?): SoundTrigger {
        this.dB = dB
        return this
    }

    fun withDb(dB: String?): SoundTrigger {
        this.dB = NumberHelper.safeToNumber(dB,0)
        return this
    }
}