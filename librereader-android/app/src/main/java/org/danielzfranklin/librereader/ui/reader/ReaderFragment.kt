package org.danielzfranklin.librereader.ui.reader

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nl.siegmann.epublib.domain.Book
import org.danielzfranklin.librereader.model.BookID
import org.danielzfranklin.librereader.model.BookStyle
import org.danielzfranklin.librereader.ui.reader.displayModel.BookDisplay
import org.danielzfranklin.librereader.ui.reader.displayModel.BookPageDisplay
import kotlin.coroutines.coroutineContext

abstract class ReaderFragment(@LayoutRes contentLayoutId: Int) :
    Fragment(contentLayoutId) {

    abstract fun onViewCreatedAndDataReceived(
        view: View,
        savedInstanceState: Bundle?,
        data: Data
    )

    data class Data(
        private val underlying: DisplayIndependentData,
        val display: StateFlow<BookDisplay>
    ) {
        val id = underlying.id
        val position = underlying.position
        val inOverview = underlying.inOverview

        fun toggleOverview() = underlying.toggleOverview()

        companion object {
            suspend fun from(data: DisplayIndependentData, context: Context, parent: ViewGroup): Data {
                val display = data.style
                    .map { style ->
                        withContext(Dispatchers.Default) {
                            val pageDisplay = BookPageDisplay.fitParent(parent, style)
                            val display = BookDisplay(context, data.id, data.epub, pageDisplay)
                            display
                        }   
                    }
                    .stateIn(CoroutineScope(coroutineContext))
                return Data(data, display)
            }
        }
    }

    private var data: Data? = null
    private var platform: Platform? = null

    private data class Platform(
        val view: View,
        val savedInstanceState: Bundle?
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        synchronized(this) {
            val cachedData = data
            if (cachedData != null) {
                onViewCreatedAndDataReceived(view, savedInstanceState, cachedData)
            } else {
                platform = Platform(view, savedInstanceState)
            }
        }
    }

    fun onData(newData: Data) {
        synchronized(this) {
            val cachedPlatform = platform
            if (cachedPlatform != null) {
                onViewCreatedAndDataReceived(
                    cachedPlatform.view,
                    cachedPlatform.savedInstanceState,
                    newData
                )
            } else {
                data = newData
            }
        }
    }

    data class DisplayIndependentData(
        val id: BookID,
        val style: StateFlow<BookStyle>,
        val position: PositionProcessor,
        val epub: Book,
        private val _inOverview: MutableStateFlow<Boolean>
    ) {
        val inOverview = _inOverview.asStateFlow()

        fun toggleOverview() {
            var prev = _inOverview.value
            while (!_inOverview.compareAndSet(prev, !prev)) {
                prev = _inOverview.value
            }
        }
    }
}