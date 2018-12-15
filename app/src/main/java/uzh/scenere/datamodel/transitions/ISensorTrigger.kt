package uzh.scenere.datamodel.transitions

import android.content.Context
import uzh.scenere.sensors.AbstractSensorListener

interface ISensorTrigger: ITrigger {
    fun initSensors() {
        val sensorListener: AbstractSensorListener = AbstractSensorListener.getInstance(getContext())
        sensorListener.createSensorArray()
    }
}