package org.danielzfranklin.librereader.ui.screen.library

import android.graphics.Bitmap
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.navigation.NavDeepLinkRequest
import androidx.navigation.NavHostController
import androidx.test.espresso.matcher.ViewMatchers.assertThat
import io.mockk.*
import kotlinx.coroutines.flow.flow
import org.danielzfranklin.librereader.R
import org.danielzfranklin.librereader.model.BookID
import org.danielzfranklin.librereader.model.BookMeta
import org.danielzfranklin.librereader.model.BookPosition
import org.danielzfranklin.librereader.model.BookStyle
import org.danielzfranklin.librereader.repo.Repo
import org.danielzfranklin.librereader.ui.LocalRepo
import org.danielzfranklin.librereader.ui.MainActivity
import org.danielzfranklin.librereader.ui.theme.LibreReaderTheme
import org.hamcrest.CoreMatchers.`is`
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class LibraryTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private val id1 = BookID("test:1")
    private val id2 = BookID("test:2")
    private lateinit var repo: Repo

    private var receivedBooks = false

    private lateinit var navController: NavHostController

    @Before
    fun setUp() {
        repo = mockk {
            every { listBooks() } returns flow {
                emit(
                    listOf(
                        BookMeta(id1, BookPosition(id1, 0f, 0, 0), BookStyle(), "Book 1", 0, 0),
                        BookMeta(id2, BookPosition(id2, 0f, 0, 0), BookStyle(), "Book 2", 0, 0),
                    )
                )
                receivedBooks = true
            }

            coEvery { getCover(any()) } returns Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        }

        navController = mockk()

        rule.setContent {
            LibreReaderTheme {
                CompositionLocalProvider(LocalRepo provides repo) {
                    LibraryScreen(navController)
                }
            }
        }
    }

    @Test
    fun displaysTopBar() {
        rule.onNodeWithTag("topAppBar").assert(hasAnyChild(hasText("Library")))
        val importDesc = rule.activity.getString(R.string.import_book)
        rule.onNodeWithTag("topAppBar").assert(hasAnyChild(hasContentDescription(importDesc)))
    }

    @Test
    fun displaysBookTitles() {
        rule.onNodeWithText("Book 1").assertIsDisplayed()
        rule.onNodeWithText("Book 2").assertIsDisplayed()
    }

    @Test
    fun clickingBookNavigatesToReaderForIt() {
        val request = slot<NavDeepLinkRequest>()
        every { navController.navigate(capture(request), any()) } just Runs

        rule.onNodeWithText("Book 2").performClick()

        assertThat(
            request.captured.uri.toString(),
            `is`("android-app://androidx.navigation.compose/reader/test:2")
        )
    }
}