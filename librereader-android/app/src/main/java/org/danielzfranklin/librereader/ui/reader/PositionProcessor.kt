package org.danielzfranklin.librereader.ui.reader

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import org.danielzfranklin.librereader.repo.model.BookPosition
import org.danielzfranklin.librereader.ui.reader.displayModel.BookDisplay
import timber.log.Timber

class PositionProcessor(initialValue: BookPosition) {
    val position
        get() = _events.value.position

    fun set(changer: Any, position: BookPosition) {
        if (_events.value.position == position) {
            return
        }

        if (!_events.tryEmit(Change(changer, position))) {
            throw IllegalStateException("State flow should never refuse")
        }
    }

    data class Change(val changer: Any, val position: BookPosition)

    private val _events = MutableStateFlow(Change(this, initialValue))
    val events = _events.asStateFlow()
}