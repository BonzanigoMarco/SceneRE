package uzh.scenere.views

import android.content.Context
import android.support.v4.content.ContextCompat
import android.view.Gravity
import android.view.MotionEvent.*
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RelativeLayout
import uzh.scenere.R
import uzh.scenere.views.SreButton.ButtonParentLayout.*

open class SreButton(context: Context, parent: ViewGroup?, label: String?, height: Int? = null, width: Int? = null): Button(context) {

    constructor(context: Context, parent: ViewGroup?, stringId: Int, height: Int? = null, width: Int? = null): this(context,parent,context.getString(stringId),height,width)

    init {
        text = label
        create(context, parent, height, width)
    }

    enum class ButtonParentLayout {
        RELATIVE, LINEAR, UNKNOWN
    }

    private var parentLayout: ButtonParentLayout = if (parent is LinearLayout) LINEAR else if (parent is RelativeLayout) RELATIVE else UNKNOWN
    private lateinit var function: () -> Unit
    private var longClickOnly: Boolean = false

    private fun create(context: Context, parent: ViewGroup?, height: Int?, width: Int?) {
        id = View.generateViewId()
        gravity = Gravity.CENTER
        background = context.getDrawable(R.drawable.sre_button)
        setTextColor(ContextCompat.getColor(context,R.color.srePrimaryPastel))
        val padding = context.resources.getDimension(R.dimen.dpi5).toInt()
        val margin = context.resources.getDimension(R.dimen.dpi5).toInt()
        setPadding(padding, padding, padding, padding)
        if (parent is LinearLayout) {
            val params = LinearLayout.LayoutParams(width
                    ?: LinearLayout.LayoutParams.WRAP_CONTENT, height
                    ?: LinearLayout.LayoutParams.WRAP_CONTENT)
            params.setMargins(margin, margin, margin, margin)
            layoutParams = params
        } else if (parent is RelativeLayout) {
            val params = RelativeLayout.LayoutParams(width
                    ?: RelativeLayout.LayoutParams.WRAP_CONTENT, height
                    ?: RelativeLayout.LayoutParams.WRAP_CONTENT)
            params.setMargins(margin, margin, margin, margin)
            layoutParams = params
        }
        setOnTouchListener { _, event ->
            when (event?.action) {
                ACTION_DOWN -> alpha = 0.5f
                ACTION_UP, ACTION_CANCEL, ACTION_OUTSIDE -> alpha = 1.0f
            }
            false
        }
        setOnClickListener { _ ->
            execAction()
        }
        setOnLongClickListener {
            execAction(true)
            false
        }
    }

    open fun addRule(verb: Int, subject: Int? = null): SreButton {
        when (parentLayout) {
            RELATIVE -> {
                if (subject == null) {
                    (layoutParams as RelativeLayout.LayoutParams).addRule(verb)
                } else {
                    (layoutParams as RelativeLayout.LayoutParams).addRule(verb, subject)
                }
            }
            else -> {
            }
        }
        return this
    }

    fun addExecutable(function: () -> Unit): SreButton {
        this.function = function
        return this
    }

    fun setLongClickOnly(longClick: Boolean) {
        this.longClickOnly = longClick
    }

    private fun execAction(longClick: Boolean = false) {
        if (longClickOnly && !longClick) {
            return
        }
        try {
            function()

        } catch (e: UninitializedPropertyAccessException) {
            //NOP
        }
    }

    fun getMargin(): Int {
        when (parentLayout) {
            RELATIVE -> {
                return (layoutParams as RelativeLayout.LayoutParams).leftMargin
            }
            LINEAR -> {
                return (layoutParams as LinearLayout.LayoutParams).leftMargin
            }
            else -> {
            }
        }
        return 0
    }
}