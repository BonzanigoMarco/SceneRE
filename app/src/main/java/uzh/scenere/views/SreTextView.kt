package uzh.scenere.views

import android.annotation.SuppressLint
import android.content.Context
import android.support.v4.content.ContextCompat
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import uzh.scenere.R
import uzh.scenere.views.SreTextView.STYLE.LIGHT

@SuppressLint("ViewConstructor")
open class SreTextView(context: Context, parent: ViewGroup?, label: String? = null, val style: STYLE = LIGHT): TextView(context) {

    constructor(context: Context, parent: ViewGroup?, stringId: Int, style: STYLE = LIGHT): this(context,parent,context.getString(stringId),style)

    init {
        text = label
        create(context,parent,style)
    }

    enum class STYLE{
        DARK,LIGHT
    }
    enum class ParentLayout{
        RELATIVE,LINEAR,UNKNOWN
    }

    private var parentLayout: ParentLayout = if (parent is LinearLayout) ParentLayout.LINEAR else if (parent is RelativeLayout) ParentLayout.RELATIVE else ParentLayout.UNKNOWN

    private fun create(context: Context, parent: ViewGroup?, style: STYLE) {
        id = View.generateViewId()
        gravity = Gravity.CENTER
        background = context.getDrawable(if (style== STYLE.DARK) R.drawable.sre_text_view_dark else R.drawable.sre_text_view_light)
        setTextColor(if (style== STYLE.DARK) ContextCompat.getColor(context,R.color.srePrimaryPastel) else ContextCompat.getColor(context,R.color.srePrimaryDark))
        val padding = context.resources.getDimension(R.dimen.dimPaddingTextView).toInt()
        val margin = context.resources.getDimension(R.dimen.dimMarginTextView).toInt()
        setPadding(padding,padding,padding,padding)
        if (parent is LinearLayout){
            val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,  LinearLayout.LayoutParams.WRAP_CONTENT)
            params.setMargins(margin,margin,margin,margin)
            layoutParams = params
        }else if (parent is RelativeLayout){
            val params = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,RelativeLayout.LayoutParams.WRAP_CONTENT)
            params.setMargins(margin,margin,margin,margin)
            layoutParams = params
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