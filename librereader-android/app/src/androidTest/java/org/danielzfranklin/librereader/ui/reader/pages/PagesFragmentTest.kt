package org.danielzfranklin.librereader.ui.reader.pages

import android.view.ViewGroup
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.Swipe
import androidx.test.espresso.action.ViewActions.swipeLeft
import androidx.test.espresso.action.ViewActions.swipeRight
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withTagValue
import io.mockk.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import org.danielzfranklin.librereader.TestSampleBookDisplay
import org.danielzfranklin.librereader.clickPercent
import org.danielzfranklin.librereader.model.BookPosition
import org.danielzfranklin.librereader.model.BookStyle
import org.danielzfranklin.librereader.swipeSlightlyLeft
import org.danielzfranklin.librereader.swipeSlightlyRight
import org.danielzfranklin.librereader.ui.reader.PositionProcessor
import org.danielzfranklin.librereader.ui.reader.ReaderFragment
import org.danielzfranklin.librereader.ui.reader.displayModel.BookDisplay
import org.danielzfranklin.librereader.ui.reader.displayModel.BookPageDisplay
import org.danielzfranklin.librereader.ui.reader.displayModel.BookSectionDisplay
import org.danielzfranklin.librereader.ui.reader.withPageText
import org.hamcrest.CoreMatchers
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import timber.log.Timber

@RunWith(JUnit4::class)
class PagesFragmentTest {
    private lateinit var scenario: FragmentScenario<PagesFragment>
    private lateinit var data: ReaderFragment.Data
    private lateinit var book: BookDisplay
    private val sections: List<BookSectionDisplay> by lazy { book.sections }

    @Before
    fun setUp() {
        scenario = launchFragmentInContainer()
        data = mockk()

        // Small font size means fewer pages
        val style = BookStyle(textSizeInSp = 6f)
        scenario.onFragment {
            book = TestSampleBookDisplay(
                it.requireContext(),
                BookPageDisplay.fitParent(it.view as ViewGroup, style)
            )
        }

        every { data.display } returns MutableStateFlow(book)
        every { data.id } returns book.id
    }

    private fun provideData() = scenario.onFragment { it.onData(data) }

    @Test
    fun startsOnCorrectPage() {
        val positionProcessor = mockk<PositionProcessor>()
        every { data.position } returns positionProcessor
        val pos = BookPosition.startOf(book).movedBy(book, 1)!!
        every { positionProcessor.value } returns pos
        every { positionProcessor.events } returns MutableStateFlow(
            PositionProcessor.Change(-1, pos)
        )

        provideData()

        onView(withCurrentPage()).check(matches(withPageText(pos.page(book))))
    }

    @Test
    fun pageNextOnce() {
        var pos: BookPosition = BookPosition.startOf(book)
        val positionProcessor = spyk(PositionProcessor(Job(), pos))
        every { data.position } returns positionProcessor

        excludeRecords {
            positionProcessor getProperty "value"
            positionProcessor getProperty "events"
        }

        provideData()

        pos = pos.movedBy(book, 1)!!
        onPagesView().perform(swipeLeft())

        verify {
            positionProcessor.set(any(), pos)
        }

        confirmVerified(positionProcessor)
    }

    @Test
    fun pageNext() {
        var pos: BookPosition? = BookPosition.startOf(book)
        val positionProcessor = spyk(PositionProcessor(Job(), pos!!))
        every { data.position } returns positionProcessor

        excludeRecords {
            positionProcessor getProperty "value"
            positionProcessor getProperty "events"
        }

        provideData()

        for (sectionIndex in sections.indices) {
            for ((pageIndex, page) in sections[sectionIndex].pages().withIndex()) {
                Timber.d("On page %s of section %s", pageIndex, sectionIndex)
                Timber.d("Current position %s", positionProcessor.value)

                onView(withCurrentPage())
                    .check(matches(withPageText(page)))

                pos = pos?.movedBy(book, 1)

                onPagesView().perform(swipeLeft())

                if (pos != null) {
                    verify(timeout = 1000) {
                        positionProcessor.set(any(), pos)
                    }
                }
            }
        }

        confirmVerified(positionProcessor)
    }

