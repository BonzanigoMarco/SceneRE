package uzh.scenere.datamodel.transitions

import uzh.scenere.sensors.SensorHelper

interface ISensorTrigger: ITrigger {
    fun initSensors() {
        val sensorListener: SensorHelper = SensorHelper.getInstance(getContext())
        sensorListener.createSensorArray()
    }
}