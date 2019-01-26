package uzh.scenere.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import uzh.scenere.datamodel.Path
import uzh.scenere.datamodel.steps.AbstractStep
import uzh.scenere.datamodel.steps.StandardStep
import uzh.scenere.datamodel.trigger.AbstractTrigger
import uzh.scenere.datamodel.trigger.direct.ButtonTrigger

@SuppressLint("ViewConstructor")
class WalkthroughPlayLayout(context: Context, private val path: HashMap<Int, Path>?): LinearLayout(context) {

    private val stepLayout: RelativeLayout = RelativeLayout(context)
    private val triggerLayout: RelativeLayout = RelativeLayout(context)

    enum class WalkthroughPlayMode{
        STEP_INDUCED, TRIGGER_INDUCED
    }

    private var first = path?.get(0)?.getStartingPoint()
    private var second = path?.get(0)?.getNextElement(first)

    private val mode: WalkthroughPlayMode = if (first is AbstractStep) WalkthroughPlayMode.STEP_INDUCED else WalkthroughPlayMode.TRIGGER_INDUCED

    init {
        prepareLayout(context)
    }

    private fun prepareLayout(context: Context) {
        removeAllViews()
        stepLayout.removeAllViews()
        triggerLayout.removeAllViews()
        orientation = HORIZONTAL
        weightSum = 10f
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)

        stepLayout.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, 3f)
        triggerLayout.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, 7f)

        stepLayout.setBackgroundColor(Color.BLUE)
        triggerLayout.setBackgroundColor(Color.YELLOW)

        addView(stepLayout)
        addView(triggerLayout)


        when (mode) {
            WalkthroughPlayMode.STEP_INDUCED -> {
                when (first) {
                    is StandardStep -> {
                        val text = TextView(context)
                        val layoutParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
                        layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT)
                        text.layoutParams = layoutParams
                        text.setTextColor(Color.WHITE)
                        text.gravity = Gravity.CENTER
                        text.text = (first as StandardStep).text
                        stepLayout.addView(text)
                    }
                }
                when (second) {
                    is ButtonTrigger -> {
                        val button = Button(context)
                        val layoutParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
                        layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT)
                        button.layoutParams = layoutParams
                        button.gravity = Gravity.CENTER
                        button.text = (second as ButtonTrigger).buttonLabel
                        button.setOnClickListener {
                            first = path?.get(0)?.getNextElement(second)
                            second = path?.get(0)?.getNextElement(first)
                            prepareLayout(context)
                        }
                        triggerLayout.addView(button)
                    }
                }
            }
            WalkthroughPlayMode.TRIGGER_INDUCED -> {

            }
        }
        if (first == null && second == null){ //FINISHED!
            triggerLayout.visibility = GONE
            val text = TextView(context)
            val layoutParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
            layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT)
            text.layoutParams = layoutParams
            text.setTextColor(Color.WHITE)
            text.gravity = Gravity.CENTER
            text.text = "Scenario Finished!"
            //TODO Outro && Intro
            stepLayout.addView(text)
        }
    }
}