package uzh.scenere.views

import android.annotation.SuppressLint
import android.content.Context
import android.support.v4.content.ContextCompat
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import uzh.scenere.R
import uzh.scenere.datamodel.*
import uzh.scenere.helpers.DatabaseHelper
import uzh.scenere.helpers.DipHelper
import uzh.scenere.helpers.NullHelper
import uzh.scenere.helpers.NumberHelper

@SuppressLint("ViewConstructor")
class ScenarioAnalyticLayout(context: Context, vararg  val walkthroughs: Walkthrough) : LinearLayout(context) {

    private val margin = 5f

    enum class ScenarioMode {
        STEPS, COMMENTS
    }

    init {
        orientation = VERTICAL
        val params = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        params.setMargins(DipHelper.get(resources).dip5,0, DipHelper.get(resources).dip15,0)
        layoutParams = params
        createOverview()
    }

    private lateinit var steps: HashMap<Stakeholder,HashMap<Int,StatisticArrayList<String>>>
    private lateinit var stepTimes: HashMap<Stakeholder,HashMap<Int,StatisticArrayList<Long>>>
    private lateinit var walkthroughAmount: HashMap<Stakeholder,Int>
    private var stakeholderPointer: Int = 0

    fun getActiveStakeholder(): Stakeholder?{
        if (walkthroughAmount.size>stakeholderPointer){
            var pointer = 0
            for (stakeholder in walkthroughAmount.entries){
                if (pointer == stakeholderPointer){
                    return stakeholder.key
                }
                pointer++
            }
        }
        return null
    }

    fun getStakeholderCount(): Int{
        return walkthroughAmount.size
    }

    fun nextStakeholder(){
        if (!steps.isEmpty()) {
            if (stakeholderPointer < steps.size - 1) {
                stakeholderPointer++
            } else {
                stakeholderPointer = 0
            }
        }
        visualizeOverview()
    }
    fun previousStakeholder(){
        if (!steps.isEmpty()){
            if (stakeholderPointer>0){
                stakeholderPointer--
            }else{
                stakeholderPointer = steps.size-1
            }
        }
        visualizeOverview()
    }

    private fun createOverview() {
        steps = HashMap()
        stepTimes = HashMap()
        walkthroughAmount = HashMap()
        for (walkthrough in walkthroughs){
            walkthrough.load()
            val stakeholder = DatabaseHelper.getInstance(context).read(walkthrough.stakeholderId, Stakeholder::class, NullHelper.get(Stakeholder::class))
            if (!steps.contains(stakeholder)){
                steps[stakeholder] = HashMap()
                stepTimes[stakeholder] = HashMap()
            }
            walkthroughAmount[stakeholder] = NumberHelper.nvl(walkthroughAmount[stakeholder],0)+1
            for (stepId in Walkthrough.WalkthroughProperty.STEP_ID_LIST.getAll(String::class)){
                val stepNumber = Walkthrough.WalkthroughStepProperty.STEP_NUMBER.get(stepId, Int::class)
                if (!steps[stakeholder]!!.contains(stepNumber)){
                    steps[stakeholder]!![stepNumber] = StatisticArrayList()
                    stepTimes[stakeholder]!![stepNumber] = StatisticArrayList()
                }
                val stepTitle = Walkthrough.WalkthroughStepProperty.STEP_TITLE.get(stepId, String::class)
                val stepTime = Walkthrough.WalkthroughStepProperty.STEP_TIME.get(stepId, Long::class)
//                val stepType = Walkthrough.WalkthroughStepProperty.STEP_TYPE.get(stepId, String::class)
//                val triggerInfo = Walkthrough.WalkthroughStepProperty.TRIGGER_INFO.get(stepId, String::class) //TODO for later
                steps[stakeholder]!![stepNumber]!!.add(stepTitle)
                stepTimes[stakeholder]!![stepNumber]!!.add(stepTime)
            }
        }
        visualizeOverview()
    }

    private fun visualizeOverview() {
        removeAllViews()
        var pointer = 0
        for (entry in steps) { //Iterate Stakeholders
            if (pointer == stakeholderPointer){
                addView(createLine("Stakeholder",false, entry.key.name))
                val walkthroughCount = walkthroughAmount[entry.key]
                addView(createLine("Walkthroughs",false,"$walkthroughCount"))
                for (step in entry.value.entries){
                    addView(createLine("Step #${step.key}", false, step.value.getStatistics()))
                    val times = stepTimes[entry.key]?.get(step.key)
                    if (times != null) {
                        val avg = times.avg()?.div(1000)
                        addView(createLine("Avg Time", false, "$avg Seconds"))
                    }
                }
            }
            pointer++
        }
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

    private var added = false
    fun addTo(viewGroup: ViewGroup): Boolean {
        if (!added){
            viewGroup.addView(this)
            added = true
            return true
        }
        return false
    }


}