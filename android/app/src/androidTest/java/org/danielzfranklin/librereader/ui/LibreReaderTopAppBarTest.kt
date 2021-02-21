package org.danielzfranklin.librereader.ui

import androidx.compose.material.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import junit.framework.Assert.assertTrue
import org.danielzfranklin.librereader.ui.theme.LibreReaderTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class LibreReaderTopAppBarTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun containsTitle() {
        rule.setContent {
            LibreReaderTheme {
                LibreReaderTopAppBar(title = "Title Text") {
                    Text("Action A")
                    Text("Action B")
                }
            }
        }

        rule.onNodeWithText("Title Text").assertIsDisplayed()
    }

    @Test
    fun containsActionsInCorrectOrder() {
        rule.setContent {
            LibreReaderTheme {
                LibreReaderTopAppBar("Title Text") {
                    Text("Action A")
                    Text("Action B")
                }
            }
        }

        val actionA = rule.onNodeWithText("Action A")
        val actionB = rule.onNodeWithText("Action B")

        actionA.assertIsDisplayed()
        actionB.assertIsDisplayed()

        val actionABounds = actionA.fetchSemanticsNode().boundsInRoot
        val actionBBounds = actionB.fetchSemanticsNode().boundsInRoot

        assertTrue(
            "actionA comes before actionB",
            actionABounds.left < actionBBounds.left
        )
    }
}