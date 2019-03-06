package uzh.scenere.views

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import uzh.scenere.R
import uzh.scenere.const.Constants.Companion.FRACTION
import uzh.scenere.const.Constants.Companion.NEW_LINE
import uzh.scenere.const.Constants.Companion.NOTHING
import uzh.scenere.datamodel.StatisticArrayList
import uzh.scenere.datamodel.Walkthrough
import uzh.scenere.helpers.DipHelper
import uzh.scenere.helpers.StringHelper
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

@SuppressLint("ViewConstructor")
class StepAnalyticsLayout(context: Context, vararg  val walkthroughs: Walkthrough) : LinearLayout(context) {

    enum class ScenarioMode {
        STEPS, COMMENTS
    }

    private lateinit var comments: HashMap<String,ArrayList<CommentWrapper>>
    private lateinit var sortedStepList: ArrayList<String>
    private var activeStepName: String = NOTHING

    private var stepPointer: Int = 0

    fun nextStep(){
        if (!sortedStepList.isEmpty()) {
            if (stepPointer < sortedStepList.size-1) {
                stepPointer++
            } else {
                stepPointer = 0
            }
        }
        visualizeOverview()
    }
    fun previousStep(){
        if (!sortedStepList.isEmpty()){
            if (stepPointer>0){
                stepPointer--
            }else{
                stepPointer = sortedStepList.size-1
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
        comments = HashMap()
        sortedStepList = ArrayList()
        val sortingMap = TreeMap<Float,String>()
        for (walkthrough in walkthroughs){
            walkthrough.load()
            val author = Walkthrough.WalkthroughProperty.WT_OWNER.get(String::class)
            val timestamp = Walkthrough.WalkthroughProperty.TIMESTAMP.getDisplayText()
            for (stepId in Walkthrough.WalkthroughProperty.STEP_ID_LIST.getAll(String::class)){
                if (!comments.contains(stepId)){
                    comments[stepId] = ArrayList()
                }
                val stepNumber = Walkthrough.WalkthroughStepProperty.STEP_NUMBER.get(stepId, Int::class)
                val stepTitle = Walkthrough.WalkthroughStepProperty.STEP_TITLE.get(stepId, String::class)
                val stepTime = Walkthrough.WalkthroughStepProperty.STEP_TIME.get(stepId, Long::class)
                val stepComments = Walkthrough.WalkthroughStepProperty.STEP_COMMENTS.getAll(stepId, String::class)
                val stepText = Walkthrough.WalkthroughStepProperty.STEP_TEXT.get(stepId, String::class)
                val wrapper = CommentWrapper(author,stepTime,stepComments,stepTitle,stepText,stepNumber,timestamp)
                comments[stepId]?.add(wrapper)
                addStepToSortingMap(sortingMap,stepId,stepNumber.toFloat())
            }
        }
        //SORTING
        for (entry in sortingMap.entries){
            sortedStepList.add(entry.value)
        }
        //CLEANUP
        val removalList = ArrayList<String>()
        for (entry in comments){
            var remove = true
            for (wrapper in entry.value){
                if (!wrapper.comments.isNullOrEmpty()){
                    remove = false
                }
            }
            if (remove){
                removalList.add(entry.key)
            }
        }
        for (stepId in removalList){
            sortedStepList.remove(stepId)
            comments.remove(stepId)
        }
        visualizeOverview()
    }

    private fun addStepToSortingMap(sortingMap: TreeMap<Float, String>, stepId: String, stepNumber: Float) {
        val fetchedStepId = sortingMap.get(stepNumber)
        if (fetchedStepId != null && fetchedStepId != stepId){
            addStepToSortingMap(sortingMap,stepId,stepNumber.plus(FRACTION))
        }else{
            sortingMap[stepNumber] = stepId
        }
    }

    private fun visualizeOverview() {
        removeAllViews()
        val stepId = sortedStepList[stepPointer]
        val wrapperList = comments[stepId]
        if (!wrapperList.isNullOrEmpty()){
            val commentWrapper = wrapperList.first()
            activeStepName = commentWrapper.title
            addView(createLine(context.getString(R.string.analytics_step_number),false, stepPointer.toString()))
            addView(createLine(context.getString(R.string.analytics_step_title),false, commentWrapper.title))
            addView(createLine(context.getString(R.string.analytics_step_text),false, commentWrapper.text))
            addView(createLine(context.getString(R.string.analytics_step_runs),false, wrapperList.size.toString()))
            val times = StatisticArrayList<Long>()
            for (wrapper in wrapperList) {
                times.add(wrapper.time)
            }
            val avg = times.avg()
            addView(createLine(context.getString(R.string.analytics_avg_time),false, context.getString(R.string.analytics_x_seconds,avg)))
            for (wrapper in wrapperList){
                if (!wrapper.comments.isNullOrEmpty()){
                    val comments = StringHelper.concatList(NEW_LINE,wrapper.comments)
                    addView(createLine(context.getString(R.string.analytics_comment_of,wrapper.author),false, comments,SreTextView.TextStyle.MEDIUM))
                    addView(createLine(context.getString(R.string.analytics_comment_timestamp),false, wrapper.timestamp))
                }
            }
        }
    }

    private fun createLine(labelText: String, multiLine: Boolean = false, presetValue: String? = null, specialStyle: SreTextView.TextStyle = SreTextView.TextStyle.DARK): View? {
        val layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        val wrapper = LinearLayout(context)
        wrapper.layoutParams = layoutParams
        wrapper.weightSum = 2f
        wrapper.orientation = if (multiLine) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
        val label = SreTextView(context,wrapper,context.getString(R.string.label, labelText))
        val weightedParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
        weightedParams.weight = 1f
        label.setWeight(weightedParams)
        val text = SreTextView(context,wrapper,presetValue,specialStyle)
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

    fun getStepCount(): Int {
        return sortedStepList.size
    }

    fun getActiveStepName(): String {
        return activeStepName
    }

    private class CommentWrapper(val author: String, val time: Long, val comments: List<String>, val title: String, val text: String,val stepNumber: Int, val timestamp: String)
}