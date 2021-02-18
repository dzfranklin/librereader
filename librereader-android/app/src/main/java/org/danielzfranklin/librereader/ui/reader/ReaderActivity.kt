package org.danielzfranklin.librereader.ui.reader

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.tinder.StateMachine
import kotlinx.coroutines.*
import org.danielzfranklin.librereader.R
import org.danielzfranklin.librereader.databinding.ReaderActivityBinding
import org.danielzfranklin.librereader.model.BookID
import org.danielzfranklin.librereader.ui.reader.overview.OverviewFragment
import org.danielzfranklin.librereader.ui.reader.pages.PagesFragment
import org.danielzfranklin.librereader.util.find
import timber.log.Timber

class ReaderActivity : AppCompatActivity(R.layout.reader_activity), CoroutineScope {
    override val coroutineContext = lifecycleScope.coroutineContext

    private sealed class State {
        object Loading : State()
        data class LoadingHasCover(val cover: Bitmap) : State()
        data class ProcessingData(val data: ReaderFragment.DisplayIndependentData) : State()
        object InitializingFragments : State()
        object Pages : State()
        object Overview : State()
    }

    private sealed class Event {
        data class GotCover(val cover: Bitmap) : Event()
        data class GotData(val data: ReaderFragment.DisplayIndependentData) : Event()
        data class GotProcessedData(val data: ReaderFragment.Data) : Event()
        object InitializedFragments : Event()
        object SwitchToOverview : Event()
        object SwitchToPages : Event()
    }

    private sealed class SideEffect {
        data class DisplayCoverWhileLoading(val cover: Bitmap) : SideEffect()
        data class ProcessData(val data: ReaderFragment.DisplayIndependentData) : SideEffect()
        data class InitializeFragments(val data: ReaderFragment.Data) : SideEffect()
        object ShowPages : SideEffect()
        object ShowOverview : SideEffect()
    }

    private val state = StateMachine.create<State, Event, SideEffect> {
        initialState(State.Loading)

        state<State.Loading> {
            on<Event.GotCover> {
                transitionTo(
                    State.LoadingHasCover(it.cover),
                    SideEffect.DisplayCoverWhileLoading(it.cover)
                )
            }
        }

        state<State.LoadingHasCover> {
            on<Event.GotData> {
                transitionTo(State.ProcessingData(it.data), SideEffect.ProcessData(it.data))
            }
        }

        state<State.ProcessingData> {
            on<Event.GotProcessedData> {
                transitionTo(State.InitializingFragments, SideEffect.InitializeFragments(it.data))
            }
        }

        state<State.InitializingFragments> {
            on<Event.InitializedFragments> {
                transitionTo(State.Pages, SideEffect.ShowPages)
            }
        }

        state<State.Pages> {
            on<Event.SwitchToOverview> {
                transitionTo(State.Overview, SideEffect.ShowOverview)
            }
        }

        state<State.Overview> {
            on<Event.SwitchToPages> {
                transitionTo(State.Pages, SideEffect.ShowPages)
            }
        }

        onTransition {
            when (it) {
                is StateMachine.Transition.Valid -> {
                    Timber.i("State transition %s", it)
                    it.sideEffect?.let { effect -> performSideEffect(effect) }
                }
                is StateMachine.Transition.Invalid ->
                    throw IllegalStateException("Invalid transition $it")
            }
        }
    }

    private lateinit var binding: ReaderActivityBinding
    private lateinit var bookId: BookID
    private val model: ReaderViewModel by viewModels { ReaderViewModel.Factory(bookId) }
    private lateinit var pages: PagesFragment
    private lateinit var overview: OverviewFragment

    private fun performSideEffect(effect: SideEffect) {
        when (effect) {
            is SideEffect.DisplayCoverWhileLoading -> {
                binding.cover.apply {
                    visibility = View.VISIBLE
                    setImageBitmap(effect.cover)
                }
            }

            is SideEffect.ProcessData -> launch {
                val data =
                    ReaderFragment.Data.from(effect.data, this@ReaderActivity, binding.container)
                state.transition(Event.GotProcessedData(data))
            }

            is SideEffect.InitializeFragments -> {
                pages = supportFragmentManager.find(PAGES_FRAGMENT) ?: PagesFragment()
                overview = supportFragmentManager.find(OVERVIEW_FRAGMENT) ?: OverviewFragment()

                launch {
                    listOf(async {
                        pages.onData(effect.data)
                    }, async {
                        overview.onData(effect.data)
                    }).awaitAll()

                    withContext(Dispatchers.Main) {
                        supportFragmentManager.commit {
                            setReorderingAllowed(true)
                            add(R.id.container, overview)
                            add(R.id.container, pages)
                        }

                        binding.progress.visibility = View.GONE
                        binding.cover.visibility = View.GONE
                    }

                    state.transition(Event.InitializedFragments)
                }
            }

            is SideEffect.ShowPages -> supportFragmentManager.commit {
                setReorderingAllowed(true)
                detach(overview)
                attach(pages)
            }

            is SideEffect.ShowOverview -> supportFragmentManager.commit {
                setReorderingAllowed(true)
                detach(pages)
                attach(overview)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bookId = intent.getParcelableExtra(EXTRA_BOOK_ID)
            ?: throw IllegalArgumentException("Missing required extra EXTRA_BOOK_ID")

        binding = ReaderActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        launch {
            val loading = model.load()

            val cover = loading.cover.await()
            state.transition(Event.GotCover(cover))

            val data = loading.data.await()
            state.transition(Event.GotData(data))
        }
    }

    fun switchToPages() {
        state.transition(Event.SwitchToPages)
    }

    fun switchToOverview() {
        state.transition(Event.SwitchToOverview)
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
