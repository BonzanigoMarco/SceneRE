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
import uzh.scenere.datastructures.StatisticArrayList
import uzh.scenere.datamodel.Walkthrough
import uzh.scenere.helpers.*
import java.util.*

@SuppressLint("ViewConstructor")
class ScenarioAnalyticLayout(context: Context, vararg  val walkthroughs: Walkthrough) : LinearLayout(context) {

    enum class ScenarioMode {
        STEPS, COMMENTS
    }

    private lateinit var steps: HashMap<Stakeholder,TreeMap<Int, StatisticArrayList<String>>>
    private lateinit var paths: HashMap<Stakeholder, StatisticArrayList<String>>
    private lateinit var stepTimes: HashMap<Stakeholder,TreeMap<Int, StatisticArrayList<Long>>>
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
            val stakeholder = walkthrough.stakeholder
            if (!steps.contains(stakeholder)){
                steps[stakeholder] = TreeMap()
                stepTimes[stakeholder] = TreeMap()
                paths[stakeholder] = StatisticArrayList()
            }
            walkthroughAmount.addOne(stakeholder)
            val pathSteps = TreeMap<Int,String>()
            var path = NOTHING
            var stepNumber = 1
            for (stepId in Walkthrough.WalkthroughProperty.STEP_ID_LIST.getAll(String::class)){
                if (!steps[stakeholder]!!.contains(stepNumber)){
                    steps[stakeholder]!![stepNumber] = StatisticArrayList()
                    stepTimes[stakeholder]!![stepNumber] = StatisticArrayList()
                }
                val stepTitle = Walkthrough.WalkthroughStepProperty.STEP_TITLE.get(stepId, String::class)
                val stepTime = Walkthrough.WalkthroughStepProperty.STEP_TIME.get(stepId, Long::class)
                val triggerInfoList = Walkthrough.WalkthroughStepProperty.TRIGGER_INFO.getAll(stepId, String::class)
                path += "($stepNumber) $stepTitle".plus(NEW_LINE)
                for (triggerInfo in triggerInfoList){
                    steps[stakeholder]!![stepNumber]!!.add(triggerInfo)
                }
                stepTimes[stakeholder]!![stepNumber]!!.add(stepTime)
                pathSteps[stepNumber] = stepTitle
                stepNumber++
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
                var dark = true
                for (step in entry.value.entries){
                    addView(createLine(context.getString(R.string.analytics_transition_x,step.key), false, step.value.getStatistics(),dark))
                    val times = stepTimes[entry.key]?.get(step.key)
                    if (times != null) {
                        val avg = times.avg()
                        addView(createLine(context.getString(R.string.analytics_avg_time), false, context.getString(R.string.analytics_x_seconds,avg),dark))
                    }
                    dark = !dark
                }
                val paths = paths[entry.key]
                if (paths != null){
                    var counter = 1
                    for (path in paths.getDesc()){
                        val percentage = NumberHelper.floor(paths.getPercentage(path), 2)
                        addView(createLine(context.getString(R.string.analytics_path_version_x,counter).plus(", $percentage%"), false, path,dark))
                        counter++
                        dark = !dark
                    }
                }
            }
            pointer++
        }
    }

    private fun createLine(labelText: String, multiLine: Boolean = false, presetValue: String? = null, dark: Boolean = true): View? {
        val layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        val wrapper = LinearLayout(context)
        wrapper.layoutParams = layoutParams
        wrapper.weightSum = 2f
        wrapper.orientation = if (multiLine) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
        val label = SreTextView(context,wrapper,context.getString(R.string.label, labelText))
        val weightedParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
        weightedParams.weight = 1f
        label.setWeight(weightedParams)
        val text = SreTextView(context,wrapper,presetValue,if (dark) SreTextView.TextStyle.DARK else SreTextView.TextStyle.MEDIUM)
        text.setWeight(1f)
        text.textAlignment = if (multiLine) View.TEXT_ALIGNMENT_TEXT_START else View.TEXT_ALIGNMENT_TEXT_END
        text.setSingleLine(multiLine)
        wrapper.addView(label)
        wrapper.addView(text)
        return wrapper
    }

    fun addTo(viewGroup: ViewGroup): Boolean {
        if (parent == null){
            viewGroup.addView(this)
            return true
        }
        return false
    }
}