package org.danielzfranklin.librereader.repo

import android.graphics.Color
import androidx.test.espresso.matcher.ViewMatchers.assertThat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.first
import org.danielzfranklin.librereader.instrumentation
import org.danielzfranklin.librereader.model.BookID
import org.danielzfranklin.librereader.model.BookMeta
import org.danielzfranklin.librereader.model.BookPosition
import org.danielzfranklin.librereader.model.BookStyle
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.collection.IsEmptyCollection
import org.hamcrest.core.IsEqual
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4


@RunWith(JUnit4::class)
class BookDaoTest {
    private lateinit var subject: BookDao

    private val idA = BookID("test:a")
    private val bookA = BookMeta(
        id = idA,
        title = "TitleA",
        coverBgColor = Color.BLUE,
        coverTextColor = Color.WHITE,
        position = BookPosition(idA, 0f, 0, 0),
        style = BookStyle()
    )
    private val idB = BookID("test:b")
    private val bookB = BookMeta(
        id = idB,
        title = "TitleA",
        coverBgColor = Color.BLUE,
        coverTextColor = Color.WHITE,
        position = BookPosition(idB, 0f, 0, 0),
        style = BookStyle()
    )

    @Before
    fun setUp() {
        val context = instrumentation.targetContext
        subject = BookDao.create(context)
    }

    @After
    fun tearDown() {
        instrumentation.targetContext.deleteDatabase(BookDao.DB_FILE)
    }

    @Test
    fun listWhenEmpty() {
        val list = runBlocking { subject.list().first() }
        assertThat(list, IsEmptyCollection())
    }

    @Test
    fun list() {
        runBlocking {
            subject.insert(bookA)
            subject.insert(bookB)
        }

        val list = runBlocking { subject.list().first() }
        assertThat(list.size, `is`(2))

        val (out1, out2) = list
        // We don't check order
        if (out1 == bookA) {
            assertThat(bookB, IsEqual(out2))
        } else {
            assertThat(bookB, IsEqual(out1))
            assertThat(bookA, IsEqual(out2))
        }
    }

    @Test
    fun listFlowsNewBooks() {
        try {
            runBlocking {
                launch {
                    subject.insert(bookA)
                    delay(100)
                    subject.insert(bookB)
                }
                delay(100)

                subject.list().collectIndexed { index, list ->
                    if (index == 0) {
                        assertThat(list.size, `is`(1))
                        assertThat(bookA, IsEqual(list[0]))
                    } else {
                        assertThat(list.size, `is`(2))

                        val (out1, out2) = list
                        // We don't check order
                        if (out1 == bookA) {
                            assertThat(bookB, IsEqual(out2))
                        } else {
                            assertThat(bookB, IsEqual(out1))
                            assertThat(bookA, IsEqual(out2))
                        }

                        cancel("Done")
                    }
                }
            }
        } catch (_: CancellationException) { }
    }
}