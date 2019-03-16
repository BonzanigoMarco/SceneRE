package uzh.scenere.helpers

import android.content.Context
import android.graphics.Typeface
import android.support.v4.content.ContextCompat
import android.widget.TextView
import uzh.scenere.R

class StyleHelper private constructor(context: Context) {

    val switchToNormalMode = HashMap<Int,Int>()
    val switchToContrastMode = HashMap<Int,Int>()

    init {
        addEntry(context,R.color.sreWhite,R.color.sreGray)
        addEntry(context,R.color.srePrimaryPastel,R.color.sreBlack_II)
        addEntry(context,R.color.srePrimaryLight,R.color.sreBlack_III)
        addEntry(context,R.color.srePrimary,R.color.sreBlack_IV)
        addEntry(context,R.color.srePrimaryDark,R.color.sreBlack_V)
        addEntry(context,R.color.srePrimaryDisabledText,R.color.sreWhite_I)
        addEntry(context,R.color.srePrimaryDisabled,R.color.sreWhite_II)
        addEntry(context,R.color.sreBlack,R.color.sreWhite_III)
    }

    private fun addEntry(context: Context, normalId: Int, contrastId: Int){
        val normal = ContextCompat.getColor(context, normalId)
        val contrast = ContextCompat.getColor(context, contrastId)
        switchToNormalMode[contrast] = normal
        switchToNormalMode[contrastId] = normal
        switchToContrastMode[normal] = contrast
        switchToContrastMode[normalId] = contrast
    }


    fun switch(view: TextView, toStyle: SreStyle){
        var textColor: Int? = null
        var hintColor: Int? = null
        when(toStyle){
            SreStyle.NORMAL -> {
                textColor = switchToNormalMode[view.currentTextColor]
                hintColor = switchToNormalMode[view.currentHintTextColor]
                if (StringHelper.hasText(view.text) && !view.text.matches(".*[^\\x20-\\x7E].*".toRegex())){
                    //no icons contained
                    view.typeface = Typeface.DEFAULT
                }
            }
            SreStyle.CONTRAST -> {
                textColor = switchToContrastMode[view.currentTextColor]
                hintColor = switchToContrastMode[view.currentHintTextColor]
                if (StringHelper.hasText(view.text) && !view.text.matches(".*[^\\x20-\\x7E].*".toRegex())){
                    //no icons contained
                    view.typeface = Typeface.DEFAULT_BOLD
                }
            }
        }
        if (textColor != null){
            view.setTextColor(textColor)
        }
        if (hintColor != null){
            view.setHintTextColor(hintColor)
        }
    }

    companion object {
        // Volatile: writes to this field are immediately made visible to other threads.
        @Volatile
        private var instance: StyleHelper? = null

        fun get(context: Context): StyleHelper {
            return when {
                instance != null -> instance!!
                else -> synchronized(this) {
                    if (instance == null) {
                        instance = StyleHelper(context)
                    }
                    instance!!
                }
            }
        }
    }
}