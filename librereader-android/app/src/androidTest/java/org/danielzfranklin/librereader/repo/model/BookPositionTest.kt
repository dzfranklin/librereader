package org.danielzfranklin.librereader.repo.model

import androidx.test.espresso.matcher.ViewMatchers.assertThat
import androidx.test.platform.app.InstrumentationRegistry
import org.danielzfranklin.librereader.TestBookDisplay
import org.danielzfranklin.librereader.ui.reader.displayModel.BookDisplay
import org.danielzfranklin.librereader.ui.reader.displayModel.BookPageDisplay
import org.hamcrest.CoreMatchers.`is`
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import timber.log.Timber

@RunWith(JUnit4::class)
internal class BookPositionTest {
    private lateinit var display: BookDisplay
    private lateinit var pageDisplay: BookPageDisplay

    @Before
    fun setUp() {
        pageDisplay = BookPageDisplay(
            width = 500,
            height = 1000,
            style = BookStyle()
        )

        val context = InstrumentationRegistry.getInstrumentation().context
        display = TestBookDisplay(context, pageDisplay)
    }

    @Test
    fun pageIndexShouldRoundTrip() {
        for (i in 0 until display.pageCount()) {
            val pos = BookPosition.fromPageIndex(display, i)
                ?: throw NullPointerException("Could not create pos from i=$i")

            val result = pos.pageIndex(display)
            assert(pos.pageIndex(display) == i) {
                """
                    |i = $i
                    |BookPosition.fromPageIndex(display, i) = $pos
                    |BookPosition.fromPageIndex(display, i).pageIndex(display) = $result
                """.trimMargin()
            }
        }
    }

    @Test
    fun moveBy1Forwards() {
        var position = BookPosition.startOf(display)
        var expectedCharIndex = 0
        var sectionIndex = 0
        for (expectedPageIndex in 0 until display.pageCount()) {
            val measured = position.pageIndex(display)
            Timber.i("Position %s", position)

            assertThat("Wrong page (position: $position)", measured, `is`(expectedPageIndex))
            assertThat(
                "Wrong charIndex (position $position, expectedPageIndex: $expectedPageIndex)",
                position.charIndex,
                `is`(expectedCharIndex)
            )

            // don't advance if at the last page
            if (expectedPageIndex != display.pageCount() - 1) {
                expectedCharIndex += position.page(display).length

                position = position.movedBy(display, 1)
                    ?: throw IllegalStateException("Null position for page $expectedPageIndex")

                if (sectionIndex != position.sectionIndex) {
                    expectedCharIndex = 0
                    sectionIndex = position.sectionIndex
                }
            }
        }
    }

    @Test
    fun moveBy1Backwards() {
        val endPosition = BookPosition.endOf(display)
        var position = endPosition

        var expectedCharIndex =
            display.sections.last().textLength - endPosition.page(display).length
        var sectionIndex = display.sections.size - 1

        for (expectedPageIndex in display.pageCount() - 1 downTo 0) {
            val measured = position.pageIndex(display)
            Timber.i("Position %s, page %s", position, expectedPageIndex)

            assertThat("Wrong page (position: $position)", measured, `is`(expectedPageIndex))

            if (position != endPosition) {
                assertThat(
                    "Wrong charIndex (position $position, expectedPageIndex: $expectedPageIndex)",
                    position.charIndex,
                    `is`(expectedCharIndex)
                )
            }

            // don't advance if at the last page
            if (expectedPageIndex != 0) {
                position = position.movedBy(display, -1)
                    ?: throw IllegalStateException("Null position for page $expectedPageIndex")

                if (sectionIndex == position.sectionIndex) {
                    expectedCharIndex -= position.page(display).length
                } else {
                    sectionIndex = position.sectionIndex
                    val section = display.sections[sectionIndex]
                    // start of the last page
                    expectedCharIndex = section.textLength - section.pages().last().length
                }

            }
        }
    }
}