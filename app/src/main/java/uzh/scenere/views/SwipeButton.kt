package uzh.scenere.views

import android.animation.*
import android.content.Context
import android.graphics.Color
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import uzh.scenere.R
import uzh.scenere.helpers.NumberHelper


class SwipeButton(context: Context?, attributeSet: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : LinearLayout(context,attributeSet,defStyleAttr,defStyleRes) {
    enum class SwipeButtonState{
        LEFT,UP,DOWN,RIGHT,MIDDLE
    }

    private var exec : SwipeButtonExecution = object : SwipeButtonExecution {}

    private var state : SwipeButtonState = SwipeButtonState.MIDDLE

    private var sliderButton: IconTextView? = null
    private var sliderLane: RelativeLayout? = null
    private var topLayout: LinearLayout? = null
    private var bottomLayout: RelativeLayout? = null

    private var initialX: Float = 0f
    private var initialY: Float = 0f
    private var initialEventY = -1f
    private var initialEventX = -1f
    private var initialButtonWidth: Int = 0

    private var backgroundLayout: RelativeLayout? = null
    private var topText: TextView? = null
    private var bottomText: TextView? = null
    private var leftText: TextView? = null
    private var rightText: TextView? = null

    private var topIcon: Int = R.string.icon_email
    private var bottomIcon: Int = R.string.icon_lock
    private var leftIcon: Int = R.string.icon_delete
    private var rightIcon: Int = R.string.icon_edit

    private var initialized: Boolean = false
    private var active: Boolean = false

    fun setExecution(exec: SwipeButtonExecution){
        this.exec = exec
    }

    init {
        init(context)
    }
    constructor(context: Context?) : this(context,null,-1,-1){
        init(context)
    }
    constructor(context: Context?, attributeSet: AttributeSet?) : this(context, attributeSet,-1,-1){
        init(context)
    }
    constructor(context: Context?, attributeSet: AttributeSet?, defStyleAttr: Int) : this(context, attributeSet, defStyleAttr,-1){
        init(context)
    }
    private fun init(context: Context?){
        if (initialized){
            return
        }
        val padding = 35

        //Master Layout Params
        this.orientation = LinearLayout.VERTICAL
        val masterLayoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0)
        masterLayoutParams.weight = 3f
        layoutParams = masterLayoutParams

        //Grid, Top Box (33%)
        val topLayout = LinearLayout(context)
        this.topLayout = topLayout
        topLayout.setBackgroundColor(Color.RED)
        val topLayoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0)
        topLayoutParams.weight = 1f
        topLayout.layoutParams = topLayoutParams
        addView(topLayout)

        //Grid, Bottom Box (66%)
        val bottomLinearLayout = LinearLayout(context)
        val bottomLayoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0)
        bottomLayoutParams.weight = 2f
        bottomLinearLayout.layoutParams = bottomLayoutParams
        addView(bottomLinearLayout)

        //Content Wrapper for Bottom Box
        val bottomLayout = RelativeLayout(context)
        this.bottomLayout = bottomLayout
        bottomLinearLayout.addView(bottomLayout)

        //Background
        val backgroundLayout = RelativeLayout(context)
        this.backgroundLayout = backgroundLayout
        val backgroundLayoutParams = createParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT,RelativeLayout.CENTER_HORIZONTAL)
        backgroundLayout.setBackgroundColor(Color.BLACK)
        bottomLayout.addView(backgroundLayout,backgroundLayoutParams)

        //Slider-Lane
        val sliderLane = RelativeLayout(context)
        this.sliderLane = sliderLane
        val layoutParamsView = createParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT,RelativeLayout.CENTER_IN_PARENT)
        sliderLane.background = ContextCompat.getDrawable(context, R.drawable.shape_slider_background)
        bottomLayout.addView(sliderLane, layoutParamsView)

        //Slider-Button
        val sliderButton = IconTextView(context)
        this.sliderButton = sliderButton
        val layoutParamsButton = createParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT,RelativeLayout.ALIGN_PARENT_LEFT,RelativeLayout.CENTER_VERTICAL)
        sliderButton.text = null
        sliderButton.setTextColor(Color.WHITE)
        sliderButton.textSize = 20f //TODO Scaling issue
        sliderButton.setPadding(padding*4,padding,padding*4,padding)
        sliderButton.background = ContextCompat.getDrawable(context, R.drawable.shape_slider_foreground)
        bottomLayout.addView(sliderButton, layoutParamsButton)

        //Top Icon
        val topText = IconTextView(context)
        this.topText = topText
        topText.text = resources.getText(topIcon)
        topText.setTextColor(Color.WHITE)
        val topTextParams = createParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,RelativeLayout.ALIGN_PARENT_TOP,RelativeLayout.CENTER_HORIZONTAL)
        topText.setPadding(padding,padding,padding,padding)
        topText.background = ContextCompat.getDrawable(context, R.drawable.shape_slider_top)
        //Top Background
        val topBg = IconTextView(context)
        val topBgParams = createParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,RelativeLayout.ALIGN_PARENT_TOP,RelativeLayout.CENTER_HORIZONTAL)
        topBg.setPadding(padding*4,2*padding,padding*4,padding)
        topBg.background = ContextCompat.getDrawable(context, R.drawable.shape_slider_top)

        backgroundLayout.addView(topBg,topBgParams)
        backgroundLayout.addView(topText,topTextParams)

        //Bottom Icon
        val bottomText = IconTextView(context)
        this.bottomText = bottomText
        bottomText.text = resources.getText(bottomIcon)
        bottomText.setTextColor(Color.WHITE)
        val bottomTextParams = createParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,RelativeLayout.ALIGN_PARENT_BOTTOM,RelativeLayout.CENTER_HORIZONTAL)
        bottomText.setPadding(padding,padding,padding,padding)
        bottomText.background = ContextCompat.getDrawable(context, R.drawable.shape_slider_bottom)
        //Bottom Background
        val bottomBg = IconTextView(context)
        val bottomBgParams = createParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,RelativeLayout.ALIGN_PARENT_BOTTOM,RelativeLayout.CENTER_HORIZONTAL)
        bottomBg.setPadding(padding*4,padding,padding*4,padding)
        bottomBg.background = ContextCompat.getDrawable(context, R.drawable.shape_slider_bottom)

        backgroundLayout.addView(bottomBg,bottomBgParams)
        backgroundLayout.addView(bottomText,bottomTextParams)

        //Left Icon
        val leftText = IconTextView(context)
        this.leftText = leftText
        val leftTextParams = createParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,RelativeLayout.ALIGN_PARENT_LEFT)
        leftText.text = resources.getText(leftIcon)
        leftText.setTextColor(Color.WHITE)
        leftText.setSingleLine()
        leftText.setPadding(padding,padding,padding,padding)
        this.leftText = leftText
        sliderLane.addView(leftText, leftTextParams)

        //Right Icon
        val rightText = IconTextView(context)
        this.rightText = rightText
        val rightTextParams = createParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,RelativeLayout.ALIGN_PARENT_RIGHT)
        rightText.text = resources.getText(rightIcon)
        rightText.setTextColor(Color.WHITE)
        rightText.setSingleLine()
        rightText.setPadding(padding,padding,padding,padding)
        sliderLane.addView(rightText, rightTextParams)

        //Touch Listener
        setOnTouchListener(getButtonTouchListener())

        //Initializer
        val layout = this
        val viewTreeObserver = layout.viewTreeObserver
        viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                layout.viewTreeObserver.removeOnGlobalLayoutListener(this)
                val width = layout.measuredWidth
                val initialX = width / 2f - sliderButton.width / 2f
                layout.initialX = initialX
                sliderButton.x = initialX
                layout.initialY = sliderButton.y
            }
        })

        //Finish Initialization
        initialized = true
    }

    private fun getButtonTouchListener(): View.OnTouchListener? {
        return OnTouchListener { _, event ->
            val sliderWidth = NumberHelper.nvl(sliderButton?.width, 0f).toFloat()
            val sliderHeight = NumberHelper.nvl(sliderButton?.height, 0f).toFloat()
            val topSpan = NumberHelper.nvl(topLayout?.height, 0f).toFloat()
            val sliderX = NumberHelper.nvl(sliderButton?.x, 0f).toFloat()
            val sliderY = NumberHelper.nvl(sliderButton?.y, 0f).toFloat()
            val sliderSpanX = (width-sliderWidth)/2
            val sliderSpanY = (height-sliderHeight)/2
            when (event.action) {
                MotionEvent.ACTION_DOWN ->{
                    initialEventY = event.y
                    initialEventX = event.x
                    return@OnTouchListener true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (Math.abs(initialEventX-event.x)>Math.abs(initialEventY-event.y) && sliderButton?.y==initialY){
                        /*HORIZONTAL*/
                        //Handle text transparency
                        val alpha = 1 - 1.3f * Math.abs(sliderSpanX - sliderX) / sliderSpanX
                        topText?.alpha = alpha
                        bottomText?.alpha = alpha
                        leftText?.alpha = alpha
                        rightText?.alpha = alpha
                        //Handle general slide to the left || right
                        if (event.x > sliderWidth / 2 && event.x + sliderWidth / 2 < width){
                            sliderButton?.x = (event.x - sliderWidth / 2)
                        }
                        //Handle corner conditions on the left
                        if  (event.x < sliderWidth / 2 && sliderX + sliderWidth / 2 > 0) {
                            sliderButton?.x = 0f
                        }
                        //Handle corner conditions on the right
                        if  (event.x + sliderWidth / 2 > width && sliderX + sliderWidth / 2 < width) {
                            sliderButton?.x = (width - sliderWidth)
                        }
                    }else if(Math.abs(initialEventX-event.x)<Math.abs(initialEventY-event.y) && sliderButton?.x==initialX){
                        /*VERTICAL*/
                        //Handle text transparency
                        val alpha = 1 - 1.3f * Math.abs((sliderSpanY/2) - sliderY) / (sliderSpanY/2)
                        topText?.alpha = alpha
                        bottomText?.alpha = alpha
                        leftText?.alpha = alpha
                        rightText?.alpha = alpha
                        //Handle general slide to up || down
                        if (event.y > sliderHeight / 2 && event.y + sliderHeight / 2 < height){
//                        if (event.y + sliderHeight / 2 > -height && event.y + sliderHeight / 2 < height){
                            sliderButton?.y = (event.y - sliderHeight / 2)-topSpan
                        }
                        //Handle corner conditions on up
                        if  (event.y < (topSpan + sliderHeight / 2) && sliderY + sliderHeight / 2 > 0) {
                            sliderButton?.y = 0f
                        }
                        //Handle corner conditions on down
                        if  (event.y + sliderHeight / 2 > height && sliderY + sliderHeight / 2 < height) {
                            sliderButton?.y = height-sliderHeight-topSpan
                        }
                    }
                    return@OnTouchListener true
                }
                MotionEvent.ACTION_UP ->{
                    if (active) {
                        collapse()
                    } else {
                        initialButtonWidth = sliderWidth.toInt()
                        when {
                            sliderX + sliderWidth > width * 0.85 -> expand(rightIcon, SwipeButtonState.RIGHT)
                            sliderX < width * 0.15 -> expand(leftIcon, SwipeButtonState.LEFT)
                            sliderY + sliderHeight > (height-topSpan) * 0.975 -> expand(bottomIcon, SwipeButtonState.DOWN)
                            sliderY < height * 0.025 -> expand(topIcon, SwipeButtonState.UP)
                            else -> reset()
                        }
                    }
                    return@OnTouchListener true
                }
            }
            false
        }
    }

    private fun expand(icon: Int, state: SwipeButtonState) {
        this.state = state
        val sliderX = NumberHelper.nvl(sliderButton?.x, 0f).toFloat()
        val sliderWidth = NumberHelper.nvl(sliderButton?.width, 0).toInt()
        val positionAnimator = ValueAnimator.ofFloat(sliderX, 0f)
        positionAnimator.addUpdateListener {
            val x = positionAnimator.animatedValue as Float
            sliderButton?.x = x
        }
        val widthAnimator = ValueAnimator.ofInt(sliderWidth,width)
        widthAnimator.addUpdateListener {
            val params = sliderButton?.layoutParams
            params?.width = widthAnimator.animatedValue as Int
            sliderButton?.layoutParams = params
        }
        val positionYAnimator = createAnimator(Direction.Y,200)
        val listener = object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                active = true
                sliderButton?.text = resources.getText(icon)
            }}

        playAnimations(listener,positionAnimator, widthAnimator,positionYAnimator)
        execute()
    }

    private fun collapse() {
        this.state = SwipeButtonState.MIDDLE
        sliderButton?.text = null
        initialEventX = -1f
        initialEventY = -1f
        val sliderWidth = NumberHelper.nvl(sliderButton?.width, 0).toInt()
        val widthAnimator = ValueAnimator.ofInt(sliderWidth,initialButtonWidth)
        val positionAnimator = ValueAnimator.ofFloat(0f, initialX)
        positionAnimator.addUpdateListener {
            val x = positionAnimator.animatedValue as Float
            sliderButton?.x = x
        }
        widthAnimator.addUpdateListener {
            val params = sliderButton?.layoutParams
            params?.width = widthAnimator.animatedValue as Int
            sliderButton?.layoutParams = params
        }
        widthAnimator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                active = false
                reset()
            }
        })

        val leftTextAnimator = ObjectAnimator.ofFloat(leftText as TextView, "alpha", 1f)
        val rightTextAnimator = ObjectAnimator.ofFloat(rightText as TextView, "alpha", 1f)
        val topTextAnimator = ObjectAnimator.ofFloat(topText as TextView, "alpha", 1f)
        val bottomTextAnimator = ObjectAnimator.ofFloat(bottomText as TextView, "alpha", 1f)

        playAnimations(null,positionAnimator, widthAnimator,leftTextAnimator,rightTextAnimator,topTextAnimator,bottomTextAnimator)
    }

    private fun reset() {
        val positionXAnimator = createAnimator(Direction.X,200L)
        val positionYAnimator = createAnimator(Direction.Y,200L)

        val rightTextAnimator = ObjectAnimator.ofFloat(rightText as TextView, "alpha", 1f)
        val leftTextAnimator = ObjectAnimator.ofFloat(leftText as TextView, "alpha", 1f)
        val topTextAnimator = ObjectAnimator.ofFloat(topText as TextView, "alpha", 1f)
        val bottomTextAnimator = ObjectAnimator.ofFloat(bottomText as TextView, "alpha", 1f)

        playAnimations(null,positionXAnimator,positionYAnimator,leftTextAnimator,rightTextAnimator,topTextAnimator,bottomTextAnimator)
    }

    enum class Direction{
        X,Y
    }
    private fun createAnimator(dir : Direction, duration: Long): ValueAnimator? {
        val sliderPos = NumberHelper.nvl(if (dir==Direction.X) sliderButton?.x else sliderButton?.y, 0f).toFloat()
        val positionAnimator = ValueAnimator.ofFloat(sliderPos, if (dir==Direction.X) initialX else initialY)
        positionAnimator.interpolator = AccelerateDecelerateInterpolator()
        positionAnimator.addUpdateListener {
            val value = positionAnimator.animatedValue as Float
            if (dir==Direction.X){
                sliderButton?.x = value
            }else{
                sliderButton?.y = value
            }
        }
        positionAnimator.duration = duration
        return positionAnimator
    }

    private fun <T : Animator?> playAnimations(listener: AnimatorListenerAdapter?, vararg animators: T) {
        val animatorSet = AnimatorSet()
        if (listener != null){
            animatorSet.addListener(listener)
        }
        animatorSet.playTogether(animators.asList())
        animatorSet.start()
    }
    private fun createParams(width : Int, height : Int, vararg rules : Int) : RelativeLayout.LayoutParams {
        val params = RelativeLayout.LayoutParams(width,height)
        for (rule in rules.asList()) {
            params.addRule(rule)
        }
        return params
    }
    private fun execute(){
        when (state) {
            SwipeButtonState.LEFT -> exec.execLeft()
            SwipeButtonState.RIGHT -> exec.execRight()
            SwipeButtonState.UP -> exec.execUp()
            SwipeButtonState.DOWN -> exec.execDown()
            SwipeButtonState.MIDDLE -> exec.execReset()
        }
    }
    interface SwipeButtonExecution{
        fun execLeft(){/*NOP*/}
        fun execRight(){/*NOP*/}
        fun execUp(){/*NOP*/}
        fun execDown(){/*NOP*/}
        fun execReset(){/*NOP*/}
    }
}