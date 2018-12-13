package uzh.scenere.views

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.widget.ScrollView
import android.view.MotionEvent
import android.widget.LinearLayout


class SwipeButtonScrollView(context : Context, attributeSet: AttributeSet) : ScrollView(context, attributeSet) {

    private var mLocked = false
    private var mLockedInternal = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(ev: MotionEvent): Boolean {
        return !mLockedInternal && !mLocked && super.onTouchEvent(ev);
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        mLockedInternal = false
        if (childCount == 1 && getChildAt(0) is LinearLayout) {
            val innerLinearLayout = getChildAt(0) as LinearLayout
            for (i in 0..innerLinearLayout.childCount) {
                if (innerLinearLayout.getChildAt(i) is SwipeButton && (innerLinearLayout.getChildAt(i) as SwipeButton).interacted) {
                    mLockedInternal = true
                    return false;
                }
            }
        }
        return !mLocked && super.onInterceptTouchEvent(ev)
    }

}