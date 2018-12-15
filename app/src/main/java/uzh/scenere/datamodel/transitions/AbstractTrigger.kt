package uzh.scenere.datamodel.transitions

import android.content.Context

abstract class AbstractTrigger(context: Context): ITrigger {
    private var context: Context = context

    override fun getContext(): Context {
        return context
    }
}