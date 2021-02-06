package org.danielzfranklin.librereader.ui.reader

import kotlin.math.max
import kotlin.math.min

sealed class TurnState {
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