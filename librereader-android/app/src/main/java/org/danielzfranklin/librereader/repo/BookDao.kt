package org.danielzfranklin.librereader.repo

import android.content.Context
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import com.squareup.sqldelight.runtime.coroutines.mapToOneOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.danielzfranklin.librereader.Database
import org.danielzfranklin.librereader.model.*

class BookDao(private val context: Context, private val database: Database) {
    val queries = database.dbBookQueries

    fun list(): Flow<List<BookMeta>> =
        queries.listTitleOrder(::metaMapper)
            .asFlow()
            .mapToList()

    suspend fun updatePosition(position: BookPosition) = withContext(Dispatchers.IO) {
        queries.updatePosition(
            id = position.id.toString(),
            percent = position.percent,
            sectionIndex = position.sectionIndex,
            charIndex = position.charIndex
        )
    }

    suspend fun insert(meta: BookMeta) = withContext(Dispatchers.IO) {
        queries.create(
            id = meta.id.toString(),
            title = meta.title,
            coverBgColor = meta.coverBgColor,
            coverTextColor = meta.coverTextColor,
            textColor = meta.style.textColor,
            bgColor = meta.style.bgColor,
            typeface = meta.style.typeface.id,
            textSizeSp = meta.style.textSizeInSp,
            paddingDp = meta.style.paddingInDp,
        )
    }

    fun getBookStyleFlow(id: BookID): Flow<BookStyle?> =
        queries.getStyle(id.toString())
            .asFlow()
            .mapToOneOrNull()
            .map {
                it?.let {
                    BookStyle(
                        it.textColor,
                        it.bgColor,
                        BookTypeface.fromName(it.typeface)!!,
                        it.textSizeSp,
                        it.paddingDp
                    )
                }
            }

    suspend fun getPosition(id: BookID): BookPosition? = withContext(Dispatchers.IO) {
        queries.getPosition(id.toString()).executeAsOneOrNull()?.let {
            BookPosition(id, it.percent, it.sectionIndex, it.charIndex)
        }
    }

    private fun metaMapper(
        idString: String, title: String, coverBgColor: Int, coverTextColor: Int, percent: Float,
        sectionIndex: Int, charIndex: Int, textColor: Int, bgColor: Int, typefaceName: String,
        textSize: Float, padding: Int
    ): BookMeta {
        val id = BookID(idString)
        val position = BookPosition(id, percent, sectionIndex, charIndex)
        val typeface = BookTypeface.fromName(typefaceName)!!
        val style = BookStyle(textColor, bgColor, typeface, textSize, padding)
        val cover = BookFiles.open(context, id)!!.coverBitmap()!!
        return BookMeta(id, position, style, cover, title, coverBgColor, coverTextColor)
    }
}