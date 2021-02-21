package org.danielzfranklin.librereader.ui.screen.library

import android.graphics.Bitmap
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import junit.framework.Assert.assertEquals
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.danielzfranklin.librereader.model.BookID
import org.danielzfranklin.librereader.model.BookMeta
import org.danielzfranklin.librereader.model.BookPosition
import org.danielzfranklin.librereader.model.BookStyle
import org.danielzfranklin.librereader.repo.Repo
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.system.measureTimeMillis

@RunWith(JUnit4::class)
class LibraryModelTest {
    @Test
    fun parallelizesCoverLoads() {
        val id1 = BookID("test:1")
        val id2 = BookID("test:2")

        val repo = mockk<Repo> {
            every { listBooks() } returns flowOf(
                listOf(
                    BookMeta(id1, BookPosition(id1, 0f, 0, 0), BookStyle(), "", 0, 0),
                    BookMeta(id2, BookPosition(id2, 0f, 0, 0), BookStyle(), "", 0, 0),
                )
            )

            coEvery { getCover(any()) } coAnswers {
                when (val id = this.arg<BookID>(0)) {
                    id1 -> delay(500)
                    id2 -> delay(400)
                    else -> throw IllegalArgumentException("Unexpected id $id")
                }

                Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
            }
        }

        val subject = LibraryModel(repo)

        val millis = runBlocking {
            measureTimeMillis {
                subject.books.first()
            }
        }

        assertEquals("Should take time equal to the longer delay.", 500f, millis.toFloat(), 200f)
    }
}