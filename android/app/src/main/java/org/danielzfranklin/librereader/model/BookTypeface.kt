package org.danielzfranklin.librereader.model

import android.content.Context
import android.content.res.Resources
import android.graphics.Typeface
import androidx.annotation.StringRes
import org.danielzfranklin.librereader.R
import timber.log.Timber

data class BookTypeface(
    val id: String,
    @StringRes private val displayName: Int,
    private val resourceId: Int
) {
    fun get(context: Context): Typeface {
        return try {
            context.resources.getFont(resourceId)
        } catch (e: Resources.NotFoundException) {
            Timber.w("Resource for typeface %s %s (id %s) missing", id, displayName, resourceId)
            context.resources.getFont(DEFAULT_RESOURCE)
        }
    }

    companion object {
        private const val DEFAULT_RESOURCE = R.font.crimson_pro
        val DEFAULT = BookTypeface("crimson_pro", R.string.crimson_pro_font, DEFAULT_RESOURCE)

        private val typefaces = mapOf(
            DEFAULT.id to DEFAULT
        )

        fun list(): Collection<BookTypeface> = typefaces.values

        fun fromName(name: String): BookTypeface? = typefaces[name]
    }
}