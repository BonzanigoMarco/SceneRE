package uzh.scenere.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.support.v4.content.ContextCompat
import android.text.Spanned
import android.util.TypedValue
import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView
import uzh.scenere.R
import uzh.scenere.datamodel.IElement
import uzh.scenere.datamodel.steps.AbstractStep
import uzh.scenere.datamodel.trigger.direct.IfElseTrigger
import uzh.scenere.helpers.NumberHelper
import uzh.scenere.views.SreTextView.TextStyle.DARK
import uzh.scenere.views.SreTextView.TextStyle.LIGHT
import java.io.Serializable


@SuppressLint("ViewConstructor")
class Element (context: Context, private var element: IElement, private val top: Boolean, private  val left: Boolean, private  val right: Boolean, private  val bottom: Boolean) : RelativeLayout(context), Serializable {

    var editButton: IconButton? = null
    var deleteButton: IconButton? = null
    var addButton: IconButton? = null
    var removeButton: IconButton? = null
    var pathSpinner: SreSpinner? = null
    private var connectionTop: TextView? = null
    private var connectionLeft: TextView? = null
    private var connectionRight: TextView? = null
    private var connectionBottom: TextView? = null
    private var centerElement: SreTextView? = null
    private var topWrapper: RelativeLayout? = null
    private var centerWrapper: RelativeLayout? = null
    private var bottomWrapper: RelativeLayout? = null

