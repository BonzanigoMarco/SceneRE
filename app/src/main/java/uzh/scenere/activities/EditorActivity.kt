package uzh.scenere.activities

import android.content.res.Configuration
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import kotlinx.android.synthetic.main.activity_editor.*
import kotlinx.android.synthetic.main.scroll_holder.*
import uzh.scenere.R
import java.util.*
import kotlin.collections.HashMap


class EditorActivity : AbstractManagementActivity() {
    override fun isInEditMode(): Boolean {
        return false
    }

    override fun isInViewMode(): Boolean {
        return true
    }

    override fun resetEditMode() {
        //NOP
    }

    override fun createEntity() {
        //NOP
    }

    override fun getConfiguredInfoString(): Int {
        return R.string.icon_explain_editor
    }

    override fun getConfiguredLayout(): Int {
        return R.layout.activity_editor
    }

    private val explanationMap: HashMap<Int,Map.Entry<Int,Int>> = HashMap<Int,Map.Entry<Int,Int>>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        holder_scroll.setBackgroundColor(Color.WHITE)
        populateExplanationMap()
        execAdaptToOrientationChange()
    }

    private fun populateExplanationMap() {
        explanationMap[R.array.spinner_steps] = AbstractMap.SimpleEntry(R.string.explanation_steps_title,R.string.explanation_steps_content)
        explanationMap[R.array.spinner_triggers_communication] = AbstractMap.SimpleEntry(R.string.explanation_communication_triggers_title,R.string.explanation_communication_triggers_content)
        explanationMap[R.array.spinner_triggers_direct] = AbstractMap.SimpleEntry(R.string.explanation_direct_triggers_title,R.string.explanation_direct_triggers_content)
        explanationMap[R.array.spinner_triggers_indirect] = AbstractMap.SimpleEntry(R.string.explanation_indirect_triggers_title,R.string.explanation_indirect_triggers_content)
        explanationMap[R.array.spinner_triggers_sensor] = AbstractMap.SimpleEntry(R.string.explanation_sensor_triggers_title,R.string.explanation_sensor_triggers_content)
    }


    fun onToolSelectionClicked(v: View) {
        when(v.id){
            R.id.editor_button_steps -> {updateSpinner(R.array.spinner_steps)}
            R.id.editor_button_triggers_communication -> {updateSpinner(R.array.spinner_triggers_communication)}
            R.id.editor_button_triggers_direct -> {updateSpinner(R.array.spinner_triggers_direct)}
            R.id.editor_button_triggers_indirect -> {updateSpinner(R.array.spinner_triggers_indirect)}
            R.id.editor_button_triggers_sensor -> {updateSpinner(R.array.spinner_triggers_sensor)}
            else -> {}
        }
    }

    fun onAddButtonClicked(v: View){
        //Do magic
    }

    private fun updateSpinner(arrayResource: Int) {
        showInformation(explanationMap[arrayResource]?.key,explanationMap[arrayResource]?.value)
        val spinnerArrayAdapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, resources.getStringArray(arrayResource))
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) // The drop down view
        editor_spinner_selection.adapter = spinnerArrayAdapter
        editor_spinner_selection.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                editor_button_add.isEnabled = !(editor_spinner_selection.selectedItem as String).contains("[")
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                //NOP
            }
        };
    }

    private var handlerId = 0L
    private fun showInformation(titleId: Int?, contentId: Int?){
        if (titleId == null || contentId == null){
            return
        }
        execMorphInfoBar(InfoState.NORMAL)
        holder_text_info_title.text = resources.getString(titleId)
        holder_text_info_content.text = resources.getString(contentId)
        val localHandlerId = Random().nextLong()
        handlerId = localHandlerId
        Handler().postDelayed({
            if (localHandlerId == handlerId){
                holder_text_info_title.text = null
                holder_text_info_content.text = null
                execMorphInfoBar(InfoState.MINIMIZED)
            }
        }, 5000)
    }

    override fun execAdaptToOrientationChange() {
        if (resources.configuration.orientation == ORIENTATION_LANDSCAPE){
            contentDefaultMaxLines = 2
        }else if (resources.configuration.orientation == ORIENTATION_PORTRAIT){
            contentDefaultMaxLines = 4
        }
    }
}