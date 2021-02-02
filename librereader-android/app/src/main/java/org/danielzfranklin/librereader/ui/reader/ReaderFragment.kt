package org.danielzfranklin.librereader.ui.reader

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import org.danielzfranklin.librereader.databinding.FragmentReaderBinding

class ReaderFragment : Fragment() {
    private lateinit var binding: FragmentReaderBinding
    private val model: ReaderViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = FragmentReaderBinding.inflate(layoutInflater, container, false)
        return binding.root
    }
}