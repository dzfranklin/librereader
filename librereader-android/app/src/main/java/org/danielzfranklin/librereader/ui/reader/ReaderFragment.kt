package org.danielzfranklin.librereader.ui.reader

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import org.danielzfranklin.librereader.R
import org.danielzfranklin.librereader.databinding.ReaderFragmentBinding
import java.io.InputStream

@Suppress("unused") // used in fragment_main.xml
class ReaderFragment : Fragment() {
    private lateinit var binding: ReaderFragmentBinding
    private lateinit var model: ReaderViewModel
    private lateinit var pagesView: ReaderPagesView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = ReaderFragmentBinding.inflate(layoutInflater, container, false)
        model = ViewModelProvider(
            this,
            ReaderViewModel.Factory(requireActivity().application, getSampleBook())
        ).get(ReaderViewModel::class.java)

        pagesView = ReaderPagesView(
            requireContext(),
            lifecycleScope.coroutineContext,
            model.book,
            model.initialPosition
        )
        binding.pagesParent.addView(pagesView, binding.pagesParent.layoutParams)

        return binding.root
    }

    private fun getSampleBook(): InputStream {
        return resources.openRawResource(R.raw.frankenstein_public_domain)
    }
}