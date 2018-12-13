package uzh.scenere.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.util.Log

class AbstractSensorListener private constructor(context: Context) {
    private val sensorCount = 40
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
    private val sensorArray: ArrayList<Sensor> = ArrayList(sensorCount)
    //https://stackoverflow.com/questions/40398072/singleton-with-parameter-in-kotlin

    companion object {
        // Volatile: writes to this field are immediately made visible to other threads.
        @Volatile private var instance : AbstractSensorListener? = null

        fun getInstance(context: Context): AbstractSensorListener {
            return when {
                instance != null -> instance!!
                else -> synchronized(this) {
                    if (instance == null) {
                        instance = AbstractSensorListener(context)
                    }
                    instance!!
                }
            }
        }
    }

    public fun get(): String? {
        return sensor?.name
    }

    public fun createSensorArray() : String{
        sensorArray.clear()
        for (i in 0..sensorCount){
            val sensorElement = sensorManager.getDefaultSensor(i)
            if (sensorElement != null){
                Log.i("SensorArray",i.toString()+"-"+sensorElement.name)
                sensorArray.add(sensorElement)
            }
        }
        return sensorArray.size.toString()+" Sensors found and initialized."
    }
}