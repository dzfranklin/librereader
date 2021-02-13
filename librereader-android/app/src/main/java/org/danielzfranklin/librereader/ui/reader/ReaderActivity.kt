package org.danielzfranklin.librereader.ui.reader

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import org.danielzfranklin.librereader.R
import org.danielzfranklin.librereader.databinding.ReaderActivityBinding
import org.danielzfranklin.librereader.model.BookID

class ReaderActivity : AppCompatActivity() {
    private lateinit var binding: ReaderActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bookId: BookID = intent.getParcelableExtra(EXTRA_BOOK_ID)
            ?: throw IllegalArgumentException("Missing required extra EXTRA_BOOK_ID")

        binding = ReaderActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            // Fragments are restored automatically, see <https://blog.propaneapps.com/android/fragments-restoration-magic/>
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                add(R.id.readerFragmentContainer, ReaderFragment.create(bookId))
            }
        }
    }

    companion object {
        fun start(context: Context, bookId: BookID) =
            context.startActivity(startIntent(context, bookId))

        fun startIntent(context: Context, bookId: BookID) =
            Intent(context, ReaderActivity::class.java).apply {
                putExtra(EXTRA_BOOK_ID, bookId)
            }

        private const val EXTRA_BOOK_ID = "EXTRA_BOOK_ID"
    }
}