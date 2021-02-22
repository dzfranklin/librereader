package org.danielzfranklin.librereader.ui

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.danielzfranklin.librereader.model.BookID
import org.danielzfranklin.librereader.ui.screen.library.LibraryScreen

@Composable
fun Nav() {
    val navController = rememberNavController()

    NavHost(navController, startDestination = Screen.Library.route) {
        composable(Screen.Library.route) { LibraryScreen(navController) }
        composable(Screen.Reader.route) {
            val bookId = BookID(it.arguments!!.getString("bookId")!!)
            Text(bookId.toString())
        }
    }
}