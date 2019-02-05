package uzh.scenere.helpers

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.text.*
import android.text.style.MetricAffectingSpan
import java.io.Serializable

class StringHelper{
    companion object { //Static Reference
        fun concatTokens(delimiter: String, obj: List<Serializable>): String{
            var conc = ""
            for (o in obj){
                conc += if (o is String){
                    o+delimiter
                }else{
                    o.toString()+delimiter
                }
            }
            return conc.substring(0,conc.length-delimiter.length)
        }

        fun concatTokens(delimiter: String, vararg obj: Serializable): String{
            var conc = ""
            for (o in obj){
                conc += if (o is String){
                    o+delimiter
                }else{
                    o.toString()+delimiter
                }
            }
            return conc.substring(0,conc.length-delimiter.length)
        }
        fun nvl(value: String?, valueIfNull: String): String {
            return value ?: valueIfNull; // Elvis Expression of Java number==null?valueIfNull:number
        }

        fun lookupOrEmpty(id: Int?, applicationContext: Context?): CharSequence? {
            return if (id==null) "" else applicationContext?.resources?.getString(id)
        }

        fun hasText(text: Editable?): Boolean {
            if (text == null) return false
            return hasText(text.toString())
        }

        fun hasText(text: String?): Boolean {
            return (text != null && text.isNotEmpty())
        }

        fun hasText(text: CharSequence?): Boolean {
            return (text != null && text.isNotEmpty())
        }

        fun extractNameFromClassString(className: String): String{
            val split = className.split(".")
            return split[split.size-1]
        }

        fun styleString(spannedString: SpannedString, typeface: Typeface?): SpannableString {
            val annotations = spannedString.getSpans(0, spannedString.length, android.text.Annotation::class.java)
            val spannableString = SpannableString(spannedString)
            for (annotation in annotations) {
                if (annotation.key == "font") {
                    val fontName = annotation.value
                    if (fontName == "font_awesome") {
                        spannableString.setSpan(CustomTypefaceSpan(typeface),
                                spannedString.getSpanStart(annotation),
                                spannedString.getSpanEnd(annotation),
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }
                }
            }
            return spannableString
        }
        class CustomTypefaceSpan(private val typeface: Typeface?) : MetricAffectingSpan() {

            override fun updateDrawState(drawState: TextPaint) {
                apply(drawState)
            }

            override fun updateMeasureState(paint: TextPaint) {
                apply(paint)
            }

            private fun apply(paint: Paint) {
                val oldTypeface = paint.typeface
                val oldStyle = if (oldTypeface != null) oldTypeface.style else 0
                val fakeStyle = oldStyle and typeface!!.style.inv()

                if (fakeStyle and Typeface.BOLD != 0) {
                    paint.isFakeBoldText = true
                }

                if (fakeStyle and Typeface.ITALIC != 0) {
                    paint.textSkewX = -0.25f
                }

                paint.typeface = typeface
            }

        }
    }


}