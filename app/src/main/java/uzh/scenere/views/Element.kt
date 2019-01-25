package uzh.scenere.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView
import uzh.scenere.helpers.NumberHelper
import java.io.Serializable


@SuppressLint("ViewConstructor")
class Element private constructor(private val elementMode: ElementMode, private val top: Boolean,private  val left: Boolean,private  val right: Boolean,private  val bottom: Boolean, context: Context) : RelativeLayout(context), Serializable {

    enum class ElementMode {
        STEP, TRIGGER;
        fun create(label: String?, top: Boolean,  left: Boolean,  right: Boolean,  bottom: Boolean, context: Context):Element{
            val element = Element(this,top,left,right,bottom,context)
            element.centerElement?.text = label
            return element
        }
    }
    fun getElementMode(): ElementMode? = elementMode

    var connectionTop: TextView? = null
    var connectionLeft: TextView? = null
    var connectionRight: TextView? = null
    var connectionBottom: TextView? = null
    var centerElement: TextView? = null
    var topWrapper: RelativeLayout? = null
    var centerWrapper: RelativeLayout? = null
    var bottomWrapper: RelativeLayout? = null

    private var dpiPaddingSmall = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1f, resources.displayMetrics).toInt()
    private var dpiMargin = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5f, resources.displayMetrics).toInt()
    private var dpiPadding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10f, resources.displayMetrics).toInt()
    private var dpiConnectorWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2f, resources.displayMetrics).toInt()
    private var dpiConnectorHeight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50f, resources.displayMetrics).toInt()

    init {
        //PARENT
        layoutParams = RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        //CHILDREN
        connectionTop = TextView(context)
        connectionLeft = TextView(context)
        connectionRight = TextView(context)
        connectionBottom = TextView(context)
        centerElement = TextView(context)
        //TEXT
        connectionTop?.textSize = 0f
        connectionLeft?.textSize = 0f
        connectionRight?.textSize = 0f
        connectionBottom?.textSize = 0f

        topWrapper = RelativeLayout(context)
        topWrapper?.id = View.generateViewId()
        centerWrapper = RelativeLayout(context)
        centerWrapper?.id = View.generateViewId()
        bottomWrapper = RelativeLayout(context)
        bottomWrapper?.id = View.generateViewId()
        //TOP
        createTop()
        val topParams = RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        topParams.addRule(RelativeLayout.ALIGN_PARENT_START, RelativeLayout.TRUE)
        addView(topWrapper, topParams)
        //CENTER
        createCenter()
        val centerParams = RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        centerParams.addRule(RelativeLayout.BELOW, NumberHelper.nvl(topWrapper?.id, 0))
        addView(centerWrapper, centerParams)
        //BOTTOM
        createBottom()
        val bottomParams = RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        bottomParams.addRule(RelativeLayout.BELOW, NumberHelper.nvl(centerWrapper?.id, 0))
        addView(bottomWrapper, bottomParams)

//        val fadeIn = AnimationUtils.loadAnimation(context, uzh.scenere.R.anim.fade_in)
//        val slideDown = AnimationUtils.loadAnimation(context, uzh.scenere.R.anim.slide_down)
//
//        val s = AnimationSet(true)
//        s.interpolator = AccelerateInterpolator()
//
//        s.addAnimation(slideDown)
//        s.addAnimation(fadeIn)
//        startAnimation(s)
    }


    private fun createTop() {
        // LEFT
        // CENTER
        if (top) {
            connectionTop?.setBackgroundColor(Color.BLACK)
        }
        connectionTop?.setPadding(dpiPaddingSmall, dpiPaddingSmall, dpiPaddingSmall, dpiPaddingSmall)
        val centerParams = LayoutParams(dpiConnectorWidth, dpiConnectorHeight)
        centerParams.addRule(CENTER_HORIZONTAL, TRUE)
        topWrapper?.addView(connectionTop, centerParams)
        // RIGHT
    }

    private fun createCenter() {
        // CENTER
        centerElement?.id = View.generateViewId()
        centerElement?.setBackgroundColor(if (elementMode == ElementMode.STEP) Color.BLUE else Color.YELLOW)
        centerElement?.setTextColor(if (elementMode == ElementMode.STEP) Color.WHITE else Color.BLACK)
        centerElement?.setPadding(dpiPadding, dpiPadding, dpiPadding, dpiPadding)
        val centerParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        centerParams.addRule(CENTER_IN_PARENT, TRUE)
        centerWrapper?.addView(centerElement, centerParams)
        // LEFT
        if (left) {
            connectionLeft?.setBackgroundColor(Color.BLACK)
        }
        connectionLeft?.setPadding(dpiPaddingSmall, dpiPaddingSmall, dpiPaddingSmall, dpiPaddingSmall)
        val leftParams = LayoutParams(dpiConnectorHeight, dpiConnectorWidth)
        leftParams.addRule(START_OF, NumberHelper.nvl(centerElement?.id, 0))
        leftParams.addRule(ALIGN_PARENT_START, TRUE)
        leftParams.addRule(CENTER_VERTICAL, TRUE)
        centerWrapper?.addView(connectionLeft, leftParams)
        // RIGHT
        if (right) {
            connectionRight?.setBackgroundColor(Color.BLACK)
        }
        connectionRight?.setPadding(dpiPaddingSmall, dpiPaddingSmall, dpiPaddingSmall, dpiPaddingSmall)
        val rightParams = LayoutParams(dpiConnectorHeight, dpiConnectorWidth)
        rightParams.addRule(END_OF, NumberHelper.nvl(centerElement?.id, 0))
        rightParams.addRule(ALIGN_PARENT_END, TRUE)
        rightParams.addRule(CENTER_VERTICAL, TRUE)
        centerWrapper?.addView(connectionRight, rightParams)
    }

    private fun createBottom() {
        // CENTER
        connectionBottom?.id = View.generateViewId()
        if (bottom) {
            connectionBottom?.setBackgroundColor(Color.BLACK)
        }
        connectionBottom?.setPadding(dpiPaddingSmall, dpiPaddingSmall, dpiPaddingSmall, dpiPaddingSmall)
        val centerParams = LayoutParams(dpiConnectorWidth, dpiConnectorHeight)
        centerParams.addRule(CENTER_HORIZONTAL, TRUE)
        bottomWrapper?.addView(connectionBottom, centerParams)
        // LEFT
//        val leftButton = IconTextView(context)
//        leftButton.text = resources.getString(R.string.icon_layer)
//        leftButton.setBackgroundColor(Color.WHITE)
//        leftButton.setTextColor(Color.GRAY)
//        leftButton.setPadding(dpiPadding, dpiPadding, dpiPadding, dpiPadding)
//        val leftParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
//        leftParams.setMargins(dpiMargin, dpiMargin, dpiMargin, dpiMargin)
//        leftParams.addRule(CENTER_VERTICAL, TRUE)
//        leftParams.addRule(ALIGN_PARENT_START, TRUE)
//        leftParams.addRule(LEFT_OF, NumberHelper.nvl(connectionBottom?.id, 0))
//        bottomWrapper?.addView(leftButton,leftParams)
//        // RIGHT
//        val rightButton = IconTextView(context)
//        rightButton.text = resources.getString(R.string.icon_path)
//        rightButton.setBackgroundColor(Color.BLACK)
//        rightButton.setTextColor(Color.WHITE)
//        rightButton.setPadding(dpiPadding, dpiPadding, dpiPadding, dpiPadding)
//        val rightParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
//        rightParams.setMargins(dpiMargin, dpiMargin, dpiMargin, dpiMargin)
//        rightParams.addRule(CENTER_VERTICAL, TRUE)
//        rightParams.addRule(ALIGN_PARENT_END, TRUE)
//        rightParams.addRule(RIGHT_OF, NumberHelper.nvl(connectionBottom?.id, 0))
//        bottomWrapper?.addView(rightButton,rightParams)
    }

    fun connectToNext(){
        connectionBottom?.setBackgroundColor(Color.BLACK)
    }
}