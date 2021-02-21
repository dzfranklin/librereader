package org.danielzfranklin.librereader.repo

import android.content.Context
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.compose.ui.graphics.toArgb
import androidx.palette.graphics.Palette
import kotlinx.coroutines.*
import nl.siegmann.epublib.epub.EpubReader
import org.danielzfranklin.librereader.model.BookID
import org.danielzfranklin.librereader.model.BookMeta
import org.danielzfranklin.librereader.model.BookPosition
import org.danielzfranklin.librereader.model.BookStyle
import org.danielzfranklin.librereader.ui.theme.bookCoverFallbackBackgroundColor
import org.danielzfranklin.librereader.ui.theme.bookCoverFallbackTextColor
import org.danielzfranklin.librereader.util.writeTo
import java.io.File
import java.io.InputStream
import java.nio.ByteBuffer
import java.util.*
import java.util.zip.ZipInputStream
import kotlin.coroutines.CoroutineContext

// False positive, see <https://youtrack.jetbrains.com/issue/KTIJ-830>
@Suppress("BlockingMethodInNonBlockingContext")
class EpubImport(
    coroutineContext: CoroutineContext,
    private val context: Context,
    private val uri: Uri
) : CoroutineScope {
    override val coroutineContext =
        CoroutineScope(coroutineContext + Dispatchers.IO).coroutineContext

    class Factory(private val context: Context, private val coroutineContext: CoroutineContext) {
        fun get(uri: Uri) = EpubImport(coroutineContext, context, uri)
    }

    private val tempUuid = UUID.randomUUID().toString()

    private val inProgressDir = context.getDir(IN_PROGRESS_DIR, Context.MODE_PRIVATE)

    private data class ParsedData(
        val title: String,
        val coverStream: InputStream,
        val coverBg: Int,
        val coverText: Int
    )

    suspend fun import(): BookMeta = coroutineScope {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Cannot open input stream")
        val tempFile = File(inProgressDir, tempUuid)
        inputStream.writeTo(tempFile)

        val idDeferred = async {
            withContext(Dispatchers.IO) {
                BookID.forEpub(tempFile.inputStream())
            }
        }

        val parsedDeferred = async {
            val epub = tempFile.inputStream().use { inputStream ->
                ZipInputStream(inputStream).use { zipStream ->
                    EpubReader().readEpub(zipStream)
                }
            }

            val coverSource =
                ImageDecoder.createSource(ByteBuffer.wrap(epub.coverImage.data))
            val cover = ImageDecoder.decodeBitmap(coverSource) { decoder, _, _ ->
                // required to read the pixels to generate a swatch
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }

            val palette = Palette.from(cover).generate()
            val swatch = if (palette.vibrantSwatch != null) {
                palette.vibrantSwatch
            } else {
                palette.mutedSwatch
            }

            val coverBg = swatch?.rgb ?: bookCoverFallbackBackgroundColor.toArgb()
            val coverText =
                swatch?.bodyTextColor ?: bookCoverFallbackTextColor.toArgb()

            ParsedData(epub.title, epub.coverImage.inputStream, coverBg, coverText)
        }

        val id = idDeferred.await()
        val parsed = parsedDeferred.await()

        val position = BookPosition(id, 0f, 0, 0)
        val style = BookStyle()

        val bookFiles = BookFiles.Factory(context).create(id)
        awaitAll(
            async {
                parsed.coverStream.writeTo(bookFiles.coverFile)
            },
            async {
                tempFile.inputStream().writeTo(bookFiles.epubFile)
            }
        )
        tempFile.delete()

        BookMeta(id, position, style, parsed.title, parsed.coverBg, parsed.coverText)
    }

    companion object {
        private const val IN_PROGRESS_DIR = "imported_books_in_progress"
    }
}