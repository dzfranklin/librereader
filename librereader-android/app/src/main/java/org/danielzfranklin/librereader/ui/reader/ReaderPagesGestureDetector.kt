package org.danielzfranklin.librereader.ui.reader

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber

class ReaderPagesGestureDetector(
    context: Context,
    private val listener: Listener
) : GestureDetector(context, listener) {
    constructor(
        context: Context,
        state: MutableStateFlow<TurnState>,
        pageWidth: StateFlow<Float>
    ) : this(context, Listener(state, pageWidth))

    fun onWindowVisibilityChanged(visibility: Int) {
        if (visibility == View.GONE || visibility == View.INVISIBLE) {
            listener.autoComplete()
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event?.action == MotionEvent.ACTION_UP) {
            listener.autoComplete()
        }

        return super.onTouchEvent(event)
    }

    class Listener(
        private val state: MutableStateFlow<TurnState>,
        private val pageWidth: StateFlow<Float>
    ) : GestureDetector.SimpleOnGestureListener() {
        fun autoComplete() {
            // TODO: Animate
            when (val currentState = state.value) {
                is TurnState.TurningBackwards -> {
                    if (currentState.percent > .5) {
                        state.value = TurnState.CompletingTurnBack
                    } else {
                        state.value = TurnState.CancellingTurnBack
                    }
                }

                is TurnState.TurningForwards -> {
                    if (currentState.percent > 0.5) {
                        state.value = TurnState.CompletingTurnForward
                    } else {
                        state.value = TurnState.CancellingTurnForward
                    }
                }
            }
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

            val deltaPercent = distanceX / pageWidth.value

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