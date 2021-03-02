package org.danielzfranklin.librereader.ui.screen.reader.paginatedText

import android.os.Parcelable
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.text.selection.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.unit.*
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastFirstOrNull
import kotlinx.coroutines.*
import kotlinx.parcelize.Parcelize
import org.danielzfranklin.librereader.ui.screen.reader.PageRenderer
import org.danielzfranklin.librereader.util.contains
import kotlin.coroutines.CoroutineContext
import kotlin.math.max

internal fun Modifier.selectablePageText(
    page: PageRenderer,
    enabled: State<Boolean>,
    manager: PageTextSelectionManager,
) = if (!enabled.value) this then deselect(manager) else composed {
    // Based on Compose MultiWidgetSelectionDelegate, with many features we don't need stripped out

    val colors = LocalTextSelectionColors.current

    pointerInput(page) {
        forEachGesture {
            val down = awaitPointerEventScope { awaitFirstDown(requireUnconsumed = false) }
            val position = down.position.round()
            val inSelection = manager.selection.value != null

            when {
                inSelection && manager.insideHandle(position, true) -> {
                    awaitHandleDragOrCancel(manager, down, true)
                }
                inSelection && manager.insideHandle(position, false) -> {
                    awaitHandleDragOrCancel(manager, down, false)
                }
                !inSelection -> {
                    awaitWordBasedDragOrCancel(manager, down)
                }
                manager.insideSelection(position) -> {
                    down.consumeAllChanges()
                    awaitChangedToUp(down.id)
                }
                else -> {
                    manager.deselect()
                }
            }
        }
    } then drawWithCache {
        val selection = manager.selection.value
        val highlight = selection?.let { manager.computeHighlightPath(it) }
        val startHandle = selection?.let { manager.computeHandlePath(it, true) }
        val endHandle = selection?.let { manager.computeHandlePath(it, false) }

        onDrawBehind {
            // TODO: Remove this line. Used to fix bug <https://issuetracker.google.com/issues/181589173>
            manager.selection.value
            if (selection != null) {
                drawPath(highlight!!, colors.backgroundColor)
                drawPath(startHandle!!, colors.handleColor)
                drawPath(endHandle!!, colors.handleColor)
            }
        }
    }
}

private fun Modifier.deselect(manager: PageTextSelectionManager): Modifier {
    manager.deselect()
    return this
}

private suspend fun PointerInputScope.awaitHandleDragOrCancel(
    manager: PageTextSelectionManager,
    down: PointerInputChange,
    draggingStartHandle: Boolean
) {
    down.consumeAllChanges()

    manager.hideSelectionToolbar()

    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent()
            if (event.isPointerUp(down.id)) break
            val drag = event.changes.fastFirstOrNull { it.id == down.id } ?: break

            manager.selection.value ?: break
            if (draggingStartHandle) {
                manager.update(
                    startPosition = drag.position,
                    isStartHandleDragged = true
                )
            } else {
                manager.update(
                    endPosition = drag.position,
                )
            }

            drag.consumeAllChanges()
        }
    }

    manager.showSelectionToolbar()
}

private suspend fun PointerInputScope.awaitWordBasedDragOrCancel(
    manager: PageTextSelectionManager,
    down: PointerInputChange
) {
    down.consumeAllChanges()

    if (awaitLongPressOrCancellation(down) == null) {
        manager.showSelectionToolbar()
        return
    }

    manager.hideSelectionToolbar()
    manager.update(
        startPosition = down.position,
        endPosition = down.position,
        wordBased = true,
    )

    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent()
            if (event.isPointerUp(down.id)) break
            val drag = event.changes.fastFirstOrNull { it.id == down.id } ?: break

            manager.update(
                endPosition = drag.position,
                wordBased = true,
            )
            drag.consumeAllChanges()
        }
    }

    manager.showSelectionToolbar()
}

private suspend fun PointerInputScope.awaitChangedToUp(id: PointerId) {
    awaitPointerEventScope {
        do {
            val event = awaitPointerEvent().changes.fastFirstOrNull { it.id == id }
            event?.consumeAllChanges()
        } while (event != null && !event.changedToUpIgnoreConsumed())
    }
}


