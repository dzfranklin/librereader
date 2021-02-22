package org.danielzfranklin.librereader.epub

import android.graphics.drawable.Drawable
import kotlinx.coroutines.CoroutineScope
import nl.siegmann.epublib.domain.Book
import nl.siegmann.epublib.domain.Resources
import kotlin.coroutines.CoroutineContext

class Epub(
    override val coroutineContext: CoroutineContext,
    private val epub: Book,
) : CoroutineScope {
    val resources: Resources = epub.resources
    val maxSection = epub.spine.spineReferences.size - 1

    private val sections = mutableMapOf<Int, EpubSection>()

    fun section(index: Int): EpubSection? {
        val cached = sections[index]
        return if (cached != null) {
            cached
        } else {
            val ref = epub.spine.spineReferences.getOrNull(index) ?: return null
            EpubSection(this, ref)
        }
    }
}