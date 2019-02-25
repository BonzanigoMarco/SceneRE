package uzh.scenere.helpers

import java.text.SimpleDateFormat
import java.util.*


class DateHelper {

    companion object {
        fun getCurrentTimestamp(pattern: String = "dd-MM-yyyy_HH_mm_ss"): String{
            val date = Date(System.currentTimeMillis())
            val format = SimpleDateFormat(pattern)
            return format.format(date)
        }
    }
}