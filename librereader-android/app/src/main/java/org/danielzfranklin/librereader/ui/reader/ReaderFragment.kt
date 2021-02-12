package org.danielzfranklin.librereader.ui.reader

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import nl.siegmann.epublib.domain.Book
import org.danielzfranklin.librereader.databinding.ReaderFragmentBinding
import org.danielzfranklin.librereader.model.BookID
import org.danielzfranklin.librereader.model.BookMeta
import org.danielzfranklin.librereader.ui.reader.displayModel.BookDisplay
import org.danielzfranklin.librereader.ui.reader.displayModel.BookPageDisplay
import org.danielzfranklin.librereader.ui.reader.overviewView.OverviewView
import org.danielzfranklin.librereader.ui.reader.pagesView.PagesView
import timber.log.Timber

class ReaderFragment : Fragment(), View.OnLayoutChangeListener, CoroutineScope {
    override val coroutineContext = lifecycleScope.coroutineContext

    private lateinit var binding: ReaderFragmentBinding
    private lateinit var bookId: BookID
    private val model: ReaderViewModel by viewModels { ReaderViewModel.Factory(bookId) }

    private var rootLaidOut = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        bookId = requireArguments().getParcelable(ARG_BOOK_ID) ?: throw IllegalArgumentException(
            "Missing argument ARG_BOOK_ID"
        )

        binding = ReaderFragmentBinding.inflate(layoutInflater, container, false)

        binding.root.addOnLayoutChangeListener(this)

        launch {
            model.state.collect {
                when (it) {
                    // NOTE: We make pagesParent invisible instead of gone so it gets laid out
                    is ReaderViewModel.LoadState.Loading -> {
                        binding.pagesParent.visibility = View.INVISIBLE
                        binding.loadingProgress.visibility = View.VISIBLE
                        binding.loadingImage.visibility = View.GONE
                    }
                    is ReaderViewModel.LoadState.LoadingHasMeta -> {
                        binding.pagesParent.visibility = View.INVISIBLE
                        binding.loadingProgress.visibility = View.VISIBLE
                        binding.loadingImage.visibility = View.VISIBLE
                        binding.loadingImage.setImageBitmap(it.meta.cover)
                    }
                    is ReaderViewModel.LoadState.Loaded -> {
                        binding.pagesParent.visibility = View.VISIBLE
                        binding.loadingProgress.visibility = View.GONE
                        binding.loadingImage.visibility = View.GONE

                        if (rootLaidOut) {
                            attach(it)
                        } else {
                            Timber.i("Skipping recreating pages view because onLayoutChange will do so later")
                        }
                    }
                }
            }
        }

        return binding.root
    }

    override fun onLayoutChange(
        v: View?,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
        oldLeft: Int,
        oldTop: Int,
        oldRight: Int,
        oldBottom: Int
    ) {
        rootLaidOut = true

        if (left == oldLeft && top == oldTop && right == oldRight && bottom == oldBottom) {
            return
        }

        val cached = model.state.value
        if (cached is ReaderViewModel.LoadState.Loaded) {
            attach(cached)
        }
    }

    private fun attach(state: ReaderViewModel.LoadState.Loaded) {
        display(state.meta, state.epub, state.positionProcessor)

        launch {
            var prevStyle = state.meta.style // Since we display immediately, then get a flow
            state.styleUpdates
                .collectLatest {
                    if (it != prevStyle) {
                        display(state.meta.copy(style = it), state.epub, state.positionProcessor)
                        prevStyle = it
                    }
                }
        }
    }

    private fun display(meta: BookMeta, epub: Book, positionProcessor: PositionProcessor) {
        binding.pagesParent.removeAllViews()

        val pageDisplay = BookPageDisplay.fitParent(binding.pagesParent, meta.style)
        val book = BookDisplay(requireContext(), model.bookId, epub, pageDisplay)

        binding.pagesParent.addView(
            PagesView(
                requireContext(),
                coroutineContext,
                book,
                positionProcessor,
                model.showOverview
            ), layoutParams
        )

        binding.pagesParent.addView(
            OverviewView(
                requireContext(),
                coroutineContext,
                book,
                positionProcessor,
                model.showOverview
            ), layoutParams
        )
    }

    companion object {
        fun create(bookId: BookID): ReaderFragment {
            val fragment = ReaderFragment()
            fragment.arguments = Bundle().apply {
                putParcelable(ARG_BOOK_ID, bookId)
            }
            return fragment
        }

        private const val ARG_BOOK_ID = "ARG_BOOK_ID"

        private val layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }
}