package uzh.scenere.helpers

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.text.*
import android.text.style.MetricAffectingSpan
import java.io.Serializable
import android.text.Html
import android.os.Build
import android.text.Spanned
import uzh.scenere.const.Constants.Companion.NOTHING


class StringHelper{
    companion object { //Static Reference
        fun <T: Serializable> concatTokens(delimiter: String, obj: List<T>): String{
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

        fun concatList(delimiter: String, obj: List<String>): String{
            var conc = ""
            for (o in obj){
                conc += o+delimiter
            }
            return conc.substring(0,conc.length-delimiter.length)
        }

        fun concatListWithoutIdBrackets(delimiter: String, obj: List<String>): String{
            var conc = ""
            for (o in obj){
                val split = o.split("[")
                conc += split[0]+delimiter
            }
            return conc.substring(0,conc.length-delimiter.length)
        }

        fun concatWithIdBrackets(str: String, id: Int): String{
            return "$str[$id]"
        }

        fun concatWithIdBrackets(str: String, id: String): String{
            return "$str[$id]"
        }

        fun nvl(value: String?, valueIfNull: String): String {
            return if (hasText(value)) value!! else  valueIfNull
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

        fun fromHtml(html: String?): Spanned {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
            } else {
                Html.fromHtml(html)
            }
        }

        fun countSubstrings(string: String, substring: String): Int{
            var count = 0
            var original = string
            var replaced = string
            var finished = false
            do {
                replaced = original.replaceFirst(substring, NOTHING)
                if (replaced != original){
                    original = replaced
                    count++
                }else{
                    finished = true
                }
            }while(!finished)
            return count
        }

        fun substringAfterOccurrences(string: String, substring: String, occurrences: Int): String{
            if (string.length > substring.length) {
                var count = 0
                for (index in 0 until (string.length - substring.length)) {
                    if (string.substring(index,index+substring.length) == substring){
                        count++
                    }
                    if (count >= occurrences){
                        return string.substring(0,index)
                    }
                }
            }
            return string
        }

        fun cutHtmlAfter(html: String, linesMax: Int, hint:String): String{
            val lines = countSubstrings(html, "<br>")
            if (lines > linesMax){
                return substringAfterOccurrences(html,"<br>",linesMax)+"<br>"+hint
            }
            return html
        }
    }
}