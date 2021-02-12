package org.danielzfranklin.librereader.ui.reader

import android.content.ContentResolver
import android.net.Uri
import android.text.Spanned
import android.view.View
import android.view.ViewGroup
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.*
import androidx.test.espresso.action.ViewActions.swipeLeft
import androidx.test.espresso.action.ViewActions.swipeRight
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withTagValue
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.runBlocking
import nl.siegmann.epublib.domain.Book
import org.danielzfranklin.librereader.R
import org.danielzfranklin.librereader.instrumentation
import org.danielzfranklin.librereader.model.BookID
import org.danielzfranklin.librereader.model.BookMeta
import org.danielzfranklin.librereader.model.BookPosition
import org.danielzfranklin.librereader.repo.Repo
import org.danielzfranklin.librereader.ui.reader.displayModel.BookDisplay
import org.danielzfranklin.librereader.ui.reader.displayModel.BookPageDisplay
import org.danielzfranklin.librereader.ui.reader.displayModel.BookSectionDisplay
import org.hamcrest.BaseMatcher
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.Description
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import timber.log.Timber
import org.danielzfranklin.librereader.test.R as TestR

@RunWith(JUnit4::class)
class TestPagesView {
    private lateinit var scenario: ActivityScenario<ReaderActivity>
    private lateinit var id: BookID
    private lateinit var repo: Repo
    private lateinit var meta: StateFlow<BookMeta>
    private lateinit var epub: Book
    private lateinit var display: BookDisplay
    private lateinit var sections: List<BookSectionDisplay>

    @Before
    fun setUp() {
        repo = Repo.get()
        id = runBlocking {
            repo.importBook(
                Uri.Builder()
                    .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                    .authority(instrumentation.context.packageName)
                    .path(TestR.raw.frankenstein_public_domain_partial.toString())
                    .build()
            )
        }

        scenario =
            ActivityScenario.launch(ReaderActivity.startIntent(instrumentation.targetContext, id))

        runBlocking {
            val metaLoad = async { repo.getBook(id).stateIn(this) }
            val epubLoad = async { repo.getEpub(id) }
            meta = metaLoad.await()
            epub = epubLoad.await()
        }

        display = createDisplay(meta.value, epub)
        sections = display.sections
    }

    @Test
    fun pageNext() {
        for (sectionIndex in sections.indices) {
            for ((pageIndex, page) in sections[sectionIndex].pages().withIndex()) {
                Timber.d("On page %s of section %s", pageIndex, sectionIndex)
                Timber.i("Current position %s", meta.value.position)

                onView(withCurrentPage())
                    .check(matches(withPageText(page)))

                onReaderView().perform(swipeLeft())
                Thread.sleep(50)
            }
        }
    }

    @Test
    fun pagePrev() {
        runBlocking {
            repo.updatePosition(BookPosition.endOf(display))
        }

        for (sectionIndex in sections.indices.reversed()) {
            for ((pageIndex, page) in sections[sectionIndex].pages().withIndex().reversed()) {
                Timber.i("Expected page %s of section %s", pageIndex, sectionIndex)
                Timber.i("Current position %s", meta.value.position)

                onView(withCurrentPage())
                    .check(matches(withPageText(page)))

                onReaderView().perform(swipeRight())
                Thread.sleep(250)
            }
        }
    }


    @Test
    fun cancelPageNext() {
        // TODO: On my Pixel this ends up being a selection event
        onReaderView().perform(swipeSlightlyLeft(Swipe.SLOW))
        onView(withCurrentPage()).check(matches(withPageText(sections[0].pages()[0])))
    }

    @Test
    fun flingPageNext() {
        onReaderView().perform(swipeSlightlyLeft(Swipe.FAST))
        Thread.sleep(250)
        onView(withCurrentPage()).check(matches(withPageText(sections[0].pages()[1])))
    }


    @Test
    fun cancelPagePrev() {
        // TODO: On my Pixel this ends up being a selection event
        onReaderView().perform(swipeLeft())
        Thread.sleep(10)
        onView(withCurrentPage()).check(matches(withPageText(sections[0].pages()[1])))

        onReaderView().perform(swipeSlightlyRight(Swipe.SLOW))
        onView(withCurrentPage()).check(matches(withPageText(sections[0].pages()[1])))
    }

    @Test
    fun flingPagePrev() {
        onReaderView().perform(swipeLeft())
        Thread.sleep(10)
        onView(withCurrentPage()).check(matches(withPageText(sections[0].pages()[1])))

        onReaderView().perform(swipeSlightlyRight(Swipe.FAST))
        onView(withCurrentPage()).check(matches(withPageText(sections[0].pages()[0])))
    }

    @Test
    fun swipePrevOnFirstPage() {
        onReaderView().perform(swipeRight())
        Thread.sleep(250)
        onView(withCurrentPage()).check(matches(withPageText(sections[0].pages()[0])))
    }

    @Test
    fun swipeNextOnLastPage() {
        val lastPage = sections.last().pages().last()

        runBlocking {
            repo.updatePosition(BookPosition.endOf(display))
        }
        Thread.sleep(10)
        onView(withCurrentPage()).check(matches(withPageText(lastPage)))

        onReaderView().perform(swipeLeft())
        onView(withCurrentPage()).check(matches(withPageText(lastPage)))
    }

    private fun swipeSlightlyLeft(swiper: Swiper) = GeneralSwipeAction(
        swiper,
        { view ->
            val coords = GeneralLocation.CENTER_RIGHT.calculateCoordinates(view)
            coords[0] -= 100f
            coords
        },
        { view ->
            val coords = GeneralLocation.CENTER_RIGHT.calculateCoordinates(view)
            coords[0] *= 0.8f
            coords
        },
        Press.FINGER
    )

    private fun swipeSlightlyRight(swiper: Swiper) = GeneralSwipeAction(
        swiper,
        { view ->
            val coords = GeneralLocation.CENTER_LEFT.calculateCoordinates(view)
            coords[0] = 100f
            coords
        },
        { view ->
            val coords = GeneralLocation.CENTER_RIGHT.calculateCoordinates(view)
            coords[0] *= 0.2f
            coords
        },
        Press.FINGER
    )

    private fun createDisplay(meta: BookMeta, epub: Book): BookDisplay {
        var display: BookDisplay? = null
        scenario.onActivity { activity ->
            display = BookDisplay(
                activity, meta.id, epub, BookPageDisplay.fitParent(
                    activity.requireViewById<ViewGroup>(R.id.readerLayout),
                    meta.style
                )
            )
        }
        // Will hang until onActivity called

        return display!!
    }

    private fun onReaderView() = onView(withId(R.id.readerLayout))

    private fun withCurrentPage() = withTagValue(`is`("currentPage"))

    private fun withPageText(text: Spanned) = WithPageTextMatcher(text)

    private class WithPageTextMatcher(private val text: Spanned) : BaseMatcher<View>() {
        override fun describeTo(description: Description?) {
            description?.appendText("With text: ${text.toString().trim()}")
        }

        override fun matches(item: Any?): Boolean {
            return item is PageView &&
                    item.text.toString().trim() == text.toString().trim()
        }
    }
}