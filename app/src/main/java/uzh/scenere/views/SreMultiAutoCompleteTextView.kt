package uzh.scenere.views

import android.annotation.SuppressLint
import android.content.Context
import android.text.*
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.MultiAutoCompleteTextView
import uzh.scenere.R
import uzh.scenere.const.Constants.Companion.MATERIAL_100_BLUE
import uzh.scenere.const.Constants.Companion.MATERIAL_100_GREEN
import uzh.scenere.const.Constants.Companion.MATERIAL_100_LIME
import uzh.scenere.const.Constants.Companion.MATERIAL_100_ORANGE
import uzh.scenere.const.Constants.Companion.MATERIAL_100_RED
import uzh.scenere.const.Constants.Companion.MATERIAL_100_TURQUOISE
import uzh.scenere.const.Constants.Companion.MATERIAL_100_VIOLET
import uzh.scenere.const.Constants.Companion.MATERIAL_100_YELLOW
import uzh.scenere.const.Constants.Companion.NEW_LINE
import uzh.scenere.const.Constants.Companion.NEW_LINE_C
import uzh.scenere.const.Constants.Companion.NOTHING
import uzh.scenere.const.Constants.Companion.SPACE_C
import uzh.scenere.datamodel.Attribute
import uzh.scenere.datamodel.Object
import java.io.Serializable
import kotlin.reflect.KClass


@SuppressLint("ViewConstructor")
class SreMultiAutoCompleteTextView(context: Context, val objects: ArrayList<Serializable>) : MultiAutoCompleteTextView(context) {
    private val colorArray = arrayOf(MATERIAL_100_RED, MATERIAL_100_VIOLET, MATERIAL_100_BLUE, MATERIAL_100_TURQUOISE, MATERIAL_100_GREEN, MATERIAL_100_LIME, MATERIAL_100_YELLOW, MATERIAL_100_ORANGE)
    private var objectPointer = 0
    private val objectMap = HashMap<String, Serializable>()
    private val objectLabels = HashMap<String, String>()

    private fun getObjectLabels(): ArrayList<String>{
        val list = ArrayList<String>()
        for (entry in objectLabels.entries){
            list.add(entry.key)
        }
        return list
    }

    init {
        if (!objects.isEmpty()) {
            addObjects(objects)
            initSuggestions(context)
        }
    }

    private fun initSuggestions(context: Context) {
        val adapter = ArrayAdapter<String>(context, R.layout.suggestion_dropdown, getObjectLabels())
        setAdapter(adapter)
        threshold = 1
        setTokenizer(SreSpaceTokenizer())
        setOnItemClickListener { _, _, _, _ ->
            colorizeObjects(this, false)
        }
        addTextChangedListener(SreAutoCompleteTextWatcher(this))
    }

    private fun getLastToken(): String? {
        val split = text.toString().replace(NEW_LINE_C, SPACE_C).split(SPACE_C)
        return split[split.size - 1]
    }

    fun addObjects(objects: ArrayList<Serializable>): SreMultiAutoCompleteTextView {
        if (objectPointer >= colorArray.size){
            Log.e("AutoComplete","Not enough Colors defined.")
            return this
        }
        when (objects[0]) {
            is Object -> {
                for (obj in objects) {
                    val name = (obj as Object).name
                    objectMap[name] = obj
                    objectLabels[name] = fontBegin.replace(placeholder, colorArray[objectPointer]) + name + fontEnd
                }
            }
            is Attribute -> {
                for (obj in objects) {
                    val key = (obj as Attribute).key
                    if (key != null) {
                        objectMap[key] = obj
                        objectLabels[key] = fontBegin.replace(placeholder, colorArray[objectPointer]) + key + fontEnd
                    }
                }
            }

            is String -> {
                for (obj in objects) {
                    objectMap[(obj as String)] = obj
                    objectLabels[obj] = fontBegin.replace(placeholder, colorArray[objectPointer]) + obj + fontEnd
                }
            }
            else -> throw ClassNotFoundException("No Mapping for this Class available")
        }
        objectPointer++
        return this
    }

    fun <T: Serializable> getContextObjects(classFilter: KClass<T>? = null): ArrayList<Serializable> {
        val list = ArrayList<Serializable>()
        for (entry in objectMap){
            if (classFilter != null && entry.value::class != classFilter){
                continue
            }
            if (text.toString().contains(entry.key)){
                list.add(entry.value)
            }
        }
        return list
    }

    class SreAutoCompleteTextWatcher(private val textView: SreMultiAutoCompleteTextView) : TextWatcher {
        private var ignore = false
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            //NOP
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            if (!ignore) {
                val lastToken = textView.getLastToken()
                if (textView.objectLabels[lastToken] != null){
                    ignore = true
                    colorizeObjects(textView, before == count)
                    ignore = false
                }
            }
        }

        override fun afterTextChanged(s: Editable?) {
            //NOP
        }

    }

    class SreSpaceTokenizer : Tokenizer {
        override fun findTokenStart(text: CharSequence, cursor: Int): Int {
            var i = cursor

            while (i > 0 && (text[i - 1] != ' ' && text[i - 1] != NEW_LINE_C)) {
                i--
            }
            while (i < cursor && (text[i] == ' ' || text[i] == NEW_LINE_C)) {
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

            return if (i > 0 && text[i - 1] == ' ') {
                text
            } else {
                if (text is Spanned) {
                    val sp = SpannableString(text.toString() + SPACE_C)
                    TextUtils.copySpansFrom(text, 0, text.length,
                            Any::class.java, sp, 0)
                    sp
                } else {
                    text.toString() + SPACE_C
                }
            }
        }
    }

    companion object {
        const val placeholder = "XXX"
        const val fontBegin = "<font color='XXX'>"
        const val fontEnd = "</font>"
        private val fontBeginRegex = "<font color='(.*?)'>".toRegex()
        fun colorizeObjects(textView: SreMultiAutoCompleteTextView, addSpace: Boolean) {
            var editText = textView.text.toString().replace(fontBeginRegex, NOTHING).replace(fontEnd, NOTHING).replace(NEW_LINE, "<br/>")
            for (entry in textView.objectLabels.entries) {
                editText = editText.replace(entry.key, entry.value)
            }
            textView.setText(Html.fromHtml(editText + (if (addSpace) SPACE_C else NOTHING)))
            textView.setSelection(textView.text.length)
        }
    }
}