package uzh.scenere.views

import android.content.Context
import android.graphics.Typeface
import android.support.v4.content.res.ResourcesCompat
import android.util.AttributeSet
import android.widget.TextView
import android.view.Gravity



class IconTextView(context : Context?, attributeSet: AttributeSet?) : TextView(context, attributeSet){
    private var m_context : Context? = context

    constructor(context: Context?) : this(context,null){
        this.m_context = context
        create()
    }

    private fun create() {
        gravity = Gravity.CENTER
        typeface = Typeface.createFromAsset(m_context?.assets,"FontAwesome900.otf")
    }
}