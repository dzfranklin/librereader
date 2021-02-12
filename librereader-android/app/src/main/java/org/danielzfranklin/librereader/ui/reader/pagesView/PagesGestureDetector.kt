package org.danielzfranklin.librereader.ui.reader.pagesView

import android.content.Context
import android.util.DisplayMetrics
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.math.abs

class PagesGestureDetector(
    context: Context,
    private val listener: Listener
) : GestureDetector(context, listener) {
    constructor(
        context: Context,
        turnState: MutableStateFlow<TurnState>,
        showOverview: MutableStateFlow<Boolean>,
        pageWidth: Float
    ) : this(
        context,
        Listener(turnState, showOverview, pageWidth, context.resources.displayMetrics)
    )

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
        private val turnState: MutableStateFlow<TurnState>,
        private val showOverview: MutableStateFlow<Boolean>,
        private val pageWidth: Float,
        displayMetrics: DisplayMetrics
    ) : GestureDetector.SimpleOnGestureListener() {
        var turnBackwardsEnabled = true
        var turnForwardsEnabled = true

        /**
         * @return If an autocompletion occurred
         */
        fun autoComplete(): Boolean {
            return when (val currentState = turnState.value) {
                is TurnState.TurningBackwards -> {
                    val percent = currentState.percent
                    if (percent > .5f) {
                        turnState.value = TurnState.CompletingTurnBack(percent)
                    } else {
                        turnState.value = TurnState.CancellingTurnBack(percent)
                    }
                    true
                }

                is TurnState.TurningForwards -> {
                    val percent = currentState.percent
                    if (percent > 0.5f) {
                        turnState.value = TurnState.CompletingTurnForward(percent)
                    } else {
                        turnState.value = TurnState.CancellingTurnForward(percent)
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
            } else if (turnForwardsEnabled && event.x > tapForwardCutoff) {
                turnForward()
            } else {
                showOverview.value = true
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

            when (val prevState = turnState.value) {
                is TurnState.TurningForwards -> {
                    if (!turnForwardsEnabled) {
                        return false
                    }
                    turnState.value = TurnState.CompletingTurnForward(prevState.percent)
                }

                is TurnState.TurningBackwards -> {
                    if (!turnBackwardsEnabled) {
                        return false
                    }
                    turnState.value = TurnState.CompletingTurnBack(prevState.percent)
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
            turnState.value = TurnState.BeganTurnForward
            turnState.value = TurnState.CompletingTurnForward(0f)
        }

        private fun turnBackward() {
            turnState.value = TurnState.BeganTurnBack
            turnState.value = TurnState.CompletingTurnBack(0f)
        }

        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent?,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            if (distanceX == 0f) {
                // Cancel vertical scrolls
                return true
            }

            val deltaPercent = distanceX / pageWidth

            when (val prevState = turnState.value) {
                is TurnState.TurningForwards -> {
                    if (!turnForwardsEnabled) {
                        return false
                    }
                    turnState.value = TurnState.TurningForwards(prevState.percent + deltaPercent)
                }

                is TurnState.TurningBackwards -> {
                    if (!turnBackwardsEnabled) {
                        return false
                    }
                    turnState.value = TurnState.TurningBackwards(prevState.percent - deltaPercent)
                }

                else -> {
                    if (distanceX > 0) {
                        if (!turnForwardsEnabled) {
                            return false
                        }
                        turnState.value = TurnState.BeganTurnForward
                        turnState.value = TurnState.TurningForwards(deltaPercent)
                    } else {
                        if (!turnBackwardsEnabled) {
                            return false
                        }
                        turnState.value = TurnState.BeganTurnBack
                        turnState.value = TurnState.TurningBackwards(-deltaPercent)
                    }
                }
            }

            return true
        }
    }
}
