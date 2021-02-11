package org.danielzfranklin.librereader.ui.reader

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.danielzfranklin.librereader.repo.Repo
import org.danielzfranklin.librereader.model.BookID
import org.danielzfranklin.librereader.model.BookPosition
import kotlin.coroutines.CoroutineContext

class PositionProcessor(
    override val coroutineContext: CoroutineContext,
    private val id: BookID,
    position: StateFlow<BookPosition>
) : CoroutineScope {
    private val repo = Repo.get()

    val position
        get() = _events.value.position

    fun set(changer: Any, position: BookPosition) {
        if (_events.value.position == position) {
            return
        }

        if (_events.tryEmit(Change(changer.hashCode(), position))) {
            repo.updatePosition(id, position)
        } else {
            throw IllegalStateException("State flow should never refuse")
        }
    }

    data class Change(val changer: Int, val position: BookPosition)

    private val _events = MutableStateFlow(Change(this.hashCode(), position.value))
    val events = _events.asStateFlow()

    init {
        launch {
            position.collect {
                set(this@PositionProcessor, it)
            }
        }
    }
}