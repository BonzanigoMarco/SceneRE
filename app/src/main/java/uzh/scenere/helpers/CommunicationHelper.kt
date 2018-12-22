package uzh.scenere.helpers

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.WifiManager
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.nfc.NfcAdapter
import android.provider.Settings
import android.net.ConnectivityManager
import android.os.Build


class CommunicationHelper {

    companion object {
        enum class Communications(val label: String) {
            WIFI("Wifi"),
            BLUETOOTH("Bluetooth"),
            NETWORK("Mobile Network (Externally Managed)"),
            NFC("NFC (Externally Managed)");
        }
        fun check(context: Context, communications: Communications): Boolean{
            when (communications){
                Communications.NETWORK -> {
                    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                    return try {
                        val cmClass = Class.forName(cm.javaClass.name)
                        val method = cmClass.getDeclaredMethod("getMobileDataEnabled")
                        method.isAccessible = true
                        !isInAirplaneMode(context) && method.invoke(cm) as Boolean
                    } catch (e: Exception) {
                        false;
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
            }
        }
        fun enable(context: Context, communications: Communications): Boolean{
            if (check(context,communications)) return true;
            when (communications){
                Communications.NETWORK -> {
                    context.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS));
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
                    context.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS));
                }
            }
            return true;
        }
        fun disable(context: Context, communications: Communications): Boolean{
            if (!check(context,communications)) return false;
            when (communications){
                Communications.NETWORK -> {
                    context.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS));
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
                    context.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS));
                }
            }
            return false;
        }
        fun toggle(context: Context, communications: Communications): Boolean{
            return if (check(context,communications)) disable(context, communications) else enable(context,communications);
        }

        fun getCommunications(): Array<Communications> {
            return Communications.values()
        }
        @SuppressLint("ObsoleteSdkInt")
        private fun isInAirplaneMode(context: Context): Boolean{
            return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
                Settings.System.getInt(context.contentResolver,
                        Settings.System.AIRPLANE_MODE_ON, 0) != 0;
            } else {
                Settings.Global.getInt(context.contentResolver,
                        Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
            }
        }
    }

}