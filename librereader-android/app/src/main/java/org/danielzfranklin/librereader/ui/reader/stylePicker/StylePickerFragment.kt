package org.danielzfranklin.librereader.ui.reader.stylePicker

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import org.danielzfranklin.librereader.R
import org.danielzfranklin.librereader.ui.reader.ReaderFragment

class StylePickerFragment : ReaderFragment(R.layout.style_picker_fragment), CoroutineScope {
    override val coroutineContext = lifecycleScope.coroutineContext
//
//    private lateinit var binding: StylePickerFragmentBinding
//    private lateinit var data: Data
//    private val book: StateFlow<BookDisplay> by lazy { data.display }
//    private lateinit var epub: Book
//    private lateinit var position: BookPosition
//    private lateinit var pendingStyle: MutableStateFlow<BookStyle>
//
    override fun onViewCreated(view: View, savedInstanceState: Bundle?, data: Data) {
//        binding = StylePickerFragmentBinding.bind(view)
//        pendingStyle = MutableStateFlow(data.display.value.pageDisplay.style)
//        binding.style = pendingStyle
    }
//
//    private var visibleJob: Job? = null
//
//    override fun onResume(data: Data) {
//        this.data = data
//
//        // NOTE: We don't support updating the page displayed while selecting a style
//        position = data.position.value
//
//        epub = book.value.epub
//
//        visibleJob = launch {
//            book.collect {
//                binding.page.displaySpan(position.page(it))
//                pendingStyle.value = it.pageDisplay.style
//            }
//        }
//    }
//
//    override fun onPause() {
//        super.onPause()
//        visibleJob?.cancel(CancellationException("onPause"))
//    }
//
//    private fun previewStyle(newStyle: BookStyle) {
//        val pageDisplay = book.value.pageDisplay.copy(style = newStyle)
//        val section = BookSectionDisplay(requireContext(), position.sectionIndex, epub, pageDisplay)
//        // TODO: Only paginate until the page we need
//        val page = position.page(section)
//        binding.page.displaySpan(page)
//    }
}
