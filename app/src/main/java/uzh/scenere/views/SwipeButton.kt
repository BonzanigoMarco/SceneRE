package uzh.scenere.views

import android.animation.*
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
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
import android.opengl.ETC1.getHeight
import android.opengl.ETC1.getWidth




class SwipeButton(context: Context?, attributeSet: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : LinearLayout(context, attributeSet, defStyleAttr, defStyleRes) {
    enum class SwipeButtonState {
        LEFT, UP, DOWN, RIGHT, MIDDLE
    }

    enum class SwipeButtonMode {
        DOUBLE, QUADRUPLE
    }

    enum class Direction {
        X, Y
    }
    //Data
    public var dataObject: Any? = null
    public var outputObject: TextView? = null
    //Function
    private var exec: SwipeButtonExecution = object : SwipeButtonExecution {}
    private var state: SwipeButtonState = SwipeButtonState.MIDDLE
    private var mode: SwipeButtonMode = SwipeButtonMode.QUADRUPLE
    //Grid
    private var topLayout: LinearLayout? = null
    private var bottomLayout: RelativeLayout? = null
    private var bottomBackgroundLayout: RelativeLayout? = null
    //Button State
    private var initialX = 0f
    private var initialY = 0f
    private var initialEventY = -1f
    private var initialEventX = -1f
    private var initialButtonWidth = 0
    private var sliderButton: IconTextView? = null
    private var sliderLane: RelativeLayout? = null
    //Layout
    private var labelText: TextView? = null
    private var topText: TextView? = null
    private var topBg: TextView? = null
    private var bottomText: TextView? = null
    private var bottomBg: TextView? = null
    private var leftText: TextView? = null
    private var rightText: TextView? = null
    //Configuration
    private var lIdx: Int = 0
    private var rIdx: Int = 1
    private var tIdx: Int = 2
    private var bIdx: Int = 3
    private var icons: IntArray = intArrayOf(R.string.icon_delete, R.string.icon_edit, R.string.icon_email, R.string.icon_lock)
    private var react: BooleanArray = booleanArrayOf(true, true, true, true)
    private var colors: IntArray = intArrayOf(Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE)
    //State
    private var initialized: Boolean = false
    private var active: Boolean = false
    private var activeColor: Int = Color.WHITE
    private var inactiveColor: Int = Color.GRAY
    private var individualColor: Boolean = false
    public var interacted: Boolean = false
    //Measurement
    private var dpiText = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5f, resources.displayMetrics);
    private val dpiPadding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8f, resources.displayMetrics).toInt();
    private val dpiSliderMargin = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10f, resources.displayMetrics).toInt();
    private val dpiHeight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 150f, resources.displayMetrics).toInt();
    private val dpiSideMargin = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20f, resources.displayMetrics).toInt();

    init {
        init(context)
    }

    //XML Constructors
    constructor(context: Context?) : this(context, null, -1, -1) {
        init(context)
    }

    constructor(context: Context?, attributeSet: AttributeSet?) : this(context, attributeSet, -1, -1) {
        init(context)
    }

    constructor(context: Context?, attributeSet: AttributeSet?, defStyleAttr: Int) : this(context, attributeSet, defStyleAttr, -1) {
        init(context)
    }

    //Inline Constructors
    constructor(context: Context?, label: String) : this(context, null, -1, -1) {
        init(context)
        labelText?.text = label
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun init(context: Context?) {
        if (initialized) {
            return
        }

        //Master Layout Params
        this.orientation = LinearLayout.VERTICAL

        val masterLayoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpiHeight)
        masterLayoutParams.marginStart = dpiSideMargin
        masterLayoutParams.marginEnd = dpiSideMargin
        masterLayoutParams.topMargin = dpiSideMargin / 5
        masterLayoutParams.weight = 10f
        layoutParams = masterLayoutParams

        //Grid, Top Box (30%)
        val topLayout = LinearLayout(context)
        this.topLayout = topLayout
        topLayout.background = ContextCompat.getDrawable(context, R.drawable.blue_swipe_button_bg_top)
        val topLayoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0)
        topLayoutParams.weight = 3f
        topLayout.layoutParams = topLayoutParams
        addView(topLayout)

        //Grid, Middle Box (60%)
        val middleLinearLayout = LinearLayout(context)
        val middleLayoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0)
        middleLayoutParams.weight = 6f
        middleLinearLayout.layoutParams = middleLayoutParams
        addView(middleLinearLayout)

        //Grid, Bottom Box (10%)
        val bottomLinearLayout = LinearLayout(context)
        val bottomLayoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0)
        bottomLinearLayout.background = ContextCompat.getDrawable(context, R.drawable.blue_swipe_button_bg_bottom)
        bottomLayoutParams.weight = 1f
        bottomLinearLayout.layoutParams = bottomLayoutParams
        addView(bottomLinearLayout)

        //Content Wrapper for Bottom Box
        val bottomLayout = RelativeLayout(context)
        this.bottomLayout = bottomLayout
        middleLinearLayout.addView(bottomLayout)

        //Background
        val bottomBackgroundLayout = RelativeLayout(context)
        this.bottomBackgroundLayout = bottomBackgroundLayout
        val backgroundLayoutParams = createParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, RelativeLayout.CENTER_HORIZONTAL)
        bottomBackgroundLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.sreAnthrazit))
        bottomBackgroundLayout.background = ContextCompat.getDrawable(context, R.drawable.blue_swipe_button_bg_mid)
        bottomLayout.addView(bottomBackgroundLayout, backgroundLayoutParams)

        //Label
        val labelText = TextView(context)
        this.labelText = labelText
        val labelTextParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        labelText.layoutParams = labelTextParams
        labelText.textAlignment = View.TEXT_ALIGNMENT_CENTER
        labelText.gravity = Gravity.CENTER
        topLayout.addView(labelText, labelTextParams)

        //Slider-Lane
        val sliderLane = RelativeLayout(context)
        this.sliderLane = sliderLane
        val sliderLaneParams = createParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, RelativeLayout.CENTER_IN_PARENT)
        sliderLaneParams.marginStart = dpiSliderMargin
        sliderLaneParams.marginEnd = dpiSliderMargin
        sliderLane.background = ContextCompat.getDrawable(context, R.drawable.blue_swipe_button_slider_mid)
        bottomLayout.addView(sliderLane, sliderLaneParams)

        //Slider-Button
        val sliderButton = IconTextView(context)
        this.sliderButton = sliderButton
        val sliderButtonParams = createParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.CENTER_VERTICAL)
        sliderButton.text = null
        sliderButton.setPadding(dpiPadding * 8, dpiPadding, dpiPadding * 8, dpiPadding)
        sliderButton.background = ContextCompat.getDrawable(context, R.drawable.blue_swipe_button_slider_button)
        bottomLayout.addView(sliderButton, sliderButtonParams)

        //Top Icon
        val topText = IconTextView(context)
        this.topText = topText
        val topTextParams = createParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.CENTER_HORIZONTAL)
        topText.setPadding(dpiPadding, dpiPadding, dpiPadding, dpiPadding)
        topText.background = ContextCompat.getDrawable(context, R.drawable.blue_swipe_button_slider_top)

        //Top Background
        val topBg = IconTextView(context)
        this.topBg = topBg
        val topBgParams = createParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.CENTER_HORIZONTAL)
        topBg.setPadding(dpiPadding * 4, dpiPadding, dpiPadding * 4, dpiPadding)
        topBg.background = ContextCompat.getDrawable(context, R.drawable.blue_swipe_button_slider_top)
        bottomBackgroundLayout.addView(topBg, topBgParams)
        bottomBackgroundLayout.addView(topText, topTextParams)

        //Bottom Icon
        val bottomText = IconTextView(context)
        this.bottomText = bottomText
        val bottomTextParams = createParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.CENTER_HORIZONTAL)
        bottomText.setPadding(dpiPadding, dpiPadding, dpiPadding, dpiPadding)
        bottomText.background = ContextCompat.getDrawable(context, R.drawable.blue_swipe_button_slider_bottom)

        //Bottom Background
        val bottomBg = IconTextView(context)
        this.bottomBg = bottomBg
        val bottomBgParams = createParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.CENTER_HORIZONTAL)
        bottomBg.setPadding(dpiPadding * 4, dpiPadding, dpiPadding * 4, dpiPadding)
        bottomBg.background = ContextCompat.getDrawable(context, R.drawable.blue_swipe_button_slider_bottom)
        bottomBackgroundLayout.addView(bottomBg, bottomBgParams)
        bottomBackgroundLayout.addView(bottomText, bottomTextParams)

        //Left Icon
        val leftText = IconTextView(context)
        this.leftText = leftText
        val leftTextParams = createParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, RelativeLayout.ALIGN_PARENT_LEFT)
        leftText.setPadding(dpiPadding, dpiPadding, dpiPadding, dpiPadding)
        this.leftText = leftText
        sliderLane.addView(leftText, leftTextParams)

        //Right Icon
        val rightText = IconTextView(context)
        this.rightText = rightText
        val rightTextParams = createParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, RelativeLayout.ALIGN_PARENT_RIGHT)
        rightText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, dpiText)
        rightText.setPadding(dpiPadding, dpiPadding, dpiPadding, dpiPadding)
        sliderLane.addView(rightText, rightTextParams)

        //Touch Listener
        bottomLayout.setOnTouchListener(getButtonTouchListener())

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
        //Update Buttons
        updateViews(false)
        //Finish Initialization
        initialized = true
    }

    private fun getButtonTouchListener(): View.OnTouchListener? {
        return OnTouchListener { _, event ->
            val hitBox = Rect()
            sliderButton?.getHitRect(hitBox)
            val x = NumberHelper.nvl(event.x.toInt(), 0f).toInt()
            val y = NumberHelper.nvl(event.y.toInt(), 0f).toInt()
            val margin = 0.25f //Easier clickable with bigger Hitbox
            if (!(x * (1 + margin) >= hitBox.left && x * (1 - margin) < hitBox.right
                            && y * (1 + margin) >= hitBox.top && y * (1 - margin) < hitBox.bottom)
                    && event.action != MotionEvent.ACTION_UP) {
                return@OnTouchListener true
            }
            //Calculation Values
            val sliderWidth = NumberHelper.nvl(sliderButton?.width, 0f).toFloat()
            val sliderHeight = NumberHelper.nvl(sliderButton?.height, 0f).toFloat()
            val sliderX = NumberHelper.nvl(sliderButton?.x, 0f).toFloat()
            val sliderY = NumberHelper.nvl(sliderButton?.y, 0f).toFloat()
            val h = NumberHelper.nvl(bottomLayout?.height, 0f).toFloat()
            val w = NumberHelper.nvl(bottomLayout?.width, 0f).toFloat() - dpiSliderMargin
            val sliderSpanX = (width - sliderWidth) / 2 - (dpiSliderMargin * 2)
            val sliderSpanY = (height - sliderHeight) / 2
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    val rect = Rect()
                    sliderButton?.getHitRect(rect)
                    if (rect.contains(event.x.toInt(), event.y.toInt())) {
                        interacted = true;
                        initialEventY = event.y
                        initialEventX = event.x
                        return@OnTouchListener true
                    }
                    return@OnTouchListener false
                }
                MotionEvent.ACTION_MOVE -> {
                    if (Math.abs(initialEventX - event.x) > Math.abs(initialEventY - event.y) && sliderButton?.y == initialY) {
                        /*HORIZONTAL*/
                        //Handle text transparency
                        val alpha = 1 - 1.3f * Math.abs(sliderSpanX - sliderX) / sliderSpanX
                        topText?.alpha = alpha
                        bottomText?.alpha = alpha
                        leftText?.alpha = alpha
                        rightText?.alpha = alpha
                        //Handle general slide to the left || right
                        if (event.x > sliderWidth / 2 + dpiSliderMargin && event.x + sliderWidth / 2 < w) {
                            sliderButton?.x = (event.x - sliderWidth / 2)
                        }
                        //Handle corner conditions on the left
                        if (event.x < sliderWidth / 2 + dpiSliderMargin && sliderX + sliderWidth / 2 + dpiSliderMargin > 0) {
                            sliderButton?.x = dpiSliderMargin.toFloat()
                        }
                        //Handle corner conditions on the right
                        if (event.x + sliderWidth / 2 > w && sliderX + sliderWidth / 2 < w) {
                            sliderButton?.x = (w - sliderWidth)
                        }
                    } else if (Math.abs(initialEventX - event.x) < Math.abs(initialEventY - event.y) && sliderButton?.x == initialX && mode == SwipeButtonMode.QUADRUPLE) {
                        /*VERTICAL*/
                        //Handle text transparency
                        val alpha = 1 - 1.3f * Math.abs((sliderSpanY / 2) - sliderY) / (sliderSpanY / 2)
                        topText?.alpha = alpha
                        bottomText?.alpha = alpha
                        leftText?.alpha = alpha
                        rightText?.alpha = alpha
                        //Handle general slide to up || down
                        if (event.y > sliderHeight / 2 && event.y + sliderHeight / 2 < h) {
                            sliderButton?.y = (event.y - sliderHeight / 2)
                        }
                        //Handle corner conditions on up
                        if (event.y < sliderHeight / 2 && sliderY + sliderHeight / 2 > 0) {
                            sliderButton?.y = 0f
                        }
                        //Handle corner conditions on down
                        if (event.y > (h - sliderHeight / 2) && sliderY + sliderHeight / 2 < h) {
                            sliderButton?.y = h - sliderHeight
                        }
                    }
                    return@OnTouchListener true
                }
                MotionEvent.ACTION_UP -> {
                    interacted = false;
                    if (active) {
                        collapse()
                    } else {
                        initialButtonWidth = sliderWidth.toInt()
                        when {
                            sliderX + sliderWidth > w * 0.85 && react[rIdx] -> expand(icons[rIdx], SwipeButtonState.RIGHT)
                            sliderX < w * 0.15 && react[lIdx] -> expand(icons[lIdx], SwipeButtonState.LEFT)
                            sliderY + sliderHeight > h * 0.975 && react[bIdx] -> expand(icons[bIdx], SwipeButtonState.DOWN)
                            sliderY < h * 0.025 && react[tIdx] -> expand(icons[tIdx], SwipeButtonState.UP)
                            else -> reset()
                        }
                    }
                    return@OnTouchListener true
                }
            }
            false
        }
    }

    //Configuration
    fun setButtonIcons(lIcon: Int?, rIcon: Int?, tIcon: Int?, bIcon: Int?): SwipeButton {
        icons[lIdx] = lIcon ?: icons[lIdx]
        icons[rIdx] = rIcon ?: icons[rIdx]
        icons[tIdx] = tIcon ?: icons[tIdx]
        icons[bIdx] = bIcon ?: icons[bIdx]
        return this
    }

    fun setColors(active: Int, inactive: Int): SwipeButton {
        individualColor = false
        activeColor = active
        inactiveColor = inactive
        return this
    }

    fun setButtonStates(lActive: Boolean, rActive: Boolean, tActive: Boolean, bActive: Boolean): SwipeButton {
        react[lIdx] = lActive
        react[rIdx] = rActive
        react[tIdx] = tActive
        react[bIdx] = bActive
        return this
    }

    fun setIndividualButtonColors(lColor: Int, rColor: Int, tColor: Int, bColor: Int): SwipeButton {
        individualColor = true
        colors[lIdx] = lColor
        colors[rIdx] = rColor
        colors[tIdx] = tColor
        colors[bIdx] = bColor
        return this
    }

    fun setButtonMode(mode: SwipeButtonMode): SwipeButton {
        this.mode = mode
        return this
    }

    fun setExecutable(executable: SwipeButtonExecution): SwipeButton {
        exec = executable
        return this
    }

    fun updateViews(minimal: Boolean): SwipeButton {
        if (!minimal) {
            if (mode == SwipeButtonMode.DOUBLE) {
                dpiText *= 2
                topText?.visibility = View.GONE
                topBg?.visibility = View.GONE
                bottomText?.visibility = View.GONE
                bottomBg?.visibility = View.GONE
            }
            leftText?.setTextSize(TypedValue.COMPLEX_UNIT_DIP, dpiText)
            rightText?.setTextSize(TypedValue.COMPLEX_UNIT_DIP, dpiText)
            topText?.setTextSize(TypedValue.COMPLEX_UNIT_DIP, dpiText)
            bottomText?.setTextSize(TypedValue.COMPLEX_UNIT_DIP, dpiText)
            sliderButton?.setTextSize(TypedValue.COMPLEX_UNIT_DIP, dpiText * 1.5f)
        }
        //Color & States
        sliderButton?.setTextColor(activeColor)
        labelText?.setTextColor(activeColor)
        leftText?.setTextColor(if (individualColor) colors[lIdx] else (if (react[lIdx]) activeColor else inactiveColor))
        rightText?.setTextColor(if (individualColor) colors[rIdx] else (if (react[rIdx]) activeColor else inactiveColor))
        topText?.setTextColor(if (individualColor) colors[tIdx] else (if (react[tIdx]) activeColor else inactiveColor))
        bottomText?.setTextColor(if (individualColor) colors[bIdx] else (if (react[bIdx]) activeColor else inactiveColor))
        //Text
        leftText?.text = resources.getText(icons[lIdx])
        rightText?.text = resources.getText(icons[rIdx])
        topText?.text = resources.getText(icons[tIdx])
        bottomText?.text = resources.getText(icons[bIdx])
        return this
    }

    //Animations
    private fun expand(icon: Int, state: SwipeButtonState) {
        this.state = state
        val sliderX = NumberHelper.nvl(sliderButton?.x, 0f).toFloat()
        val sliderWidth = NumberHelper.nvl(sliderButton?.width, 0).toInt()
        val positionAnimator = ValueAnimator.ofFloat(sliderX, dpiSliderMargin.toFloat())
        positionAnimator.addUpdateListener {
            val x = positionAnimator.animatedValue as Float
            sliderButton?.x = x
        }
        val widthAnimator = ValueAnimator.ofInt(sliderWidth, width - (dpiSliderMargin * 2))
        widthAnimator.addUpdateListener {
            val params = sliderButton?.layoutParams
            params?.width = widthAnimator.animatedValue as Int
            sliderButton?.layoutParams = params
        }
        val positionYAnimator = createAnimator(Direction.Y, 200)
        val listener = object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                active = true
                sliderButton?.text = resources.getText(icon)
            }
        }

        playAnimations(listener, positionAnimator, widthAnimator, positionYAnimator)
        execute()
    }

    public fun collapse() {
        this.state = SwipeButtonState.MIDDLE
        sliderButton?.text = null
        initialEventX = -1f
        initialEventY = -1f
        val sliderWidth = NumberHelper.nvl(sliderButton?.width, 0).toInt()
        val widthAnimator = ValueAnimator.ofInt(sliderWidth, initialButtonWidth)
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

        playAnimations(null, positionAnimator, widthAnimator, leftTextAnimator, rightTextAnimator, topTextAnimator, bottomTextAnimator)
        execute()
    }

    private fun reset() {
        val positionXAnimator = createAnimator(Direction.X, 200L)
        val positionYAnimator = createAnimator(Direction.Y, 200L)

        val rightTextAnimator = ObjectAnimator.ofFloat(rightText as TextView, "alpha", 1f)
        val leftTextAnimator = ObjectAnimator.ofFloat(leftText as TextView, "alpha", 1f)
        val topTextAnimator = ObjectAnimator.ofFloat(topText as TextView, "alpha", 1f)
        val bottomTextAnimator = ObjectAnimator.ofFloat(bottomText as TextView, "alpha", 1f)

        playAnimations(null, positionXAnimator, positionYAnimator, leftTextAnimator, rightTextAnimator, topTextAnimator, bottomTextAnimator)
    }

    private fun createAnimator(dir: Direction, duration: Long): ValueAnimator? {
        val sliderPos = NumberHelper.nvl(if (dir == Direction.X) sliderButton?.x else sliderButton?.y, 0f).toFloat()
        val positionAnimator = ValueAnimator.ofFloat(sliderPos, if (dir == Direction.X) initialX else initialY)
        positionAnimator.interpolator = AccelerateDecelerateInterpolator()
        positionAnimator.addUpdateListener {
            val value = positionAnimator.animatedValue as Float
            if (dir == Direction.X) {
                sliderButton?.x = value
            } else {
                sliderButton?.y = value
            }
        }
        positionAnimator.duration = duration
        return positionAnimator
    }

    private fun <T : Animator?> playAnimations(listener: AnimatorListenerAdapter?, vararg animators: T) {
        val animatorSet = AnimatorSet()
        if (listener != null) {
            animatorSet.addListener(listener)
        }
        animatorSet.playTogether(animators.asList())
        animatorSet.start()
    }

    private fun createParams(width: Int, height: Int, vararg rules: Int): RelativeLayout.LayoutParams {
        val params = RelativeLayout.LayoutParams(width, height)
        for (rule in rules.asList()) {
            params.addRule(rule)
        }
        return params
    }

    private fun execute() {
        when (state) {
            SwipeButtonState.LEFT -> exec.execLeft()
            SwipeButtonState.RIGHT -> exec.execRight()
            SwipeButtonState.UP -> exec.execUp()
            SwipeButtonState.DOWN -> exec.execDown()
            SwipeButtonState.MIDDLE -> exec.execReset()
        }
    }

    interface SwipeButtonExecution {
        fun execLeft() {/*NOP*/
        }

        fun execRight() {/*NOP*/
        }

        fun execUp() {/*NOP*/
        }

        fun execDown() {/*NOP*/
        }

        fun execReset() {/*NOP*/
        }
    }
}