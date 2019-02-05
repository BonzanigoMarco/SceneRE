package uzh.scenere.views

import android.content.Context
import android.support.v4.content.ContextCompat
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import uzh.scenere.R
import uzh.scenere.datamodel.Project
import uzh.scenere.datamodel.Scenario
import uzh.scenere.datamodel.Stakeholder
import uzh.scenere.datamodel.Walkthrough
import uzh.scenere.helpers.DatabaseHelper
import uzh.scenere.helpers.DipHelper
import uzh.scenere.helpers.NullHelper
import uzh.scenere.helpers.className

class WalkthroughAnalyticLayout(context: Context, val walkthrough: Walkthrough, val topLayer: Boolean) : LinearLayout(context) {

    private val textSize = 4f
    private val margin = 5f
    
    init {
        orientation = VERTICAL
        val params = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        params.setMargins(0,0, resources.getDimension(R.dimen.dimScrollbar).toInt(),0)
        layoutParams = params
        createOverview()
    }

    private fun createOverview() {
        val stakeholder = DatabaseHelper.getInstance(context).read(walkthrough.stakeholderId, Stakeholder::class, NullHelper.get(Stakeholder::class))
        val scenario = DatabaseHelper.getInstance(context).read(walkthrough.scenarioId, Scenario::class, NullHelper.get(Scenario::class))
        val project = DatabaseHelper.getInstance(context).read(scenario.projectId, Project::class, NullHelper.get(Project::class))
        if (stakeholder is Stakeholder.NullStakeholder){
            addView(createLine(stakeholder.className(),false,"Unknown"))
            addView(createLine(scenario.className(),false,"Unknown"))
            addView(createLine(project.className(),false,"Unknown"))
        }else{
            addView(createLine(stakeholder.className(),false,stakeholder.name))
            addView(createLine(scenario.className(),false,scenario.title))
            addView(createLine(project.className(),false,project.title))
        }
        walkthrough.load()
        if (topLayer){
            for (property in Walkthrough.WalkthroughProperty.values()) {
                if (property.isStatisticalValue){
                    addView(createLine(property.label,false,property.getDisplayText()))
                }
            }
        }else{
            for (stepId in Walkthrough.WalkthroughProperty.STEP_ID_LIST.getAll(Walkthrough.WalkthroughProperty.STEP_ID_LIST.type)){
                for (property in (Walkthrough.WalkthroughStepProperty.values())) {
                    if (property.isStatisticalValue){
                        addView(createLine(property.label,false,property.getDisplayText(stepId as String)))
                    }
                }
            }
        }
        //TODO, Step Details
        //TODO Delete
        //TODO headless walkthroughs with diff color
    }

    private fun createLine(labelText: String, multiLine: Boolean = false, presetValue: String? = null, data: Any? = null, executable: (() -> Unit)? = null): View? {
        val layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        val childParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        DipHelper.get(resources).setMargin(childParams,margin)
        val wrapper = LinearLayout(context)
        wrapper.layoutParams = layoutParams
        wrapper.weightSum = 2f
        wrapper.orientation = if (multiLine) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
        val label = TextView(context)
        label.text = context.getString(R.string.label, labelText)
        label.textSize = DipHelper.get(resources).dip4.toFloat()
        label.layoutParams = childParams
        val text = TextView(context)
        text.setBackgroundColor(ContextCompat.getColor(context, R.color.srePrimary))
        DipHelper.get(resources).setPadding(text,margin)
        text.textAlignment = if (multiLine) View.TEXT_ALIGNMENT_TEXT_START else View.TEXT_ALIGNMENT_TEXT_END
        text.layoutParams = childParams
        text.textSize = DipHelper.get(resources).dip4.toFloat()
        text.text = presetValue
        text.setSingleLine(multiLine)
        wrapper.addView(label)
        wrapper.addView(text)
        return wrapper
    }

    private fun createEditLine(labelText: String, multiLine: Boolean = false, presetValue: String? = null, data: Any? = null, executable: (() -> Unit)? = null): View? {
        val layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        val childParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        DipHelper.get(resources).setMargin(childParams,margin)
        val wrapper = LinearLayout(context)
        wrapper.layoutParams = layoutParams
        wrapper.weightSum = 2f
        wrapper.orientation = if (multiLine) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
        val label = TextView(context)
        label.text = context.getString(R.string.label, labelText)
        label.textSize = DipHelper.get(resources).dip4.toFloat()
        label.layoutParams = childParams
        val input = EditText(context)
        input.setBackgroundColor(ContextCompat.getColor(context, R.color.srePrimary))
        DipHelper.get(resources).setPadding(input,margin)
        input.textAlignment = if (multiLine) View.TEXT_ALIGNMENT_TEXT_START else View.TEXT_ALIGNMENT_TEXT_END
        input.layoutParams = childParams
        input.textSize = DipHelper.get(resources).dip4.toFloat()
        input.hint = labelText
        input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        input.setText(presetValue)
        input.setSingleLine(multiLine)
        wrapper.addView(label)
        wrapper.addView(input)
        return wrapper
    }


}