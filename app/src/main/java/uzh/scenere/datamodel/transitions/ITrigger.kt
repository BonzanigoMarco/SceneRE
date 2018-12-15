package uzh.scenere.datamodel.transitions

import android.content.Context
import uzh.scenere.sensors.AbstractSensorListener

interface ITrigger {
    fun getContext(): Context
}