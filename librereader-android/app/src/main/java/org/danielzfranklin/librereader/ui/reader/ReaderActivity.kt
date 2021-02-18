package org.danielzfranklin.librereader.ui.reader

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import org.danielzfranklin.librereader.R
import org.danielzfranklin.librereader.databinding.ReaderActivityBinding
import org.danielzfranklin.librereader.model.BookID
import org.danielzfranklin.librereader.ui.reader.ReaderViewModel.Progress
import org.danielzfranklin.librereader.ui.reader.overview.OverviewFragment
import org.danielzfranklin.librereader.ui.reader.pages.PagesFragment
import org.danielzfranklin.librereader.util.find
import timber.log.Timber

class ReaderActivity : AppCompatActivity(R.layout.reader_activity), CoroutineScope {
    override val coroutineContext = lifecycleScope.coroutineContext

    private lateinit var binding: ReaderActivityBinding
    private lateinit var bookId: BookID
    private val model: ReaderViewModel by viewModels { ReaderViewModel.Factory(bookId) }
    private lateinit var pages: PagesFragment
    private lateinit var overview: OverviewFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bookId = intent.getParcelableExtra(EXTRA_BOOK_ID)
            ?: throw IllegalArgumentException("Missing required extra EXTRA_BOOK_ID")

        binding = ReaderActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        launch {
            model.progress.collect {
                when (it) {
                    is Progress.Loading -> {
                        binding.progress.visibility = View.VISIBLE
                        binding.cover.visibility = View.GONE
                        binding.container.visibility = View.INVISIBLE
                    }
                    is Progress.LoadingHasCover -> {
                        binding.progress.visibility = View.VISIBLE
                        binding.cover.apply {
                            visibility = View.VISIBLE
                            setImageBitmap(it.cover)
                        }
                        binding.container.visibility = View.INVISIBLE
                    }
                    is Progress.Loaded -> {
                        binding.progress.visibility = View.GONE
                        binding.cover.visibility = View.GONE
                        binding.container.visibility = View.VISIBLE

                        binding.container.post { // Ensure doesn't run before laid out
                            launch(this@ReaderActivity.coroutineContext) {
                                onLoaded(
                                    ReaderFragment.Data.from(
                                        it.data,
                                        this@ReaderActivity,
                                        binding.container
                                    )
                                )
                            }
                        }

                        cancel("Loaded")
                    }
                }
            }
        }
    }

    private fun onLoaded(data: ReaderFragment.Data) {
        Timber.d("Loaded book ${data.id}")

        pages = supportFragmentManager.find(PAGES_FRAGMENT) ?: PagesFragment()
        overview = supportFragmentManager.find(OVERVIEW_FRAGMENT) ?: OverviewFragment()

        launch {
            pages.onData(data)
        }
        launch {
            overview.onData(data)
        }

        supportFragmentManager.commit {
            setReorderingAllowed(true)
            add(R.id.container, overview)
            add(R.id.container, pages)
        }

        launch {
            data.inOverview.collect { inOverview ->
                supportFragmentManager.commit {
                    setReorderingAllowed(true)

                    if (inOverview) {
                        detach(pages)
                        attach(overview)
                    } else {
                        detach(overview)
                        attach(pages)
                    }
                }
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

        private const val PAGES_FRAGMENT = "pages"
        private const val OVERVIEW_FRAGMENT = "overview"
    }
}
