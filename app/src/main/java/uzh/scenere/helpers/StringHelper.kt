package uzh.scenere.helpers

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.text.*
import android.text.Annotation
import android.text.style.MetricAffectingSpan

class StringHelper{
    companion object { //Static Reference
        fun <T : String> nvl(value: T?, valueIfNull: T): T {
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