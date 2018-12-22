package uzh.scenere.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import android.widget.TextView

class SensorHelper private constructor(context: Context) {
    private val sensorCount = 40
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val registeredSensors: HashMap<String,Sensor> = HashMap()
    private val sensorArray: ArrayList<Sensor> = ArrayList(sensorCount)
    private var eventListener: EventListener? = null
    //https://stackoverflow.com/questions/40398072/singleton-with-parameter-in-kotlin

    companion object {
        // Volatile: writes to this field are immediately made visible to other threads.
        @Volatile private var instance : SensorHelper? = null

        fun getInstance(context: Context): SensorHelper {
            return when {
                instance != null -> instance!!
                else -> synchronized(this) {
                    if (instance == null) {
                        instance = SensorHelper(context)
                    }
                    instance!!
                }
            }
        }
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

    public fun getSensorArray(): ArrayList<Sensor>{
        if (sensorArray.isEmpty()){
            createSensorArray()
        }
        return sensorArray
    }

    var intervals = arrayListOf<Int>(
            SensorManager.SENSOR_DELAY_FASTEST,
            SensorManager.SENSOR_DELAY_GAME,
            SensorManager.SENSOR_DELAY_UI,
            SensorManager.SENSOR_DELAY_NORMAL) //Documentation
    public fun register(sensor: Sensor, outputText: TextView, filtered: Boolean){
        eventListener = eventListener ?: EventListener(outputText)
        eventListener?.filteredSensor = if (filtered) sensor else null
        registeredSensors[sensor.name] = sensor
        sensorManager.registerListener(eventListener,sensor,SensorManager.SENSOR_DELAY_GAME)
    }
    public fun unregister(){
        registeredSensors.clear()
        sensorManager.unregisterListener(eventListener)
    }

    public class EventListener(private val outputText: TextView) : SensorEventListener{
        var filteredSensor: Sensor? = null

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            //NOP
        }

        override fun onSensorChanged(event: SensorEvent?) {
            if (event==null || (filteredSensor!=null && event.sensor != filteredSensor)) return
            var valText = event.sensor.name+" Values:\n"
            for (value in event.values) valText += (value.toString()+"\n")
            outputText.text = valText
        }
    }
}