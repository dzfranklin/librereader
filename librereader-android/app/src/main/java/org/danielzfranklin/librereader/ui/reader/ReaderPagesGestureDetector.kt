package org.danielzfranklin.librereader.ui.reader

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import timber.log.Timber
import kotlin.math.abs

class ReaderPagesGestureDetector(
    context: Context,
    private val listener: Listener
) : GestureDetector(context, listener) {
    constructor(context: Context, view: ReaderPagesView) : this(context, Listener(view))

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event?.action == MotionEvent.ACTION_UP) {
            listener.complete()
        }

        return super.onTouchEvent(event)
    }

    class Listener(private val view: ReaderPagesView) : GestureDetector.SimpleOnGestureListener() {
        var turnedDistance = 0f
        var inTurnBack = false

        fun complete() {
            val cutoff = view.currentPage.width / 2
            val shouldComplete = abs(turnedDistance) > cutoff

            if (inTurnBack) {
                if (shouldComplete) {
                    animateToTurn(1f)
                } else {
                    animateToTurn(0f)
                }
            } else {
                if (shouldComplete) {
                    animateToTurn(0f)
                } else {
                    animateToTurn(1f)
                }
            }

            inTurnBack = false
            turnedDistance = 0f
        }

        private fun animateToTurn(percent: Float) {
            turnedDistance = 0f
            view.turnTo(percent) // TODO: make this animation
        }

        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent?,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            turnedDistance += distanceX

            if (turnedDistance == 0f) {
                return false
            }

            if (turnedDistance > 0) {
                view.turnTo(1 - turnedDistance / view.currentPage.width)
            } else if (turnedDistance < 0) {
                if (!inTurnBack) {
                    view.beginTurnBack()
                    inTurnBack = true
                }
                view.turnTo(abs(turnedDistance) / view.currentPage.width)
            }

            return true
        }
    }
}
