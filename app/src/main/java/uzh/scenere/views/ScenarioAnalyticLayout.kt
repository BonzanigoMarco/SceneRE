package uzh.scenere.views

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import uzh.scenere.R
import uzh.scenere.const.Constants.Companion.NEW_LINE
import uzh.scenere.const.Constants.Companion.NOTHING
import uzh.scenere.datamodel.Stakeholder
import uzh.scenere.datamodel.StatisticArrayList
import uzh.scenere.datamodel.Walkthrough
import uzh.scenere.helpers.DatabaseHelper
import uzh.scenere.helpers.DipHelper
import uzh.scenere.helpers.NullHelper
import uzh.scenere.helpers.NumberHelper
import java.util.*

@SuppressLint("ViewConstructor")
class ScenarioAnalyticLayout(context: Context, vararg  val walkthroughs: Walkthrough) : LinearLayout(context) {

    enum class ScenarioMode {
        STEPS, COMMENTS
    }

    private lateinit var steps: HashMap<Stakeholder,HashMap<Int,StatisticArrayList<String>>>
    private lateinit var paths: HashMap<Stakeholder,StatisticArrayList<String>>
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

    init {
        orientation = VERTICAL
        val params = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        params.setMargins(DipHelper.get(resources).dip5,0, DipHelper.get(resources).dip15,0)
        layoutParams = params
        createOverview()
    }

    private fun createOverview() {
        steps = HashMap()
        stepTimes = HashMap()
        walkthroughAmount = HashMap()
        paths = HashMap()
        for (walkthrough in walkthroughs){
            walkthrough.load()
            val stakeholder = DatabaseHelper.getInstance(context).read(walkthrough.stakeholderId, Stakeholder::class, NullHelper.get(Stakeholder::class))
            if (!steps.contains(stakeholder)){
                steps[stakeholder] = HashMap()
                stepTimes[stakeholder] = HashMap()
                paths[stakeholder] = StatisticArrayList()
            }
            walkthroughAmount[stakeholder] = NumberHelper.nvl(walkthroughAmount[stakeholder],0)+1
            val pathSteps = TreeMap<Int,String>()
            var path = NOTHING
            for (stepId in Walkthrough.WalkthroughProperty.STEP_ID_LIST.getAll(String::class)){
                val stepNumber = Walkthrough.WalkthroughStepProperty.STEP_NUMBER.get(stepId, Int::class)
                if (!steps[stakeholder]!!.contains(stepNumber)){
                    steps[stakeholder]!![stepNumber] = StatisticArrayList()
                    stepTimes[stakeholder]!![stepNumber] = StatisticArrayList()
                }
                val stepTitle = Walkthrough.WalkthroughStepProperty.STEP_TITLE.get(stepId, String::class)
                val stepTime = Walkthrough.WalkthroughStepProperty.STEP_TIME.get(stepId, Long::class)
                val triggerInfo = Walkthrough.WalkthroughStepProperty.TRIGGER_INFO.get(stepId, String::class)
                steps[stakeholder]!![stepNumber]!!.add(triggerInfo)
                stepTimes[stakeholder]!![stepNumber]!!.add(stepTime)
                pathSteps.put(stepNumber,stepTitle)
            }
            for (entry in pathSteps.entries){
                path += "(${entry.key}) ${entry.value}".plus(NEW_LINE)
            }
            if (path.length > NEW_LINE.length){
                paths[stakeholder]?.add(path.substring(0,path.length- NEW_LINE.length))
            }
        }
        visualizeOverview()
    }

    private fun visualizeOverview() {
        removeAllViews()
        var pointer = 0
        for (entry in steps) { //Iterate Stakeholders
            if (pointer == stakeholderPointer){
                addView(createLine(context.getString(R.string.literal_stakeholder),false, entry.key.name))
                val walkthroughCount = walkthroughAmount[entry.key]
                addView(createLine(context.getString(R.string.literal_walkthroughs),false,"$walkthroughCount"))
                for (step in entry.value.entries){
                    addView(createLine(context.getString(R.string.analytics_transition_x,step.key), false, step.value.getStatistics()))
                    val times = stepTimes[entry.key]?.get(step.key)
                    if (times != null) {
                        val avg = times.avg()
                        addView(createLine(context.getString(R.string.analytics_avg_time), false, context.getString(R.string.analytics_x_seconds,avg)))
                    }
                }
                val paths = paths[entry.key]
                if (paths != null){
                    var counter = 1
                    for (path in paths.getDesc()){
                        val percentage = NumberHelper.floor(paths.getPercentage(path), 2)
                        addView(createLine(context.getString(R.string.analytics_path_version_x,counter).plus("$percentage%"), false, path))
                        counter++
                    }
                }
            }
            pointer++
        }
    }

    private fun createLine(labelText: String, multiLine: Boolean = false, presetValue: String? = null): View? {
        val layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        val wrapper = LinearLayout(context)
        wrapper.layoutParams = layoutParams
        wrapper.weightSum = 2f
        wrapper.orientation = if (multiLine) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
        val label = SreTextView(context,wrapper,context.getString(R.string.label, labelText))
        val weightedParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
        weightedParams.weight = 1f
        label.setWeight(weightedParams)
        val text = SreTextView(context,wrapper,presetValue,SreTextView.TextStyle.DARK)
        text.setWeight(1f)
        text.textAlignment = if (multiLine) View.TEXT_ALIGNMENT_TEXT_START else View.TEXT_ALIGNMENT_TEXT_END
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