internal class PageTextSelectionManager(
    override val coroutineContext: CoroutineContext,
    private val page: PageRenderer,
    private val textToolbar: TextToolbar,
    private val clipboardManager: ClipboardManager,
    private val density: Density,
    initialSelection: Selection? = null
) : CoroutineScope {
    // Loosely based off of SelectionManager (See usage in SelectionContainer)

    private val _selection = mutableStateOf(initialSelection)
    val selection: State<Selection?> = _selection

    internal fun insideSelection(offset: IntOffset): Boolean {
        val selection = _selection.value ?: return false
        return computeHighlightPath(selection).contains(offset)
    }

    internal fun insideHandle(offset: IntOffset, isStartHandle: Boolean): Boolean {
        val selection = _selection.value ?: return false
        return computeHandlePath(selection, isStartHandle).contains(offset)
    }

    internal fun handlePosition(selection: Selection, isStartHandle: Boolean): Offset {
        val absolute = if (!selection.handlesCrossed) isStartHandle else !isStartHandle
        return if (absolute) {
            page.getBoundingBox(selection.start.offset).bottomLeft
        } else {
            page.getBoundingBox(selection.end.offset).bottomLeft
        }
    }

    // TODO cache result
    internal fun computeHighlightPath(selection: Selection): Path {
        val range = selection.toTextRange()

        if (range.min == range.max) return Path()

        return page.getPathForRange(range.min, range.max)
    }

    // TODO: Cache results
    internal fun computeHandlePath(selection: Selection, isStartHandle: Boolean): Path {
        val directions = selection.start.direction to selection.end.direction
        val isLeft = isLeft(isStartHandle, directions, selection.handlesCrossed)

        val position = handlePosition(selection, isStartHandle)

        // Path dimensions copied from Compose

        return Path().apply {
            with(density) {
                addRect(
                    Rect(
                        top = 0f,
                        bottom = 0.5f * HANDLE_HEIGHT.toPx(),
                        left = if (isLeft) 0.5f * HANDLE_WIDTH.toPx() else 0f,
                        right = if (isLeft) HANDLE_WIDTH.toPx() else 0.5f * HANDLE_WIDTH.toPx()
                    )
                )

                addOval(
                    Rect(
                        top = 0f,
                        bottom = HANDLE_HEIGHT.toPx(),
                        left = 0f,
                        right = HANDLE_WIDTH.toPx()
                    )
                )

                translate(position)
                if (isLeft) {
                    translate(Offset(-HANDLE_WIDTH.toPx(), 0f))
                }
            }
        }
    }

    private fun getSelectionInfo(
        startPosition: Offset,
        endPosition: Offset,
        previous: Selection?,
        wordBased: Boolean = false,
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

    fun deselect() {
        _selection.value = null
        hideSelectionToolbar()
    }

    var lastStartPosition: Offset? = null
    var lastEndPosition: Offset? = null

    internal fun update(
        startPosition: Offset? = null,
        endPosition: Offset? = null,
        wordBased: Boolean = false,
        isStartHandleDragged: Boolean = false
    ) {
        val actualStart = startPosition ?: lastStartPosition!!
        val actualEnd = endPosition ?: lastEndPosition!!

        _selection.value = getSelectionInfo(
            startPosition = actualStart,
            endPosition = actualEnd,
            previous = _selection.value,
            wordBased = wordBased,
            isStartHandleDragged = isStartHandleDragged
        )

        lastStartPosition = actualStart
        lastEndPosition = actualEnd
    }

    internal fun showSelectionToolbar() {
        val selection = _selection.value ?: return
        textToolbar.showMenu(
            selectionRect(selection),
            onCopyRequested = {
                copySelectionToClipboard()
                hideSelectionToolbar()
            }
        )
    }

    internal fun hideSelectionToolbar() {
        if (textToolbar.status == TextToolbarStatus.Shown) {
            textToolbar.hide()
        }
    }

    private fun copySelectionToClipboard() {
        val selection = _selection.value ?: return
        val first = if (!selection.handlesCrossed) selection.start else selection.end
        val end = if (!selection.handlesCrossed) selection.end else selection.start
        val text = page.getText(first.offset, end.offset + 1)
        clipboardManager.setText(text)

        deselect()
    }

    /**
     * Copied from compose
     *
     * Calculate selected region as Rect. The top is the top of the first selected line, and the
     * bottom is the bottom of the last selected line. The left is the leftmost handle's horizontal
     * coordinates, and the right is the rightmost handle's coordinates. */
    private fun selectionRect(selection: Selection): Rect {
        val start = page.getBoundingBox(selection.start.offset)
        val end = page.getBoundingBox(selection.end.offset)

        val leftmost = if (start.left < end.left) start else end
        val rightmost = if (start.right > end.right) start else end

        val top = if (!selection.handlesCrossed) start.top else end.top
        val bottom = if (!selection.handlesCrossed) end.bottom else start.bottom

        return Rect(
            top = top,
            bottom = bottom,
            left = leftmost.left,
            right = rightmost.right
        )
    }

    /**
     * Copied from Compose
     *
     * Computes whether the handle's appearance should be left-pointing or right-pointing.
     */
    private fun isLeft(
        isStartHandle: Boolean,
        directions: Pair<ResolvedTextDirection, ResolvedTextDirection>,
        handlesCrossed: Boolean
    ): Boolean {
        return if (isStartHandle) {
            isHandleLtrDirection(directions.first, handlesCrossed)
        } else {
            !isHandleLtrDirection(directions.second, handlesCrossed)
        }
    }

    /**
     * Copied from compose
     *
     * This method is to check if the selection handles should use the natural Ltr pointing
     * direction.
     * If the context is Ltr and the handles are not crossed, or if the context is Rtl and the handles
     * are crossed, return true.
     *
     * In Ltr context, the start handle should point to the left, and the end handle should point to
     * the right. However, in Rtl context or when handles are crossed, the start handle should point to
     * the right, and the end handle should point to left.
     */
    private fun isHandleLtrDirection(
        direction: ResolvedTextDirection,
        areHandlesCrossed: Boolean
    ): Boolean {
        return direction == ResolvedTextDirection.Ltr && !areHandlesCrossed ||
                direction == ResolvedTextDirection.Rtl && areHandlesCrossed
    }

    @Parcelize
    private data class SaverData(val selection: Selection?) : Parcelable

    companion object {
        fun saver(
            coroutineContext: CoroutineContext,
            page: PageRenderer,
            textToolbar: TextToolbar,
            clipboardManager: ClipboardManager,
            density: Density,
        ): Saver<PageTextSelectionManager, *> = Saver(
            save = { SaverData(it.selection.value) },
            restore = {
                PageTextSelectionManager(
                    coroutineContext,
                    page,
                    textToolbar,
                    clipboardManager,
                    density,
                    it.selection
                )
            }
        )

        private val HANDLE_WIDTH = 25.dp
        private val HANDLE_HEIGHT = 25.dp
    }
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
@Parcelize
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
) : Parcelable {
    /**
     * Contains information about an anchor (start/end) of selection.
     */
    @Immutable
    @Parcelize
    internal data class AnchorInfo(
        /**
         * Text direction of the character in selection edge.
         */
        val direction: ResolvedTextDirection,

        /**
         * Character offset for the selection edge. This offset is within the page
         */
        val offset: Int,
    ) : Parcelable

    /**
     * Returns the selection offset information as a [TextRange]
     */
    fun toTextRange(): TextRange {
        return TextRange(start.offset, end.offset)
    }
}

// Copied from Compose
private suspend fun PointerInputScope.awaitLongPressOrCancellation(
    initialDown: PointerInputChange
): PointerInputChange? {
    var longPress: PointerInputChange? = null
    var currentDown = initialDown
    val longPressTimeout = viewConfiguration.longPressTimeoutMillis
    return try {
        // wait for first tap up or long press
        withTimeout(longPressTimeout) {
            awaitPointerEventScope {
                var finished = false
                while (!finished) {
                    val event = awaitPointerEvent(PointerEventPass.Main)
                    if (event.changes.fastAll { it.changedToUpIgnoreConsumed() }) {
                        // All pointers are up
                        finished = true
                    }

                    if (
                        event.changes.fastAny { it.consumed.downChange || it.isOutOfBounds(size) }
                    ) {
                        finished = true // Canceled
                    }

                    // Check for cancel by position consumption. We can look on the Final pass of
                    // the existing pointer event because it comes after the Main pass we checked
                    // above.
                    val consumeCheck = awaitPointerEvent(PointerEventPass.Final)
                    if (consumeCheck.changes.fastAny { it.positionChangeConsumed() }) {
                        finished = true
                    }
                    if (!event.isPointerUp(currentDown.id)) {
                        longPress = event.changes.firstOrNull { it.id == currentDown.id }
                    } else {
                        val newPressed = event.changes.fastFirstOrNull { it.pressed }
                        if (newPressed != null) {
                            currentDown = newPressed
                            longPress = currentDown
                        } else {
                            // should technically never happen as we checked it above
                            finished = true
                        }
                    }
                }
            }
        }
        null
    } catch (_: TimeoutCancellationException) {
        longPress ?: initialDown
    }
}

// Copied from Compose
private fun PointerEvent.isPointerUp(pointerId: PointerId): Boolean =
    changes.firstOrNull { it.id == pointerId }?.pressed != true
