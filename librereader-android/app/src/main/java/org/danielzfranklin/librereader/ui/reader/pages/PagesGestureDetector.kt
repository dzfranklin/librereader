package org.danielzfranklin.librereader.ui.reader.pages

import android.content.Context
import android.util.DisplayMetrics
import android.view.GestureDetector
import android.view.MotionEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.coroutines.CoroutineContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class PagesGestureDetector(
    override val coroutineContext: CoroutineContext,
    context: Context,
    pageWidth: Float,
    val listener: Listener
) : CoroutineScope {
    /**
     * Guarantee: No callback will ever be executed while another callback is executing
     */
    interface Listener {
        fun onShowOverview()
        fun onBeginTurnBack()
        fun onBeginTurnForward()
        fun onCancelTurnBack(fromPercent: Float)
        fun onCancelTurnForward(fromPercent: Float)
        fun onCompleteTurnBack(fromPercent: Float)
        fun onCompleteTurnForward(fromPercent: Float)
        fun onTurnBackwards(percent: Float)
        fun onTurnForwards(percent: Float)
    }

    private val turnState = MutableStateFlow<TurnState>(TurnState.Initial)
    private val internalListener =
        InternalListener(this, turnState, pageWidth, context.resources.displayMetrics)
    private val detector = GestureDetector(context, internalListener)

    init {
        launch {
            turnState.collect {
                Timber.d("Detector found %s", it)
                when (it) {
                    TurnState.Initial -> Unit
                    TurnState.BeganTurnBack -> listener.onBeginTurnBack()
                    TurnState.BeganTurnForward -> listener.onBeginTurnForward()
                    is TurnState.CancellingTurnBack -> listener.onCancelTurnBack(it.fromPercent)
                    is TurnState.CancellingTurnForward -> listener.onCancelTurnForward(it.fromPercent)
                    is TurnState.CompletingTurnBack -> listener.onCompleteTurnBack(it.fromPercent)
                    is TurnState.CompletingTurnForward -> listener.onCompleteTurnForward(it.fromPercent)
                    is TurnState.TurningBackwards -> listener.onTurnBackwards(it.percent)
                    is TurnState.TurningForwards -> listener.onTurnForwards(it.percent)
                }
            }
        }
    }

    fun enableTurnBackwards() {
        internalListener.turnBackwardsEnabled = true
    }

    fun disableTurnBackwards() {
        internalListener.turnBackwardsEnabled = false
    }

    fun enableTurnForwards() {
        internalListener.turnForwardsEnabled = true
    }

    fun disableTurnForwards() {
        internalListener.turnForwardsEnabled = false
    }

    fun pause() {
        internalListener.autoComplete()
    }

    fun resume() {}

    fun onTouchEvent(event: MotionEvent?): Boolean {
        if (detector.onTouchEvent(event)) {
            return true
        }

        if (event?.action == MotionEvent.ACTION_UP) {
            return internalListener.autoComplete()
        }

        return false
    }

    private class InternalListener(
        private val manager: PagesGestureDetector,
        private val turnState: MutableStateFlow<TurnState>,
        private val pageWidth: Float,
        displayMetrics: DisplayMetrics
    ) : GestureDetector.SimpleOnGestureListener() {
        var turnBackwardsEnabled = true
        var turnForwardsEnabled = true

        /** @return If an autocompletion occurred */
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
                manager.listener.onShowOverview()
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

    private sealed class TurnState {
        object Initial : TurnState()

        object BeganTurnBack : TurnState()
        object BeganTurnForward : TurnState()

        class TurningBackwards(percent: Float) : TurnState() {
            val percent = clampPercent(percent)
        }

        class TurningForwards(percent: Float) : TurnState() {
            val percent = clampPercent(percent)
        }

        data class CompletingTurnBack(val fromPercent: Float) : TurnState()
        data class CompletingTurnForward(val fromPercent: Float) : TurnState()

        data class CancellingTurnBack(val fromPercent: Float) : TurnState()
        data class CancellingTurnForward(val fromPercent: Float) : TurnState()

        companion object {
            private fun clampPercent(percent: Float): Float {
                return max(0f, min(percent, 1f))
            }
        }
    }
}
