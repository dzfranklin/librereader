package org.danielzfranklin.librereader.ui.reader

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import nl.siegmann.epublib.domain.Book
import org.danielzfranklin.librereader.model.BookID
import org.danielzfranklin.librereader.model.BookStyle
import org.danielzfranklin.librereader.ui.reader.displayModel.BookDisplay
import org.danielzfranklin.librereader.ui.reader.displayModel.BookPageDisplay
import timber.log.Timber
import kotlin.coroutines.coroutineContext

abstract class ReaderFragment(@LayoutRes contentLayoutId: Int) :
    Fragment(contentLayoutId) {

    fun switchToPages() {
        (requireActivity() as? ReaderActivity)?.switchToPages()
            ?: Timber.w("Not switching to pages as not attached to ReaderActivity")
    }

    fun switchToOverview() {
        (requireActivity() as? ReaderActivity)?.switchToOverview()
            ?: Timber.w("Not switching to overview as not attached to ReaderActivity")
    }

    abstract fun onViewCreated(
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

        companion object {
            suspend fun from(
                data: DisplayIndependentData,
                context: Context,
                parent: ViewGroup
            ): Data {
                val display = data.style
                    .map { style ->
                        withContext(Dispatchers.Default) {
                            val pageDisplay = BookPageDisplay.fitParent(parent, style)
                            val display = BookDisplay(context, data.id, data.epub, pageDisplay)
                            display.sections[data.position.value.sectionIndex].preload()
                            display
                        }
                    }
                    .stateIn(CoroutineScope(coroutineContext))
                return Data(data, display)
            }
        }
    }

    private var data: Data? = null
    private var viewCreatedParams: ViewCreatedParams? = null

    private data class ViewCreatedParams(
        val view: View,
        val savedInstanceState: Bundle?
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        synchronized(this) {
            val cachedData = data
            if (cachedData != null) {
                onViewCreated(view, savedInstanceState, cachedData)
            } else {
                viewCreatedParams = ViewCreatedParams(view, savedInstanceState)
            }
        }
    }

    fun onData(newData: Data) {
        synchronized(this) {
            val cachedPlatform = viewCreatedParams
            if (cachedPlatform != null) {
                onViewCreated(
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
        val style: Flow<BookStyle>,
        val position: PositionProcessor,
        val epub: Book,
    )
}
