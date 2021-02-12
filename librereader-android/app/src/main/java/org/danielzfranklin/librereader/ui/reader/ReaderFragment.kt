package org.danielzfranklin.librereader.ui.reader

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.danielzfranklin.librereader.databinding.ReaderFragmentBinding
import org.danielzfranklin.librereader.model.BookID
import org.danielzfranklin.librereader.ui.reader.displayModel.BookDisplay
import org.danielzfranklin.librereader.ui.reader.displayModel.BookPageDisplay
import org.danielzfranklin.librereader.ui.reader.overviewView.OverviewView
import org.danielzfranklin.librereader.ui.reader.pagesView.PagesView
import timber.log.Timber

class ReaderFragment : Fragment(), View.OnLayoutChangeListener {
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

        lifecycleScope.launch {
            model.style.collect {
                if (rootLaidOut) {
                    recreateViews()
                } else {
                    Timber.i("Skipping recreating pages view because onLayoutChange will do so later")
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

        recreateViews()
    }

    private fun recreateViews() {
        binding.parent.removeAllViews()

        val pageDisplay = BookPageDisplay.fitParent(binding.parent, model.style.value)

        val book = BookDisplay(requireContext(), model.bookId, model.epub, pageDisplay)

        binding.parent.addView(
            PagesView(
                requireContext(),
                lifecycleScope.coroutineContext,
                book,
                model.positionProcessor,
                model.showOverview
            ), layoutParams
        )

        binding.parent.addView(
            OverviewView(
                requireContext(),
                lifecycleScope.coroutineContext,
                book,
                model.positionProcessor,
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