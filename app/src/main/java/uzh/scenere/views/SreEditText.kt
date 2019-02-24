package uzh.scenere.views

import android.annotation.SuppressLint
import android.content.Context
import android.support.v4.content.ContextCompat
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RelativeLayout
import uzh.scenere.R
import uzh.scenere.helpers.DipHelper
import uzh.scenere.views.SreEditText.EditStyle.DARK
import uzh.scenere.views.SreEditText.EditStyle.LIGHT

@SuppressLint("ViewConstructor")
open class SreEditText(context: Context, parent: ViewGroup?, text: String? = null, hint: String? = null, val style: EditStyle = DARK): EditText(context) {

    constructor(context: Context, parent: ViewGroup?, textId: Int,  hintId: Int, style: EditStyle = LIGHT): this(context,parent,context.getString(textId),context.getString(hintId),style)

    init {
        setHint(hint)
        setText(text)
        create(context,parent)
    }

    enum class EditStyle{
        DARK,LIGHT
    }
    enum class ParentLayout{
        RELATIVE,LINEAR,UNKNOWN
    }

    private var parentLayout: ParentLayout = if (parent is LinearLayout) ParentLayout.LINEAR else if (parent is RelativeLayout) ParentLayout.RELATIVE else ParentLayout.UNKNOWN

    private fun create(context: Context, parent: ViewGroup?) {
        id = View.generateViewId()
        gravity = Gravity.CENTER
        background = context.getDrawable(if (style== DARK) R.drawable.sre_edit_text_dark else R.drawable.sre_edit_text_light)
        setTextColor(if (style== DARK) ContextCompat.getColor(context,R.color.srePrimaryPastel) else ContextCompat.getColor(context,R.color.srePrimaryDark))
        val padding = DipHelper.get(resources).dip15.toInt()
        val margin = DipHelper.get(resources).dip0.toInt()
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

    open fun addRule(verb: Int, subject: Int? = null): SreEditText {
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