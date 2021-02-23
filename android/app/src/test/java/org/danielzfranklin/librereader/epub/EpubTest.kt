package org.danielzfranklin.librereader.epub

import androidx.compose.ui.text.AnnotatedString
import org.danielzfranklin.librereader.model.BookID
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.not
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.IsNull
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class EpubTest {
    val id = BookID("testid:1")
    val section = EpubSection(id, 0, AnnotatedString("Foo"))

    @Test
    fun cachesSectionAccess() {
        val computes = mutableMapOf<Int, Int>()
        val subject = Epub(id, 10, getSection = { index ->
            computes.merge(index, 1) { a, b -> a + b }
            section
        })

        subject.section(0)
        subject.section(0)
        subject.section(1)

        assertThat(computes[0], `is`(1))
        assertThat(computes[1], `is`(1))
        assertThat(computes[2], IsNull())
    }

    @Test
    fun sectionOutOfBoundsReturnsNotNull() {
        val subject = Epub(id, 0, getSection = { section })
        assertThat(subject.section(0), not(IsNull()))
    }

    @Test
    fun sectionOutOfBoundsReturnsNullWithoutTryingToCompute() {
        val subject = Epub(id, 0) { throw IllegalArgumentException("Should never be called") }
        assertThat(subject.section(1), IsNull())
    }
}