    private var dpiPaddingSmall = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1f, resources.displayMetrics).toInt()
    private var dpiMargin = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5f, resources.displayMetrics).toInt()
    private var dpiPadding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10f, resources.displayMetrics).toInt()
    private var dpiConnectorWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2f, resources.displayMetrics).toInt()
    private var dpiConnectorHeight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50f, resources.displayMetrics).toInt()

    init {
        //PARENT
        layoutParams = RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        //WRAPPER
        topWrapper = RelativeLayout(context)
        topWrapper?.id = View.generateViewId()
        centerWrapper = RelativeLayout(context)
        centerWrapper?.id = View.generateViewId()
        bottomWrapper = RelativeLayout(context)
        bottomWrapper?.id = View.generateViewId()
        //CHILDREN
        connectionTop = TextView(context)
        connectionLeft = TextView(context)
        connectionRight = TextView(context)
        connectionBottom = TextView(context)
        centerElement = SreTextView(context,centerWrapper,null,if (isStep()) DARK else LIGHT)
        //TEXT
        connectionTop?.textSize = 0f
        connectionLeft?.textSize = 0f
        connectionRight?.textSize = 0f
        connectionBottom?.textSize = 0f
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
        connectionTop?.id = View.generateViewId()
        // LEFT
        editButton = IconButton(context, topWrapper, R.string.icon_edit,dpiConnectorHeight,dpiConnectorHeight).addRule(RelativeLayout.LEFT_OF, connectionTop!!.id).addRule(CENTER_VERTICAL, TRUE)
        topWrapper?.addView(editButton)
        // CENTER
        if (top) {
            connectionTop?.setBackgroundColor(Color.BLACK)
        }
        val centerParams = LayoutParams(dpiConnectorWidth, dpiConnectorHeight+NumberHelper.nvl(editButton?.getMargin(),0)*2)
        centerParams.addRule(CENTER_HORIZONTAL, TRUE)
        connectionTop?.layoutParams = centerParams
        topWrapper?.addView(connectionTop)
        // RIGHT
        deleteButton = IconButton(context,topWrapper, R.string.icon_delete,dpiConnectorHeight,dpiConnectorHeight).addRule(RelativeLayout.RIGHT_OF, connectionTop!!.id).addRule(CENTER_VERTICAL, TRUE)
        topWrapper?.addView(deleteButton)
    }

    private fun createCenter() {
        // CENTER
        centerElement?.id = View.generateViewId()
        centerElement?.addRule(CENTER_IN_PARENT, TRUE)
        centerWrapper?.addView(centerElement)
        // LEFT
        if (left) {
            connectionLeft?.setBackgroundColor(Color.BLACK)
        }
        val leftParams = LayoutParams(dpiConnectorHeight, dpiConnectorWidth)
        leftParams.addRule(START_OF, NumberHelper.nvl(centerElement?.id, 0))
        leftParams.addRule(ALIGN_PARENT_START, TRUE)
        leftParams.addRule(CENTER_VERTICAL, TRUE)
        centerWrapper?.addView(connectionLeft, leftParams)
        // RIGHT
        if (right) {
            connectionRight?.setBackgroundColor(Color.BLACK)
        }
        val rightParams = LayoutParams(dpiConnectorHeight, dpiConnectorWidth)
        rightParams.addRule(END_OF, NumberHelper.nvl(centerElement?.id, 0))
        rightParams.addRule(ALIGN_PARENT_END, TRUE)
        rightParams.addRule(CENTER_VERTICAL, TRUE)
        centerWrapper?.addView(connectionRight, rightParams)
    }

    private fun createBottom() {
        connectionBottom?.id = View.generateViewId()
        // LEFT
        pathSpinner = SreSpinner(context,bottomWrapper, emptyArray()).addRule(RelativeLayout.LEFT_OF, connectionBottom!!.id).addRule(CENTER_VERTICAL, TRUE)
        pathSpinner?.visibility = if (element is IfElseTrigger) View.VISIBLE else View.INVISIBLE
        bottomWrapper?.addView(pathSpinner)
        // CENTER
        if (bottom) {
            connectionBottom?.setBackgroundColor(Color.BLACK)
        }
        connectionBottom?.layoutParams = connectionTop?.layoutParams
        bottomWrapper?.addView(connectionBottom)
        // RIGHT
        addButton = IconButton(context, bottomWrapper, R.string.icon_plus,dpiConnectorHeight,dpiConnectorHeight).addRule(RelativeLayout.RIGHT_OF, connectionBottom!!.id).addRule(CENTER_VERTICAL, TRUE)
        addButton?.id = View.generateViewId()
        removeButton = IconButton(context, bottomWrapper, R.string.icon_minus,dpiConnectorHeight,dpiConnectorHeight).addRule(RelativeLayout.RIGHT_OF, addButton!!.id).addRule(CENTER_VERTICAL, TRUE)
        addButton?.visibility = if (element is IfElseTrigger) View.VISIBLE else View.INVISIBLE
        removeButton?.visibility = if (element is IfElseTrigger) View.VISIBLE else View.INVISIBLE
        bottomWrapper?.addView(addButton)
        bottomWrapper?.addView(removeButton)
    }

    fun connectToNext(){
        connectionBottom?.setBackgroundColor(Color.BLACK)
        deleteButton?.visibility = View.INVISIBLE
    }

    fun disconnectFromNext(){
        connectionBottom?.setBackgroundColor(Color.TRANSPARENT)
        deleteButton?.visibility = View.VISIBLE
    }

    fun withLabel(label: String?): Element{
        centerElement?.text = label
        return this
    }

    fun withLabel(label: Spanned?): Element{
        centerElement?.text = label
        return this
    }

    fun updateElement(element: IElement): Element{
        this.element = element
        return this
    }

    fun isStep(): Boolean {
        return element is AbstractStep
    }

    fun setEditExecutable(function: () -> Unit): Element {
        editButton?.addExecutable(function)
        return this
    }

    fun setDeleteExecutable(function: () -> Unit): Element {
        deleteButton?.addExecutable(function)
        deleteButton?.setLongClickOnly(true)
        return this
    }

    fun setAddExecutable(function: () -> Unit): Element {
        addButton?.addExecutable(function)
        return this
    }

    fun setRemoveExecutable(function: () -> Unit): Element {
        removeButton?.addExecutable(function)
        return this
    }

    fun containsElement(element: IElement): Boolean {
        return this.element.getElementId() == element.getElementId()
    }

    fun setPathData(lookupData: Array<String>): Element{
        pathSpinner?.updateLookupData(lookupData)
        return this
    }

    fun setOnPathIndexSelectedExecutable(executable: (Int, Any?) -> Unit): Element{
        pathSpinner?.setIndexExecutable(executable)
        pathSpinner?.setDataObject(element)
        return this
    }

    fun setOnPathTextSelectedExecutable(executable: (String, Any?) -> Unit): Element{
        pathSpinner?.setSelectionExecutable(executable)
        pathSpinner?.setDataObject(element)
        return this
    }

    fun setInitSelectionExecutable(executable: (text: String)-> Unit): Element{
        pathSpinner?.setInitSelectionExecutable(executable)
        return this
    }

    fun setNothingSelectedExecutable(executable: ()-> Unit): Element{
        pathSpinner?.setNothingSelectedExecutable(executable)
        return this
    }

    fun resetSelectCount(): Element{
        pathSpinner?.selectCount = 0
        return this
    }

    fun setZebraPattern(enabled: Boolean = false){
        if (enabled){
            setBackgroundColor(ContextCompat.getColor(context,R.color.srePrimaryPastel))
        }else{
            setBackgroundColor(ContextCompat.getColor(context,R.color.sreWhite))
        }
    }
}