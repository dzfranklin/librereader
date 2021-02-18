package org.danielzfranklin.librereader.ui.reader

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.danielzfranklin.librereader.model.BookPosition
import org.danielzfranklin.librereader.repo.Repo
import kotlin.coroutines.CoroutineContext

class PositionProcessor(
    override val coroutineContext: CoroutineContext,
    initialPosition: BookPosition
) : CoroutineScope {
    private val repo = Repo.get()

    val value
        get() = _events.value.position

    fun set(changer: Any, position: BookPosition) {
        if (_events.value.position == position) {
            return
        }

        if (!_events.tryEmit(Change(changer.hashCode(), position))) {
            throw IllegalStateException("State flow should never refuse")
        }
    }

    data class Change(val changer: Int, val position: BookPosition)

    private val _events = MutableStateFlow(Change(this.hashCode(), initialPosition))
    val events = _events.asStateFlow()

    init {
        launch {
            events.collectLatest {
                repo.updatePosition(it.position)
            }
        }
    }
}