    @Test
    fun pagePrev() {
        var pos: BookPosition? = BookPosition.endOf(book)
        val positionProcessor = spyk(PositionProcessor(Job(), pos!!))
        every { data.position } returns positionProcessor

        excludeRecords {
            positionProcessor getProperty "value"
            positionProcessor getProperty "events"
        }

        provideData()

        for (sectionIndex in sections.indices.reversed()) {
            for ((pageIndex, page) in sections[sectionIndex].pages().withIndex().reversed()) {
                Timber.d("On page %s of section %s", pageIndex, sectionIndex)
                Timber.d("Current position %s", positionProcessor.value)

                onView(withCurrentPage())
                    .check(matches(withPageText(page)))

                pos = pos?.movedBy(book, -1)

                onPagesView().perform(swipeRight())

                if (pos != null) {
                    verify(timeout = 1000) {
                        positionProcessor.set(any(), pos)
                    }
                }
            }
        }

        confirmVerified(positionProcessor)
    }

    @Test
    fun flingPageNext() {
        every { data.position } returns PositionProcessor(
            Job(),
            BookPosition.startOfSection(book, 1)
        )
        provideData()

        onPagesView()
            .perform(swipeSlightlyLeft(Swipe.FAST))

        onView(withCurrentPage())
            .check(matches(withPageText(sections[1].pages()[1])))
    }

    @Test
    fun flingPagePrev() {
        every { data.position } returns PositionProcessor(
            Job(),
            BookPosition.startOfSection(book, 1).movedBy(book, 1)!!
        )
        provideData()

        onPagesView()
            .perform(swipeSlightlyRight(Swipe.FAST))

        onView(withCurrentPage())
            .check(matches(withPageText(sections[1].pages()[0])))
    }

    @Test
    fun cancelPageNext() {
        // TODO: On my Pixel this ends up being a selection event
        every { data.position } returns PositionProcessor(Job(), BookPosition.startOf(book))
        provideData()

        onPagesView()
            .perform(swipeSlightlyLeft(Swipe.SLOW))

        onView(withCurrentPage())
            .check(matches(withPageText(sections[0].pages()[0])))
    }

    @Test
    fun cancelPagePrev() {
        // TODO: On my Pixel this ends up being a selection event
        every { data.position } returns PositionProcessor(
            Job(),
            BookPosition.startOfSection(book, 1).movedBy(book, 1)!!
        )
        provideData()

        onPagesView()
            .perform(swipeSlightlyRight(Swipe.SLOW))

        onView(withCurrentPage())
            .check(matches(withPageText(sections[1].pages()[1])))
    }

    @Test
    fun swipePrevOnFirstPage() {
        every { data.position } returns PositionProcessor(Job(), BookPosition.startOf(book))
        provideData()

        onPagesView()
            .perform(swipeRight())

        onView(withCurrentPage())
            .check(matches(withPageText(sections[0].pages()[0])))
    }

    @Test
    fun swipeNextOnLastPage() {
        every { data.position } returns PositionProcessor(Job(), BookPosition.endOf(book))
        provideData()

        onPagesView()
            .perform(swipeLeft())

        onView(withCurrentPage())
            .check(matches(withPageText(sections.last().pages().last())))
    }

    @Test
    fun tapNext() {
        every { data.position } returns PositionProcessor(
            Job(),
            BookPosition.startOfSection(book, 1)
        )
        provideData()

        onPagesView()
            .perform(clickPercent(0.9f, 0.9f))

        onView(withCurrentPage())
            .check(matches(withPageText(sections[1].pages()[1])))
    }

    @Test
    fun tapPrev() {
        every { data.position } returns PositionProcessor(
            Job(),
            BookPosition.startOfSection(book, 1)
        )
        provideData()

        onPagesView()
            .perform(clickPercent(0.1f, 0.9f))

        onView(withCurrentPage())
            .check(matches(withPageText(sections[0].pages().last())))
    }

    @Test
    fun tapOverview() {
        TODO()
    }

    private fun onPagesView() = onView(withId(org.danielzfranklin.librereader.R.id.pages))

    private fun withCurrentPage() = withTagValue(CoreMatchers.`is`("currentPage"))
}