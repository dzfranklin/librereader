package org.danielzfranklin.librereader.ui.reader

import android.text.Spanned
import android.view.View
import android.view.ViewGroup
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.*
import androidx.test.espresso.action.ViewActions.swipeLeft
import androidx.test.espresso.action.ViewActions.swipeRight
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withTagValue
import androidx.test.ext.junit.rules.ActivityScenarioRule
import org.danielzfranklin.librereader.ui.MainActivity
import org.danielzfranklin.librereader.R
import org.danielzfranklin.librereader.repo.Repo
import org.danielzfranklin.librereader.model.Book
import org.danielzfranklin.librereader.model.BookID
import org.danielzfranklin.librereader.model.BookPosition
import org.danielzfranklin.librereader.ui.reader.displayModel.BookDisplay
import org.danielzfranklin.librereader.ui.reader.displayModel.BookPageDisplay
import org.danielzfranklin.librereader.ui.reader.displayModel.BookSectionDisplay
import org.hamcrest.BaseMatcher
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.Description
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import timber.log.Timber

@RunWith(JUnit4::class)
class TestPagesView {
    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    // TODO: when we support launching a specified book, launch this one
    private val id = BookID("librereaderidv1:a5acbb017c7081216724f86b20ac1eda")

    private lateinit var repo: Repo
    private lateinit var book: Book
    private lateinit var display: BookDisplay
    private lateinit var sections: List<BookSectionDisplay>

    @Before
    fun setUp() {
        repo = Repo.get()
        book = repo.getBook(id)!!
        display = createDisplay(book)
        sections = display.sections
    }

    @Test
    fun pageNext() {
        for (sectionIndex in sections.indices.take(3)) {
            for ((pageIndex, page) in sections[sectionIndex].pages().withIndex()) {
                Timber.d("On page %s of section %s", pageIndex, sectionIndex)

                onView(withCurrentPage())
                    .check(matches(withPageText(page)))

                onReaderView().perform(swipeLeft())
                Thread.sleep(50)
            }
        }
    }

    @Test
    fun pagePrev() {
        repo.updatePosition(id, BookPosition.endOf(display))

        for (sectionIndex in sections.indices.reversed().take(3)) {
            for ((pageIndex, page) in sections[sectionIndex].pages().withIndex().reversed()) {
                Timber.i("Expected page %s of section %s", pageIndex, sectionIndex)
                Timber.i("Current position %s", book.position.value)

                onView(withCurrentPage())
                    .check(matches(withPageText(page)))

                onReaderView().perform(swipeRight())
                Thread.sleep(250)
            }
        }
    }


    @Test
    fun cancelPageNext() {
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

        repo.updatePosition(id, BookPosition.endOf(display))
        Thread.sleep(10)
        onView(withCurrentPage()).check(matches(withPageText(lastPage)))

        onReaderView().perform(swipeLeft())
        onView(withCurrentPage()).check(matches(withPageText(lastPage)))
    }

    private fun swipeSlightlyLeft(swiper: Swiper) = GeneralSwipeAction(
        swiper,
        GeneralLocation.CENTER_RIGHT,
        { view ->
            val coords = GeneralLocation.CENTER_RIGHT.calculateCoordinates(view)
            coords[0] *= 0.8f
            coords
        },
        Press.FINGER
    )

    private fun swipeSlightlyRight(swiper: Swiper) = GeneralSwipeAction(
        swiper,
        GeneralLocation.CENTER_LEFT,
        { view ->
            val coords = GeneralLocation.CENTER_RIGHT.calculateCoordinates(view)
            coords[0] *= 0.2f
            coords
        },
        Press.FINGER
    )

    private fun createDisplay(book: Book): BookDisplay {
        var display: BookDisplay? = null
        activityRule.scenario.onActivity { activity ->
            display = BookDisplay(
                activity, book.id, book.epub, BookPageDisplay.fitParent(
                    activity.requireViewById<ViewGroup>(R.id.readerLayout),
                    book.style
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