package org.danielzfranklin.librereader.ui.reader.pagesView

import android.content.Context
import android.util.DisplayMetrics
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
    ) : this(context, Listener(state, pageWidth, context.resources.displayMetrics))

    fun enableTurnBackwards() {
        listener.turnBackwardsEnabled = true
    }

    fun disableTurnBackwards() {
        listener.turnBackwardsEnabled = false
    }

    fun enableTurnForwards() {
        listener.turnForwardsEnabled = true
    }

    fun disableTurnForwards() {
        listener.turnForwardsEnabled = false
    }

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
        private val pageWidth: Float,
        private val displayMetrics: DisplayMetrics
    ) : GestureDetector.SimpleOnGestureListener() {
        var turnBackwardsEnabled = true
        var turnForwardsEnabled = true

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

        private val tapBackCutoff = 110 * displayMetrics.density
        private val tapForwardCutoff = pageWidth - (110 * displayMetrics.density)

        override fun onSingleTapUp(event: MotionEvent?): Boolean {
            if (event == null) {
                return false
            }

            if (turnBackwardsEnabled && event.x < tapBackCutoff) {
                turnBackward()
                return true
            } else if (turnForwardsEnabled && event.x > tapForwardCutoff) {
                turnForward()
                return true
            }

            return false
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
                    if (!turnForwardsEnabled) {
                        return false
                    }
                    state.value = TurnState.CompletingTurnForward(prevState.percent)
                }

                is TurnState.TurningBackwards -> {
                    if (!turnBackwardsEnabled) {
                        return false
                    }
                    state.value = TurnState.CompletingTurnBack(prevState.percent)
                }

                else -> {
                    if (velocityX < 0) {
                        if (!turnForwardsEnabled) {
                            return false
                        } else {
                            turnForward()
                        }
                    } else {
                        if (!turnBackwardsEnabled) {
                            return false
                        } else {
                            turnBackward()
                        }
                    }
                }
            }

            return true
        }

        private fun turnForward() {
            state.value = TurnState.BeganTurnForward
            state.value = TurnState.CompletingTurnForward(0f)
        }

        private fun turnBackward() {
            state.value = TurnState.BeganTurnBack
            state.value = TurnState.CompletingTurnBack(0f)
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
                    if (!turnForwardsEnabled) {
                        return false
                    }
                    state.value = TurnState.TurningForwards(prevState.percent + deltaPercent)
                }

                is TurnState.TurningBackwards -> {
                    if (!turnBackwardsEnabled) {
                        return false
                    }
                    state.value = TurnState.TurningBackwards(prevState.percent - deltaPercent)
                }

                else -> {
                    if (distanceX > 0) {
                        if (!turnForwardsEnabled) {
                            return false
                        }
                        state.value = TurnState.BeganTurnForward
                        state.value = TurnState.TurningForwards(deltaPercent)
                    } else {
                        if (!turnBackwardsEnabled) {
                            return false
                        }
                        state.value = TurnState.BeganTurnBack
                        state.value = TurnState.TurningBackwards(-deltaPercent)
                    }
                }
            }

            return true
        }
    }
}
