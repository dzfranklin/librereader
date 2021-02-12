package org.danielzfranklin.librereader.ui.library

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.danielzfranklin.librereader.R
import org.danielzfranklin.librereader.databinding.LibraryActivityBinding
import org.danielzfranklin.librereader.repo.Repo
import org.danielzfranklin.librereader.ui.reader.ReaderActivity
import timber.log.Timber

class LibraryActivity : AppCompatActivity(R.layout.reader_fragment), CoroutineScope {
    override val coroutineContext = lifecycleScope.coroutineContext

    private lateinit var binding: LibraryActivityBinding
    private val model: LibraryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LibraryActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                add(R.id.libraryFragmentContainer, LibraryFragment())
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.library_toolbar, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_import -> {
            launchPicker()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun launchPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = EPUB_MIME
            putExtra(DocumentsContract.EXTRA_INITIAL_URI, Environment.DIRECTORY_DOWNLOADS)
        }
        startActivityForResult(intent, PICK_EPUB_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode != PICK_EPUB_REQUEST_CODE) {
            return
        }

        if (resultCode != RESULT_OK) {
            Timber.w("Cancelling import because of non-ok status %s", resultCode)
            return
        }

        val uri = data?.data
        if (uri == null) {
            Timber.w("Exiting import activity because of null data uri")
            return
        }

        launch {
            model.import(uri)
        }
    }

    companion object {
        private const val PICK_EPUB_REQUEST_CODE = 2
        private const val EPUB_MIME = "application/epub+zip"
    }
}