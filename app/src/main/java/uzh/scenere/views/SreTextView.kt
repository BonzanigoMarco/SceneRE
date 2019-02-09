package uzh.scenere.views

import android.annotation.SuppressLint
import android.content.Context
import android.support.v4.content.ContextCompat
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import uzh.scenere.R
import uzh.scenere.views.SreTextView.TextStyle.*

@SuppressLint("ViewConstructor")
open class SreTextView(context: Context, parent: ViewGroup?, label: String? = null, val style: TextStyle = LIGHT): TextView(context) {

    constructor(context: Context, parent: ViewGroup?, stringId: Int, TextStyle: TextStyle = LIGHT): this(context,parent,context.getString(stringId),TextStyle)

    init {
        text = label
        create(context,parent)
    }

    enum class TextStyle{
        DARK,LIGHT,MEDIUM,BORDERLESS_DARK, BORDERLESS_LIGHT
    }
    enum class ParentLayout{
        RELATIVE,LINEAR,FRAME,UNKNOWN
    }

    private var parentLayout: ParentLayout = if (parent is LinearLayout) ParentLayout.LINEAR else if (parent is RelativeLayout) ParentLayout.RELATIVE else if (parent is FrameLayout) ParentLayout.FRAME else ParentLayout.UNKNOWN

    private fun create(context: Context, parent: ViewGroup?) {
        id = View.generateViewId()
        gravity = Gravity.CENTER
        when (style){
            LIGHT, BORDERLESS_LIGHT -> {
                background = context.getDrawable(if (style== LIGHT) R.drawable.sre_text_view_light else R.drawable.sre_text_view_light_borderless)
                setTextColor(ContextCompat.getColor(context,R.color.srePrimaryDark))
            }
            DARK, BORDERLESS_DARK -> {
                background = context.getDrawable(if (style== DARK) R.drawable.sre_text_view_dark else R.drawable.sre_text_view_dark_borderless)
                setTextColor(ContextCompat.getColor(context,R.color.srePrimaryPastel))
            }
            MEDIUM -> {
                background = context.getDrawable(R.drawable.sre_text_view_medium)
                setTextColor(ContextCompat.getColor(context,R.color.srePrimaryPastel))
            }
        }
        val padding = context.resources.getDimension(R.dimen.dimPaddingTextView).toInt()
        val margin = context.resources.getDimension(R.dimen.dimMarginTextView).toInt()
        setPadding(padding,padding,padding,padding)
        when (parent) {
            is LinearLayout -> {
                val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,  LinearLayout.LayoutParams.WRAP_CONTENT)
                params.setMargins(margin,margin,margin,margin)
                layoutParams = params
            }
            is RelativeLayout -> {
                val params = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,RelativeLayout.LayoutParams.WRAP_CONTENT)
                params.setMargins(margin,margin,margin,margin)
                layoutParams = params
            }
            is FrameLayout -> {
                val params = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,FrameLayout.LayoutParams.MATCH_PARENT)
                params.setMargins(margin,margin,margin,margin)
                layoutParams = params
            }
        }
    }

    open fun addRule(verb: Int, subject: Int? = null): SreTextView {
        when (parentLayout){
            ParentLayout.RELATIVE -> {
                if (subject == null){
                    (layoutParams as RelativeLayout.LayoutParams).addRule(verb)
                }else{
                    (layoutParams as RelativeLayout.LayoutParams).addRule(verb,subject)
                }
            }
            else -> {}
        }
        return this
    }

    fun setWeight(weight: Float){
        when (parentLayout){
            ParentLayout.LINEAR -> {
                (layoutParams as LinearLayout.LayoutParams).weight = weight
            }
            else -> {}
        }
    }

    fun setSize(height: Int,width: Int){
        when (parentLayout){
            ParentLayout.LINEAR -> {
                (layoutParams as LinearLayout.LayoutParams).height = height
                (layoutParams as LinearLayout.LayoutParams).width = width
            }
            ParentLayout.RELATIVE -> {
                (layoutParams as RelativeLayout.LayoutParams).height = height
                (layoutParams as RelativeLayout.LayoutParams).width = width
            }
            else -> {}
        }
    }

    fun getMargin(): Int{
        when (parentLayout){
            ParentLayout.RELATIVE -> {
                return (layoutParams as RelativeLayout.LayoutParams).leftMargin
            }
            ParentLayout.LINEAR -> {
                return (layoutParams as LinearLayout.LayoutParams).leftMargin
            }
            else -> {}
        }
        return 0
    }
}