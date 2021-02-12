package org.danielzfranklin.librereader.ui.library

import android.os.Bundle
import android.os.PersistableBundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import org.danielzfranklin.librereader.R
import org.danielzfranklin.librereader.databinding.LibraryActivityBinding

class LibraryActivity: AppCompatActivity(R.layout.reader_fragment) {
    private lateinit var binding: LibraryActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LibraryActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                add(R.id.libraryFragmentContainer, LibraryFragment())
            }
        }
    }
}