package uzh.scenere.views

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.Gravity
import android.widget.TextView


class IconTextView(context: Context) : TextView(context) {

    init {
        create(context)
    }

    private fun create(context: Context) {
        gravity = Gravity.CENTER
        typeface = Typeface.createFromAsset(context.assets, "FontAwesome900.otf")
    }
}