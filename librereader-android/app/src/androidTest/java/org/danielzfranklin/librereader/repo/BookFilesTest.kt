package org.danielzfranklin.librereader.repo

import android.content.Context
import androidx.test.espresso.matcher.ViewMatchers.assertThat
import org.danielzfranklin.librereader.getResource
import org.danielzfranklin.librereader.instrumentation
import org.danielzfranklin.librereader.model.BookID
import org.danielzfranklin.librereader.util.writeTo
import org.hamcrest.core.IsNot
import org.hamcrest.core.IsNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class BookFilesTest {
    private lateinit var context: Context

    private fun id(n: Int) = BookID("testbookid:$n")

    @Before
    fun setUp() {
        context = instrumentation.targetContext
    }

    @Test
    fun createDoesNotThrow() {
        BookFiles.create(context, id(1))
    }

    @Test
    fun openReturnsNullForNonexistent() {
        assertThat(BookFiles.open(context, id(2)), IsNull())
    }

    @Test
    fun openReturnsNotNullForCreated() {
        val id = id(3)
        BookFiles.create(context, id)

        val subject = BookFiles.open(context, id)

        assertThat(subject, IsNot(IsNull()))
    }

    @Test
    fun decodesSampleCover() {
        val subject = BookFiles.create(context, id(4))

        getResource("test_cover.jpg")!!.writeTo(subject.coverFile)

        subject.coverBitmap()
    }

    @Test
    fun handlesDecodeOnNonexistentCover() {
        val subject = BookFiles.create(context, id(5))
        subject.coverBitmap()
    }
}