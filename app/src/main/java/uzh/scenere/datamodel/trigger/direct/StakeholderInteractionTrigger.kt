package uzh.scenere.datamodel.trigger.direct

import uzh.scenere.datamodel.trigger.AbstractTrigger
import uzh.scenere.datamodel.trigger.IDirectTrigger
import java.util.*

class StakeholderInteractionTrigger(id: String?, previousId: String?, pathId: String) : AbstractTrigger(id
        ?: UUID.randomUUID().toString(), previousId, pathId), IDirectTrigger {
    var text: String? = null
    var interactedStakeholderId: String? = null

    fun withText(text: String?): StakeholderInteractionTrigger {
        this.text = text
        return this
    }

    fun withInteractedStakeholderId(interactedStakeholderId: String?): StakeholderInteractionTrigger {
        this.interactedStakeholderId = interactedStakeholderId
        return this
    }

}