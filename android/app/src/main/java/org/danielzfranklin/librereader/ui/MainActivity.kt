package org.danielzfranklin.librereader.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Providers
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.danielzfranklin.librereader.model.BookID
import org.danielzfranklin.librereader.repo.Repo
import org.danielzfranklin.librereader.ui.screen.library.LibraryScreen
import org.danielzfranklin.librereader.ui.theme.LibreReaderTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            App()
        }
    }
}

val LocalRepo = staticCompositionLocalOf<Repo> { throw IllegalStateException("Repo uninitialized") }

@Composable
fun App() {
    val navController = rememberNavController()
    val coroutineScope = rememberCoroutineScope()
    val repo = Repo.create(coroutineScope, LocalContext.current)

    LibreReaderTheme {
        Scaffold {
            Providers(LocalRepo provides repo) {
                NavHost(navController, startDestination = Screen.Library.route) {
                    composable(Screen.Library.route) { LibraryScreen(navController) }
                    composable(Screen.Reader.route) {
                        val bookId = BookID(it.arguments!!.getString("bookId")!!)
                        Text(bookId.toString())
                    }
                }
            }
        }
    }
}
