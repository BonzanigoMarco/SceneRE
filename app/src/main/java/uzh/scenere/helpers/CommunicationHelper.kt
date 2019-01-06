package uzh.scenere.helpers

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.nfc.NfcAdapter
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.provider.Settings.SettingNotFoundException
import android.util.Log
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import uzh.scenere.const.Constants
import android.support.v4.content.ContextCompat.startActivity




class CommunicationHelper {

    companion object {
        enum class Communications(val label: String) {
            WIFI("Wifi"),
            BLUETOOTH("Bluetooth"),
            NETWORK("Mobile Network (Externally Managed)"),
            NFC("NFC (Externally Managed)"),
            GPS("GPS")
        }

        private const val listenerTag: String = "SRE-GPS-Listener"

        fun check(context: Activity, communications: Communications): Boolean{
            when (communications){
                Communications.NETWORK -> {
                    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                    return try {
                        val cmClass = Class.forName(cm.javaClass.name)
                        val method = cmClass.getDeclaredMethod("getMobileDataEnabled")
                        method.isAccessible = true
                        !isInAirplaneMode(context) && method.invoke(cm) as Boolean
                    } catch (e: Exception) {
                        false
                    }
                }
                Communications.WIFI -> {
                    val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                    return wifiManager.isWifiEnabled
                }
                Communications.BLUETOOTH -> {
                    val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                    return bluetoothAdapter?.isEnabled ?: false
                }
                Communications.NFC -> {
                    val nfcAdapter = NfcAdapter.getDefaultAdapter(context)
                    return nfcAdapter?.isEnabled ?: false
                }
                Communications.GPS -> {
                    return checkGpsInternal(context, false)
                }
            }
        }

        private fun checkGpsInternal(context: Activity, ignoreListener: Boolean = false): Boolean {
            var locationMode = 0
            try {
                locationMode = Settings.Secure.getInt(context.contentResolver, Settings.Secure.LOCATION_MODE)
            } catch (e: SettingNotFoundException) {
                return false
            }
            return if (locationMode != Settings.Secure.LOCATION_MODE_HIGH_ACCURACY) unregisterGpsListener(context) else (ignoreListener || SreLocationListener.exists())
        }

        private fun enable(context: Activity, communications: Communications): Boolean{
            if (check(context,communications)) return true
            when (communications){
                Communications.NETWORK -> {
                    context.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
                }
                Communications.WIFI -> {
                    val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                    wifiManager.isWifiEnabled = true
                }
                Communications.BLUETOOTH -> {
                    val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter() ?: return false
                    bluetoothAdapter.enable()
                }
                Communications.NFC -> {
                    context.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
                }
                Communications.GPS -> {
                    if (checkGpsInternal(context,true)){
                        registerGpsListener(context)
                        return true
                    }
                    val locationRequest = LocationRequest.create()
                    locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                    locationRequest.interval = 30 * 1000
                    locationRequest.fastestInterval = 5 * 1000
                    val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest).setAlwaysShow(true)
                    val locationSettingsResponse = LocationServices.getSettingsClient(context).checkLocationSettings(builder.build())
                    locationSettingsResponse.addOnCompleteListener { task ->
                        try {
                            task.getResult(ApiException::class.java)
                        } catch (exception: ApiException) {
                            when (exception.statusCode) {
                                LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> try {
                                    val resolvable = exception as ResolvableApiException
                                    resolvable.startResolutionForResult(context, Constants.PERMISSION_REQUEST_GPS)
                                } catch (e: java.lang.Exception){
                                    //NOP
                                }
                            }
                        }
                    }
                }
            }
            return true
        }

        private fun disable(context: Activity, communications: Communications): Boolean{
            if (!check(context,communications)) return false
            when (communications){
                Communications.NETWORK -> {
                    context.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
                }
                Communications.WIFI -> {
                    val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                    wifiManager.isWifiEnabled = false
                }
                Communications.BLUETOOTH -> {
                    val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                    bluetoothAdapter?.disable()
                }
                Communications.NFC -> {
                    context.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
                }
                Communications.GPS -> {
                    unregisterGpsListener(context)
                }
            }
            return false
        }

        fun toggle(context: Activity, communications: Communications): Boolean{
            return if (check(context,communications)) disable(context, communications) else enable(context,communications)
        }

        fun getCommunications(): Array<Communications> {
            return Communications.values()
        }

        @SuppressLint("ObsoleteSdkInt")
        fun isInAirplaneMode(context: Context): Boolean{
            return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
                Settings.System.getInt(context.contentResolver,
                        Settings.System.AIRPLANE_MODE_ON, 0) != 0
            } else {
                Settings.Global.getInt(context.contentResolver,
                        Settings.Global.AIRPLANE_MODE_ON, 0) != 0
            }
        }

        @SuppressLint("MissingPermission")
        fun registerGpsListener(activity: Activity): Boolean{
            if (!PermissionHelper.check(activity,PermissionHelper.Companion.PermissionGroups.GPS)){
                return false
            }
            if (!checkGpsInternal(activity,true)){
                return false
            }
            if (SreLocationListener.exists()){
                return true
            }
            val locationManager = activity.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000L, 2f, SreLocationListener.get()) //5s, 2m, parametrize -> TODO
            return true
        }

        private fun unregisterGpsListener(activity: Activity): Boolean{
            if (SreLocationListener.exists()){
                val locationManager = activity.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                locationManager.removeUpdates(SreLocationListener.get())
                SreLocationListener.destroy()
            }
            return false
        }

        fun getMapIntent(): Intent?{
            if (!SreLocationListener.exists()){
                return null
            }
            return SreLocationListener.get().getMapIntent()
        }

        class SreLocationListener private constructor(): LocationListener {
            companion object {
                private var longitude: Double? = null
                private var latitude: Double? = null

                // Volatile: writes to this field are immediately made visible to other threads.
                @Volatile private var instance : SreLocationListener? = null

                fun exists(): Boolean {return instance != null}

                fun get(): SreLocationListener {
                    return when {
                        instance != null -> instance!!
                        else -> synchronized(this) {
                            if (instance == null) {
                                instance = SreLocationListener()
                            }
                            Log.d(listenerTag, "Listener created.")
                            instance!!
                        }
                    }
                }

                fun destroy(): SreLocationListener? {
                    instance = null
                    Log.d(listenerTag, "Listener destroyed.")
                    return instance
                }
            }

            override fun onProviderEnabled(provider: String?) {
                Log.d(listenerTag, "Provider enabled: $provider")
            }

            override fun onProviderDisabled(provider: String?) {
                Log.d(listenerTag, "Provider disabled: $provider")
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
                Log.d(listenerTag, "Status changed, provider: $provider, status: $status")
            }

            override fun onLocationChanged(location: Location?) {
                longitude = location?.longitude
                latitude = location?.latitude
                Log.d(listenerTag, "Lat: $latitude, Lon: $longitude")
            }

            fun getMapIntent(): Intent?{
                if (longitude == null || latitude == null){
                    return null
                }
                return Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/?q=$latitude,$longitude"))
            }
        }
    }
}