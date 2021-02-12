package org.danielzfranklin.librereader.repo

import android.content.ContentResolver
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import nl.siegmann.epublib.epub.EpubReader
import org.danielzfranklin.librereader.LibreReaderApplication
import org.danielzfranklin.librereader.R
import org.danielzfranklin.librereader.model.*
import java.io.InputStream
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap

class Repo(private val app: LibreReaderApplication) {
    // TODO: Replace with real store
    private val positions = ConcurrentHashMap<BookID, MutableStateFlow<BookPosition>>()
    private val store = ConcurrentHashMap<BookID, Book>()
    private val metaStore = ConcurrentHashMap<BookID, BookMeta>()

    fun listBooks(): StateFlow<List<BookMeta>> {
        return MutableStateFlow(metaStore.values.toList()).asStateFlow()
    }

    fun updatePosition(id: BookID, position: BookPosition) {
        positions[id]!!.value = position
    }

    fun importBook(uri: Uri): BookID {
        val idStream = openUri(uri)
        val stream = openUri(uri)

        // TODO: compute in parallel
        val id = BookID.forEpub(idStream)
        val epub = EpubReader().readEpub(stream)

        val position = MutableStateFlow(BookPosition(id, 0f, 0, 0))
        val style = BookStyle()
        positions[id] = position
        store[id] = Book(id, style, position.asStateFlow(), epub)

        val coverSource = ImageDecoder.createSource(ByteBuffer.wrap(epub.coverImage.data))
        val cover = ImageDecoder.decodeDrawable(coverSource) { decoder, _, _ ->
            // required to read the pixels to generate a swatch
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
        val palette = Palette.from(cover.toBitmap(cover.intrinsicWidth, cover.intrinsicHeight))
            .generate()
        val swatch = if (palette.vibrantSwatch != null) {
            palette.vibrantSwatch
        } else {
            palette.mutedSwatch
        }
        val coverBg = swatch?.rgb ?: app.getColor(R.color.libraryBookDefaultBg)
        val coverText = swatch?.bodyTextColor ?: app.getColor(R.color.libraryBookDefaultText)
        metaStore[id] = BookMeta(id, cover, epub.title, position.value, coverBg, coverText)
        return id
    }

    fun getBook(id: BookID): Book? {
        return store[id]
    }

    private fun openUri(uri: Uri): InputStream {
        // TODO: Error handling
        return app.contentResolver.openInputStream(uri)!!
    }

    companion object {
        private var instance: Repo? = null

        fun initialize(application: LibreReaderApplication) {
            instance = Repo(application)

            // TODO: Remove
            instance!!.importBook(
                Uri.Builder()
                    .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                    .authority(application.packageName)
                    .path(R.raw.frankenstein_public_domain.toString())
                    .build()
            )

            instance!!.importBook(
                Uri.Builder()
                    .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                    .authority(application.packageName)
                    .path(R.raw.count_monte_cristo_public_domain.toString())
                    .build()
            )


            instance!!.importBook(
                Uri.Builder()
                    .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                    .authority(application.packageName)
                    .path(R.raw.rur_public_domain.toString())
                    .build()
            )

            instance!!.importBook(
                Uri.Builder()
                    .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                    .authority(application.packageName)
                    .path(R.raw.hadji_murad_public_domain.toString())
                    .build()
            )
        }

        fun get(): Repo {
            return instance ?: throw IllegalStateException("Repo not initialized")
        }
    }
}