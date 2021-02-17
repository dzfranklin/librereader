package org.danielzfranklin.librereader.ui.library

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.danielzfranklin.librereader.R
import org.danielzfranklin.librereader.databinding.LibraryFragmentBinding
import org.danielzfranklin.librereader.model.BookID
import org.danielzfranklin.librereader.ui.reader.ReaderActivity

class LibraryFragment : Fragment(R.layout.library_fragment), CoroutineScope {
    override val coroutineContext = lifecycleScope.coroutineContext

    private lateinit var binding: LibraryFragmentBinding
    // Ensures we get the same instance as the activity
    private val model: LibraryViewModel by viewModels({ requireActivity() })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        binding = LibraryFragmentBinding.inflate(layoutInflater, container, false)

        binding.books.layoutManager = GridLayoutManager(context, 3)
        val adapter = BooksAdapter(::onBookClick) { id -> model.getCover(id) }
        binding.books.adapter = adapter
        launch {
            model.books.collect {
                adapter.update(it)
            }
        }

        launch {
            model.isImportInProgress.collect {
                binding.importingProgress.visibility = if (it) View.VISIBLE else View.INVISIBLE
                binding.importingProgress.isIndeterminate = it
            }
        }

        return binding.root
    }

    private fun onBookClick(id: BookID) {
        ReaderActivity.start(requireContext(), id)
    }
}