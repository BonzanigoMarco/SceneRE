package uzh.scenere.views

import android.annotation.SuppressLint
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.ViewGroup
import uzh.scenere.const.Constants.Companion.MATERIAL_100_BLUE
import uzh.scenere.const.Constants.Companion.MATERIAL_100_GREEN
import uzh.scenere.const.Constants.Companion.MATERIAL_100_LIME
import uzh.scenere.const.Constants.Companion.MATERIAL_100_ORANGE
import uzh.scenere.const.Constants.Companion.MATERIAL_100_RED
import uzh.scenere.const.Constants.Companion.MATERIAL_100_TURQUOISE
import uzh.scenere.const.Constants.Companion.MATERIAL_100_VIOLET
import uzh.scenere.const.Constants.Companion.MATERIAL_100_YELLOW
import uzh.scenere.const.Constants.Companion.MATERIAL_700_BLUE
import uzh.scenere.const.Constants.Companion.MATERIAL_700_GREEN
import uzh.scenere.const.Constants.Companion.MATERIAL_700_LIME
import uzh.scenere.const.Constants.Companion.MATERIAL_700_ORANGE
import uzh.scenere.const.Constants.Companion.MATERIAL_700_RED
import uzh.scenere.const.Constants.Companion.MATERIAL_700_TURQUOISE
import uzh.scenere.const.Constants.Companion.MATERIAL_700_VIOLET
import uzh.scenere.const.Constants.Companion.MATERIAL_700_YELLOW
import uzh.scenere.const.Constants.Companion.NEW_LINE_C
import uzh.scenere.const.Constants.Companion.SPACE_C
import uzh.scenere.datamodel.Attribute
import uzh.scenere.datamodel.Object
import uzh.scenere.helpers.StringHelper
import java.io.Serializable
import kotlin.reflect.KClass


@SuppressLint("ViewConstructor")
class SreContextAwareTextView(context: Context, parent: ViewGroup?,val boldWords: ArrayList<String>, val objects: ArrayList<out Serializable>) : SreTextView(context,parent) {
    private val colorArray = if (style == STYLE.DARK)
        arrayOf(MATERIAL_100_RED, MATERIAL_100_VIOLET, MATERIAL_100_BLUE, MATERIAL_100_TURQUOISE, MATERIAL_100_GREEN, MATERIAL_100_LIME, MATERIAL_100_YELLOW, MATERIAL_100_ORANGE) else
        arrayOf(MATERIAL_700_RED, MATERIAL_700_VIOLET, MATERIAL_700_BLUE, MATERIAL_700_TURQUOISE, MATERIAL_700_GREEN, MATERIAL_700_LIME, MATERIAL_700_YELLOW, MATERIAL_700_ORANGE)
    private var objectPointer = 0
    private val objectMap = HashMap<String, Serializable>()
    private val objectLabels = HashMap<String, String>()
    private val placeholder = "XXX"
    private val fontBegin = "<font color='XXX'>"
    private val fontEnd = "</font>"

    fun getObjectLabels(): ArrayList<String>{
        val list = ArrayList<String>()
        for (entry in objectLabels.entries){
            list.add(entry.key)
        }
        return list
    }

    init {
        if (!objects.isEmpty()) {
            addObjects(objects)
        }
        initHighlighting()
    }

    private fun initHighlighting() {
        addTextChangedListener(SreContentAwareTextWatcher(this))
    }

    fun <T: Serializable>addObjects(objects: ArrayList<T>): SreContextAwareTextView {
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

    class SreContentAwareTextWatcher(private val textView: SreContextAwareTextView) : TextWatcher {
        private var ignore = false
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            //NOP
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            if (!ignore) {
                var str = s.toString()
                for (entry in textView.objectLabels.entries) {
                    str = str.replace(entry.key,entry.value)
                }
                for (word in textView.boldWords) {
                    str = str.replaceFirst(word,"<b>$word</b>")
                }
                str = str.replace("\n","<br>").replace("\r","<br>")
                ignore = true
                textView.text = StringHelper.fromHtml(str)
                ignore = false
            }
        }

        override fun afterTextChanged(s: Editable?) {
            //NOP
        }
    }
}