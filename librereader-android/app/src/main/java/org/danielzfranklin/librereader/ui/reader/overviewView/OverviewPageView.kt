package org.danielzfranklin.librereader.ui.reader.overviewView

import android.content.Context
import org.danielzfranklin.librereader.ui.reader.PageView

class OverviewPageView(context: Context) : PageView(context) {
    var onClickListener: () -> Unit = { }

    init {
        setOnClickListener { _ ->
            onClickListener()
        }
    }
//    override fun onTouchEvent(event: MotionEvent?): Boolean {
//        if (event?.action == MotionEvent.ACTION_UP) {
//            performClick()
//            return true
//        }
//
//        return super.onTouchEvent(event)
//    }
//
//    override fun performClick(): Boolean {
//        onClickListener()
//        super.performClick()
//        return true
//    }
}