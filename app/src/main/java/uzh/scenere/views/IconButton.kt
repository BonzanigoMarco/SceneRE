package uzh.scenere.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.view.MotionEvent.*
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RelativeLayout
import uzh.scenere.R
import uzh.scenere.views.IconButton.ParentLayout.*

@SuppressLint("ViewConstructor")
class IconButton(context : Context, iconId: Int, parent: ViewGroup?, height: Int? = null, width: Int? = null) : Button(context){

    init {
        text = context.getString(iconId)
        create(context,parent, height, width)
    }

    enum class ParentLayout{
        RELATIVE,LINEAR,UNKNOWN
    }

    private var parentLayout: ParentLayout = if (parent is LinearLayout) LINEAR else if (parent is RelativeLayout) RELATIVE else UNKNOWN
    private lateinit var function: () -> Unit

    private fun create(context : Context, parent: ViewGroup?, height: Int?, width: Int?) {
        id = View.generateViewId()
        gravity = Gravity.CENTER
        typeface = Typeface.createFromAsset(context.assets,"FontAwesome900.otf")
        background = context.getDrawable(R.drawable.sre_button)
        val padding = context.resources.getDimension(R.dimen.dimPaddingButton).toInt()
        val margin = context.resources.getDimension(R.dimen.dimMarginButton).toInt()
        setPadding(padding,padding,padding,padding)
        if (parent is LinearLayout){
            val params = LinearLayout.LayoutParams(width ?: LinearLayout.LayoutParams.WRAP_CONTENT,  height ?: LinearLayout.LayoutParams.WRAP_CONTENT)
            params.setMargins(margin,margin,margin,margin)
            layoutParams = params
        }else if (parent is RelativeLayout){
            val params = RelativeLayout.LayoutParams(width ?: RelativeLayout.LayoutParams.WRAP_CONTENT, height ?: RelativeLayout.LayoutParams.WRAP_CONTENT)
            params.setMargins(margin,margin,margin,margin)
            layoutParams = params
        }
        setOnTouchListener { _, event ->
            when (event?.action){
                ACTION_DOWN -> alpha = 0.5f
                ACTION_UP, ACTION_CANCEL, ACTION_OUTSIDE -> alpha = 1.0f
            }
            false
        }
        setOnClickListener { _ ->
            execAction()
        }
    }

    fun addRule(verb: Int, subject: Int?): IconButton {
        when (parentLayout){
            RELATIVE -> {
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

    fun addExecutable(function: () -> Unit){
        this.function = function
    }

    private fun execAction() {
        try{
            function()

        }catch (e: UninitializedPropertyAccessException){
            //NOP
        }
    }

    fun getMargin(): Int{
        when (parentLayout){
            RELATIVE -> {
                return (layoutParams as RelativeLayout.LayoutParams).leftMargin
            }
            LINEAR -> {
                return (layoutParams as LinearLayout.LayoutParams).leftMargin
            }
            else -> {}
        }
        return 0
    }
}