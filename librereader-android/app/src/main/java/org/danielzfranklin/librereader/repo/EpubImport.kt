package org.danielzfranklin.librereader.repo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.palette.graphics.Palette
import kotlinx.coroutines.*
import nl.siegmann.epublib.epub.EpubReader
import org.danielzfranklin.librereader.R
import org.danielzfranklin.librereader.model.BookID
import org.danielzfranklin.librereader.model.BookMeta
import org.danielzfranklin.librereader.model.BookPosition
import org.danielzfranklin.librereader.model.BookStyle
import java.io.File
import java.io.FileOutputStream
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

    private val tempUuid = UUID.randomUUID().toString()

    private val inProgressDir = context.getDir(IN_PROGRESS_DIR, Context.MODE_PRIVATE)

    private data class ParsedData(
        val title: String,
        val coverStream: InputStream,
        val cover: Bitmap,
        val coverBg: Int,
        val coverText: Int
    )

    suspend fun import(): BookMeta = coroutineScope {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Cannot open input stream")
        val tempFile = File(inProgressDir, tempUuid)
        writeStream(inputStream, tempFile)

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

            val coverBg = swatch?.rgb ?: context.getColor(R.color.libraryBookDefaultBg)
            val coverText =
                swatch?.bodyTextColor ?: context.getColor(R.color.libraryBookDefaultText)

            ParsedData(epub.title, epub.coverImage.inputStream, cover, coverBg, coverText)
        }

        val id = idDeferred.await()
        val parsed = parsedDeferred.await()

        val position = BookPosition(id, 0f, 0, 0)
        val style = BookStyle()

        val bookFiles = BookFiles(context, id)
        bookFiles.initialize()
        awaitAll(
            async {
                writeStream(parsed.coverStream, bookFiles.coverFile)
            },
            async {
                writeStream(tempFile.inputStream(), bookFiles.epubFile)
            }
        )
        tempFile.delete()

        BookMeta(id, position, style, parsed.cover, parsed.title, parsed.coverBg, parsed.coverText)
    }

    private fun writeStream(inputStream: InputStream, outFile: File) {
        inputStream.use { input ->
            FileOutputStream(outFile).use { output ->
                // See <https://stackoverflow.com/a/56074084>
                val buffer = ByteArray(4 * 1024)
                while (true) {
                    val byteCount = input.read(buffer)
                    if (byteCount < 0) {
                        break
                    }
                    output.write(buffer, 0, byteCount)
                }
                output.flush()
            }
        }
    }

    companion object {
        private const val IN_PROGRESS_DIR = "imported_books_in_progress"
    }
}