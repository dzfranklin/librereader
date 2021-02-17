package org.danielzfranklin.librereader.ui.reader.overview

import android.view.ViewGroup
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import org.danielzfranklin.librereader.R
import org.danielzfranklin.librereader.TestSampleBookDisplay
import org.danielzfranklin.librereader.clickPercent
import org.danielzfranklin.librereader.isPageView
import org.danielzfranklin.librereader.model.BookPosition
import org.danielzfranklin.librereader.model.BookStyle
import org.danielzfranklin.librereader.ui.reader.PositionProcessor
import org.danielzfranklin.librereader.ui.reader.ReaderFragment
import org.danielzfranklin.librereader.ui.reader.displayModel.BookDisplay
import org.danielzfranklin.librereader.ui.reader.displayModel.BookPageDisplay
import org.danielzfranklin.librereader.ui.reader.displayModel.BookSectionDisplay
import org.danielzfranklin.librereader.ui.reader.withPageText
import org.hamcrest.CoreMatchers.allOf
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class OverviewFragmentTest {
    private lateinit var scenario: FragmentScenario<OverviewFragment>
    private lateinit var data: ReaderFragment.Data
    private lateinit var book: BookDisplay
    private val sections: List<BookSectionDisplay> by lazy { book.sections }

    @Before
    fun setUp() {
        scenario = launchFragmentInContainer()
        data = mockk()

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
        val pos = BookPosition.startOf(book).movedBy(book, 2)!!
        every { data.position } returns PositionProcessor(Job(), pos)
        provideData()

        onView(allOf(isPageView(), isCompletelyDisplayed()))
            .check(matches(withPageText(pos.page(book))))
    }

    @Test
    fun tapNext() {
        val pos = BookPosition.startOf(book)
        every { data.position } returns PositionProcessor(Job(), pos)
        provideData()

        onPagesView()
            .perform(clickPercent(0.9f, 0.4f))

        onView(allOf(isPageView(), isCompletelyDisplayed()))
            .check(matches(withPageText(pos.movedBy(book, 1)!!.page(book))))
    }

    @Test
    fun tapPrev() {
        TODO("This fails because of incorrect start scroll position")
        val pos = BookPosition.startOfSection(book, 1).movedBy(book, 1)!!
        every { data.position } returns PositionProcessor(Job(), pos)
        provideData()

        onPagesView()
            .perform(clickPercent(0.1f, 0.4f))

        onView(allOf(isPageView(), isCompletelyDisplayed()))
            .check(matches(withPageText(pos.movedBy(book, -1)!!.page(book))))
    }

    @Test
    fun jumpToPercent() {
        TODO("Click bottom bar")
    }

    private fun onPagesView() = onView(withId(R.id.pages))
}