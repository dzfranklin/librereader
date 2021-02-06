package org.danielzfranklin.librereader.ui.reader.pagesView

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.math.abs

class ReaderPagesGestureDetector(
    context: Context,
    private val listener: Listener
) : GestureDetector(context, listener) {
    constructor(
        context: Context,
        state: MutableStateFlow<TurnState>,
        pageWidth: Float
    ) : this(context, Listener(state, pageWidth))

    fun onWindowVisibilityChanged(visibility: Int) {
        if (visibility == View.GONE || visibility == View.INVISIBLE) {
            listener.autoComplete()
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (super.onTouchEvent(event)) {
            return true
        }

        if (event?.action == MotionEvent.ACTION_UP) {
            return listener.autoComplete()
        }

        return false
    }

    class Listener(
        private val state: MutableStateFlow<TurnState>,
        private val pageWidth: Float
    ) : GestureDetector.SimpleOnGestureListener() {
        /**
         * @return If an autocompletion occurred
         */
        fun autoComplete(): Boolean {
            return when (val currentState = state.value) {
                is TurnState.TurningBackwards -> {
                    val percent = currentState.percent
                    if (percent > .5f) {
                        state.value = TurnState.CompletingTurnBack(percent)
                    } else {
                        state.value = TurnState.CancellingTurnBack(percent)
                    }
                    true
                }

                is TurnState.TurningForwards -> {
                    val percent = currentState.percent
                    if (percent > 0.5f) {
                        state.value = TurnState.CompletingTurnForward(percent)
                    } else {
                        state.value = TurnState.CancellingTurnForward(percent)
                    }
                    true
                }

                else -> false
            }
        }

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent?,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            if (abs(velocityX) < 100) {
                return false
            }

            when (val prevState = state.value) {
                is TurnState.TurningForwards -> {
                    state.value = TurnState.CompletingTurnForward(prevState.percent)
                }

                is TurnState.TurningBackwards -> {
                    state.value = TurnState.CompletingTurnBack(prevState.percent)
                }

                else -> {
                    if (velocityX < 0) {
                        state.value = TurnState.BeganTurnForward
                        state.value = TurnState.CompletingTurnForward(0f)
                    } else {
                        state.value = TurnState.BeganTurnBack
                        state.value = TurnState.CompletingTurnBack(0f)
                    }
                }
            }

            return true
        }

        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent?,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            if (distanceX == 0f) {
                return false
            }

            val deltaPercent = distanceX / pageWidth

            when (val prevState = state.value) {
                is TurnState.TurningForwards -> {
                    state.value = TurnState.TurningForwards(prevState.percent + deltaPercent)
                }

                is TurnState.TurningBackwards -> {
                    state.value = TurnState.TurningBackwards(prevState.percent - deltaPercent)
                }

                else -> {
                    if (distanceX > 0) {
                        state.value = TurnState.BeganTurnForward
                        state.value = TurnState.TurningForwards(deltaPercent)
                    } else {
                        state.value = TurnState.BeganTurnBack
                        state.value = TurnState.TurningBackwards(-deltaPercent)
                    }
                }
            }

            return true
        }
    }
}
