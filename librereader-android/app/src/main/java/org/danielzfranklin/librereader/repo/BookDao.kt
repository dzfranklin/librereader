package org.danielzfranklin.librereader.repo

import android.content.Context
import com.squareup.sqldelight.android.AndroidSqliteDriver
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import com.squareup.sqldelight.runtime.coroutines.mapToOneOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.danielzfranklin.librereader.Database
import org.danielzfranklin.librereader.db.DbBookStyle
import org.danielzfranklin.librereader.model.*

class BookDao(database: Database) {
    private val queries = database.dbBookQueries

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
            color = meta.style.color,
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
                        it.color,
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
        sectionIndex: Int, charIndex: Int, color: PresetColor, typefaceName: String,
        textSize: Float, padding: Int
    ): BookMeta {
        val id = BookID(idString)
        val position = BookPosition(id, percent, sectionIndex, charIndex)
        val typeface = BookTypeface.fromName(typefaceName)!!
        val style = BookStyle(color, typeface, textSize, padding)
        return BookMeta(id, position, style, title, coverBgColor, coverTextColor)
    }

    companion object {
        fun create(context: Context): BookDao {
            val dbDriver = AndroidSqliteDriver(Database.Schema, context, DB_FILE)
            return BookDao(
                Database(
                    dbDriver,
                    dbBookStyleAdapter = DbBookStyle.Adapter(
                        colorAdapter = PresetColorAdapter()
                    )
                )
            )
        }

        const val DB_FILE = "main.db"
    }
}