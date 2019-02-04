package uzh.scenere.views

import android.content.Context
import android.text.Html
import android.text.SpannableString
import android.text.Spanned
import android.text.TextUtils
import android.widget.ArrayAdapter
import android.widget.MultiAutoCompleteTextView
import uzh.scenere.R
import uzh.scenere.datamodel.Attribute
import uzh.scenere.datamodel.Object
import java.io.Serializable


class SreMultiAutoCompleteTextView(context: Context, val objects: ArrayList<Serializable>) : MultiAutoCompleteTextView(context) {

    private val objectMap = HashMap<String, Serializable>()
    private val objectLabels = ArrayList<String>()

    init {
        if (!objects.isEmpty()) {
            when (objects[0]) {
                is Object -> {
                    for (obj in objects) {
                        val name = (obj as Object).name
                        objectMap[name] = obj
                        objectLabels.add(name)
                    }
                }
                is Attribute -> {
                    for (obj in objects) {
                        val key = (obj as Attribute).key
                        if (key != null) {
                            objectMap[key] = obj
                            objectLabels.add(key)
                        }
                    }
                }

                is String -> {
                    for (obj in objects) {
                        objectMap[(obj as String)] = obj
                        objectLabels.add(obj)
                    }
                }
                else -> throw ClassNotFoundException("No Mapping for this Class available")
            }
            initSuggestions(context)
        }
    }

    private fun initSuggestions(context: Context) {
        val adapter = ArrayAdapter<String>(context, R.layout.suggestion_dropdown, objectLabels)
        setAdapter(adapter)
        threshold = 1
        setTokenizer(SreSpaceTokenizer())
        setOnItemClickListener { _, _, _, _ ->
            colorizeObjects(this)
        }
    }


    val fontBegin = "<font color='red'>"
    val fontEnd = "</font>"
    private fun colorizeObjects(textView: SreMultiAutoCompleteTextView) {
        var editText = textView.text.toString().replace(fontBegin, "").replace(fontEnd, "").replace("\n","<br/>")
        for (labels in textView.objectLabels) {
            editText = editText.replace(labels, "$fontBegin$labels$fontEnd")
        }
        textView.setText(Html.fromHtml(editText))
        textView.setSelection(textView.text.length)
    }


    class SreSpaceTokenizer : Tokenizer {
        override fun findTokenStart(text: CharSequence, cursor: Int): Int {
            var i = cursor

            while (i > 0 && (text[i - 1] != ' ' && text[i - 1] != '\n')) {
                i--
            }
            while (i < cursor && (text[i] == ' ' || text[i] == '\n')) {
                i++
            }
            return i
        }

        override fun findTokenEnd(text: CharSequence, cursor: Int): Int {
            var i = cursor
            val len = text.length

            while (i < len) {
                if (text[i] == ' ') {
                    return i
                } else {
                    i++
                }
            }

            return len
        }

        override fun terminateToken(text: CharSequence): CharSequence {
            var i = text.length

            while (i > 0 && text[i - 1] == ' ') {
                i--
            }

            if (i > 0 && text[i - 1] == ' ') {
                return text
            } else {
                if (text is Spanned) {
                    val sp = SpannableString(text.toString() + " ")
                    TextUtils.copySpansFrom(text, 0, text.length,
                            Any::class.java, sp, 0)
                    return sp
                } else {
                    return text.toString() + " "
                }
            }
        }
    }
}