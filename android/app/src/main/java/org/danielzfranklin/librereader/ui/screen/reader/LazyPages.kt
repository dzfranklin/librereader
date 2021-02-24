package org.danielzfranklin.librereader.ui.screen.reader

import androidx.compose.animation.asDisposableClock
import androidx.compose.animation.core.AnimationClockObservable
import androidx.compose.foundation.animation.FlingConfig
import androidx.compose.foundation.animation.defaultFlingConfig
import androidx.compose.foundation.gestures.ScrollableController
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.preferredHeight
import androidx.compose.foundation.layout.preferredSize
import androidx.compose.foundation.layout.preferredWidth
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.gesture.scrollorientationlocking.Orientation
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalAnimationClock
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.util.fastMap

// NOTE: Inspired by Compose LazyList

@Composable
fun LazyPages(
    modifier: Modifier = Modifier,
    state: LazyPagesState = rememberLazyPagesState(PageIndex(0, 0)),
    content: LazyPagesScope.() -> Unit
) {
    SubcomposeLayout(
        modifier
            .clipToBounds()
            .scrollable(Orientation.Horizontal, controller = state.scrollableController)
    ) { constraints ->
        val scope = LazyPagesScopeImpl()
        scope.apply(content)

        val pageWidthPx = constraints.minWidth
        val pageHeightPx = constraints.minHeight
        val pageWidth = pageWidthPx.toDp()
        val pageHeight = pageHeightPx.toDp()

        val childConstraints = Constraints(
            minWidth = pageWidthPx,
            maxWidth = pageWidthPx,
            minHeight = 0,
            maxHeight = Constraints.Infinity,
        )

        val sectionI = state.currentPage.value.section
        val sectionComposable = scope.sections[sectionI]
        val sectionScope = LazyPagesScopeImpl.SectionScopeImpl(pageWidth, pageHeight)

        val placeables = subcompose(sectionI) { with(sectionScope) { sectionComposable() } }
            .fastMap { it.measure(childConstraints) }

        // TODO: This won't work, because we don't get to place at the line level
        // maybe we need to compute lines?

        1
    }
}

@Composable
fun rememberLazyPagesState(initialIndex: PageIndex): LazyPagesState {
    val clock = LocalAnimationClock.current.asDisposableClock()
    val config = defaultFlingConfig()

    val saver = remember(clock, config) {
        LazyPagesState.saver(clock, config)
    }

    return rememberSaveable(clock, config, saver = saver) {
        LazyPagesState(initialIndex, clock, config)
    }
}

class LazyPagesState(
    initialIndex: PageIndex,
    private val animationClock: AnimationClockObservable,
    private val flingConfig: FlingConfig,
) {
    private var _currentPage = mutableStateOf(initialIndex)
    val currentPage: State<PageIndex> = _currentPage

    internal val scrollableController = ScrollableController(
        { TODO() },
        animationClock = animationClock,
        flingConfig = flingConfig
    )

    companion object {
        fun saver(
            animationClock: AnimationClockObservable,
            flingConfig: FlingConfig
        ): Saver<LazyPagesState, *> = listSaver(
            save = { listOf(it.currentPage.value) },
            restore = {
                LazyPagesState(
                    it[0],
                    animationClock = animationClock,
                    flingConfig = flingConfig
                )
            }
        )
    }
}

class PageIndex(val section: Int, val page: Int)


interface LazyPagesScope {
    fun section(content: @Composable SectionScope.() -> Unit)

    interface SectionScope {
        fun Modifier.fillPageMaxSize(fraction: Float): Modifier
        fun Modifier.fillPageMaxWidth(fraction: Float): Modifier
        fun Modifier.fillPageMaxHeight(fraction: Float): Modifier
    }
}

private class LazyPagesScopeImpl : LazyPagesScope {
    val sections = mutableListOf<@Composable LazyPagesScope.SectionScope.() -> Unit>()

    override fun section(content: @Composable LazyPagesScope.SectionScope.() -> Unit) {
        sections.add(content)
    }

    internal data class SectionScopeImpl(
        val maxWidth: Dp,
        val maxHeight: Dp
    ) : LazyPagesScope.SectionScope {
        override fun Modifier.fillPageMaxSize(fraction: Float) = preferredSize(
            maxWidth * fraction,
            maxHeight * fraction
        )

        override fun Modifier.fillPageMaxWidth(fraction: Float) =
            preferredWidth(maxWidth * fraction)

        override fun Modifier.fillPageMaxHeight(fraction: Float) =
            preferredHeight(maxHeight * fraction)
    }
}