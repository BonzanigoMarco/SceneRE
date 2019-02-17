package uzh.scenere.views

import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import android.widget.RelativeLayout
import uzh.scenere.R
import uzh.scenere.const.Constants.Companion.NOTHING
import uzh.scenere.const.Constants.Companion.TUTORIAL_UID_IDENTIFIER
import uzh.scenere.helpers.DatabaseHelper

class SreTutorialLayoutDialog(context: Context, private val screenWidth: Int, vararg drawableResources: Int): RelativeLayout(context){

    private val idList = ArrayList<Int>()
    private var drawablePointer = -1
    private val closeButton = SreButton(context,this,NOTHING,null,null,SreButton.ButtonStyle.TUTORIAL)
    private val imageView = ImageView(context)
    private val dialog = Dialog(context)
    private lateinit  var endExecutable: () -> Unit

    init {
        if (!drawableResources.isEmpty()){
            for (id in drawableResources){
                val alreadySeen = DatabaseHelper.getInstance(context).read(TUTORIAL_UID_IDENTIFIER.plus(id), Boolean::class, false, DatabaseHelper.DataMode.PREFERENCES)
                if (!alreadySeen){
                    idList.add(id)
                }
            }
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            val params = LayoutParams(screenWidth, ViewGroup.LayoutParams.MATCH_PARENT)
            layoutParams = params
            imageView.layoutParams = params
            closeButton.addRule(RelativeLayout.ALIGN_PARENT_END, TRUE)
            closeButton.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, TRUE)
            closeButton.translationY = -closeButton.getMargin().toFloat()
            addView(imageView)
            addView(closeButton)
            closeButton.setOnClickListener { execTutorialButtonClicked() }
            dialog.setCancelable(false)
            dialog.addContentView(this, this.layoutParams)
            execTutorialButtonClicked()
        }
    }

    private fun execTutorialButtonClicked() {
        if (drawablePointer >= 0){
            DatabaseHelper.getInstance(context).write(TUTORIAL_UID_IDENTIFIER.plus(idList[drawablePointer]),true,DatabaseHelper.DataMode.PREFERENCES)
        }
        drawablePointer ++
        if (drawablePointer >= idList.size){
            if (::endExecutable.isInitialized){
                endExecutable()
            }
            dialog.dismiss()
        }else{
            closeButton.text = if (drawablePointer == idList.size-1) context.getString(R.string.tutorial_close) else context.getString(R.string.tutorial_next)
            imageView.setImageBitmap(BitmapFactory.decodeResource(resources, idList[drawablePointer]))
        }
    }

    fun show(delayMilliseconds: Long = 0): Boolean{
        if (!idList.isEmpty()) {
            Handler().postDelayed({ dialog.show() }, delayMilliseconds)
            return true
        }
        return false
    }

    fun addEndExecutable(function: () -> Unit): SreTutorialLayoutDialog {
        endExecutable = function
        return this
    }
}