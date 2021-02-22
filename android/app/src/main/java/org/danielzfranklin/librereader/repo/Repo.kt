package org.danielzfranklin.librereader.repo

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import nl.siegmann.epublib.domain.Book
import nl.siegmann.epublib.epub.EpubReader
import org.danielzfranklin.librereader.model.BookID
import org.danielzfranklin.librereader.model.BookMeta
import org.danielzfranklin.librereader.model.BookPosition
import org.danielzfranklin.librereader.model.BookStyle
import java.util.zip.ZipInputStream
import kotlin.coroutines.CoroutineContext

class Repo(
    override val coroutineContext: CoroutineContext,
    private val bookDao: BookDao,
    private val epubImportFactory: EpubImport.Factory,
    private val bookFilesFactory: BookFiles.Factory
) : CoroutineScope {
    fun listBooks(): Flow<List<BookMeta>> = bookDao.list()

    suspend fun updatePosition(position: BookPosition) {
        bookDao.updatePosition(position)
    }

    suspend fun importBook(uri: Uri): BookID {
        val meta = epubImportFactory.get(uri).import()
        bookDao.insert(meta)
        return meta.id
    }

    suspend fun getCover(id: BookID): Bitmap? = withContext(Dispatchers.IO) {
        bookFilesFactory.open(id)?.coverBitmap()
    }

    fun getBookStyleFlow(id: BookID): Flow<BookStyle?> = bookDao.getBookStyleFlow(id)

    suspend fun getPosition(id: BookID): BookPosition? = bookDao.getPosition(id)

    fun getPositionFlow(id: BookID): Flow<BookPosition?> = bookDao.getPositionFlow(id)

    suspend fun getEpub(id: BookID): Book? = withContext(Dispatchers.IO) {
        val files = bookFilesFactory.open(id) ?: return@withContext null

        files.epubFile.inputStream().use { inputStream ->
            ZipInputStream(inputStream).use { zipStream ->
                // False positive, see <https://youtrack.jetbrains.com/issue/KTIJ-830>
                @Suppress("BlockingMethodInNonBlockingContext")
                EpubReader().readEpub(zipStream)
            }
        }
    }

    companion object {
        fun create(coroutineScope: CoroutineScope, context: Context): Repo {
            val bookDao = BookDao.create(context)
            val epubImportFactory = EpubImport.Factory(context, coroutineScope.coroutineContext)
            val bookFilesFactory = BookFiles.Factory(context)
            return Repo(
                coroutineScope.coroutineContext,
                bookDao,
                epubImportFactory,
                bookFilesFactory
            )
        }
    }
}