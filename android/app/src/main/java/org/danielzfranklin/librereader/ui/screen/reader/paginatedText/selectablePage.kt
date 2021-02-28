package org.danielzfranklin.librereader.ui.screen.reader.paginatedText

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.ResolvedTextDirection
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.danielzfranklin.librereader.ui.screen.reader.PageRenderer
import kotlin.math.max

internal fun Modifier.selectablePage(page: PageRenderer, enabled: State<Boolean>) =
    if (!enabled.value) this else graphicsLayer().composed {
        // Based on Compose MultiWidgetSelectionDelegate, with many features we don't need stripped out

        val selection = rememberSaveable(page) { mutableStateOf<Selection?>(null) }
        val colors = LocalTextSelectionColors.current

        pointerInput(page) {
            var dragBegin: Offset? = null

            coroutineScope {
                var tapDetector: Job? = null

                launch {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { offset ->
                            selection.value = getSelectionInfo(
                                page = page,
                                startPosition = offset,
                                endPosition = offset,
                                previous = null,
                            )

                            dragBegin = offset

                            tapDetector = launch {
                                detectTapGestures {
                                    selection.value = null
                                    dragBegin = null
                                    tapDetector = null
                                    cancel("Cancel on tap")
                                }
                            }
                        },
                        onDrag = { change, _ ->
                            selection.value = getSelectionInfo(
                                page = page,
                                startPosition = dragBegin!!,
                                endPosition = change.position,
                                previous = selection.value!!
                            )
                            change.consumeAllChanges()
                        })
                }
            }

            // TODO: Handles
        } then graphicsLayer().drawBehind {
            val range = selection.value?.toTextRange() ?: return@drawBehind
            if (range.min == range.max) return@drawBehind

            val path = page.getPathForRange(range.min, range.max)
            drawPath(path, colors.backgroundColor)
        }
    }

private fun getSelectionInfo(
    page: PageRenderer,
    startPosition: Offset,
    endPosition: Offset,
    previous: Selection?,
    wordBased: Boolean = true,
    isStartHandleDragged: Boolean = false,
): Selection? {
    // Based on Compose MultiWidgetSelectionDelegate getSelectionInfo, processAsSingleComposable,
    // and getRefinedSelectionInfo, with many features we don't need stripped out

    var startOffset = page.getOffsetForPosition(startPosition)
    var endOffset = page.getOffsetForPosition(endPosition)

    if (startOffset == endOffset) {
        // If the start and end offset are at the same character, and it's not the initial
        // selection, then bound to at least one character.
        val textRange = ensureAtLeastOneChar(
            offset = startOffset,
            lastOffset = page.lastOffset,
            previousSelection = previous?.toTextRange(),
            isStartHandle = isStartHandleDragged,
            handlesCrossed = previous?.handlesCrossed ?: false
        )
        startOffset = textRange.start
        endOffset = textRange.end
    }

    // nothing is selected
    if (startOffset == -1 && endOffset == -1) return null

    // Check if the start and end handles are crossed each other.
    val handlesCrossed = startOffset > endOffset

    // If under long press, update the selection to word-based.
    if (wordBased) {
        val startWordBoundary = page.getWordBoundary(startOffset.coerceIn(0, page.lastOffset))
        val endWordBoundary = page.getWordBoundary(endOffset.coerceIn(0, page.lastOffset))

        // If handles are not crossed, start should be snapped to the start of the word containing the
        // start offset, and end should be snapped to the end of the word containing the end offset.
        // If handles are crossed, start should be snapped to the end of the word containing the start
        // offset, and end should be snapped to the start of the word containing the end offset.
        startOffset = if (handlesCrossed) startWordBoundary.end else startWordBoundary.start
        endOffset = if (handlesCrossed) endWordBoundary.start else endWordBoundary.end
    }


    return Selection(
        start = Selection.AnchorInfo(
            direction = page.getBidiRunDirection(startOffset),
            offset = startOffset,
        ),
        end = Selection.AnchorInfo(
            direction = page.getBidiRunDirection(max(endOffset - 1, 0)),
            offset = endOffset,
        ),
        handlesCrossed = handlesCrossed
    )
}


/**
 * Copied from Compose
 *
 * This method adjusts the raw start and end offset and bounds the selection to one character. The
 * logic of bounding evaluates the last selection result, which handle is being dragged, and if
 * selection reaches the boundary.
 *
 * @param offset unprocessed start and end offset calculated directly from input position, in
 * this case start and offset equals to each other.
 * @param lastOffset last offset of the text. It's actually the length of the text.
 * @param previousSelection previous selected text range.
 * @param isStartHandle true if the start handle is being dragged
 * @param handlesCrossed true if the selection handles are crossed
 *
 * @return the adjusted [TextRange].
 */
private fun ensureAtLeastOneChar(
    offset: Int,
    lastOffset: Int,
    previousSelection: TextRange?,
    isStartHandle: Boolean,
    handlesCrossed: Boolean
): TextRange {
    // When lastOffset is 0, it can only return an empty TextRange.
    // When previousSelection is null, it won't start a selection and return an empty TextRange.
    if (lastOffset == 0 || previousSelection == null) return TextRange(offset, offset)

    // When offset is at the boundary, the handle that is not dragged should be at [offset]. Here
    // the other handle's position is computed accordingly.
    if (offset == 0) {
        return if (isStartHandle) {
            TextRange(1, 0)
        } else {
            TextRange(0, 1)
        }
    }

    if (offset == lastOffset) {
        return if (isStartHandle) {
            TextRange(lastOffset - 1, lastOffset)
        } else {
            TextRange(lastOffset, lastOffset - 1)
        }
    }

    // In other cases, this function will try to maintain the current cross handle states.
    // Only in this way the selection can be stable.
    return if (isStartHandle) {
        if (!handlesCrossed) {
            // Handle is NOT crossed, and the start handle is dragged.
            TextRange(offset - 1, offset)
        } else {
            // Handle is crossed, and the start handle is dragged.
            TextRange(offset + 1, offset)
        }
    } else {
        if (!handlesCrossed) {
            // Handle is NOT crossed, and the end handle is dragged.
            TextRange(offset, offset + 1)
        } else {
            // Handle is crossed, and the end handle is dragged.
            TextRange(offset, offset - 1)
        }
    }
}

/**
 * Copied from Compose
 * Information about the current Selection.
 */
@Immutable
@OptIn(ExperimentalTextApi::class)
internal data class Selection(
    /**
     * Information about the start of the selection.
     */
    val start: AnchorInfo,

    /**
     * Information about the end of the selection.
     */
    val end: AnchorInfo,
    /**
     * The flag to show that the selection handles are dragged across each other. After selection
     * is initialized, if user drags one handle to cross the other handle, this is true, otherwise
     * it's false.
     */
    // If selection happens in single widget, checking [TextRange.start] > [TextRange.end] is
    // enough.
    // But when selection happens across multiple widgets, this value needs more complicated
    // calculation. To avoid repeated calculation, making it as a flag is cheaper.
    val handlesCrossed: Boolean = false
) {
    /**
     * Contains information about an anchor (start/end) of selection.
     */
    @Immutable
    internal data class AnchorInfo(
        /**
         * Text direction of the character in selection edge.
         */
        val direction: ResolvedTextDirection,

        /**
         * Character offset for the selection edge. This offset is within the page
         */
        val offset: Int,
    )

    /**
     * Returns the selection offset information as a [TextRange]
     */
    fun toTextRange(): TextRange {
        return TextRange(start.offset, end.offset)
    }
}