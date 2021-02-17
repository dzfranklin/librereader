package org.danielzfranklin.librereader

import androidx.test.espresso.action.*
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.platform.app.InstrumentationRegistry
import org.danielzfranklin.librereader.ui.reader.PageView
import org.hamcrest.CoreMatchers

val instrumentation
    get() = InstrumentationRegistry.getInstrumentation()!!

fun swipeSlightlyLeft(swiper: Swiper) = GeneralSwipeAction(
    swiper,
    { view ->
        val coords = GeneralLocation.CENTER_RIGHT.calculateCoordinates(view)
        coords[0] -= 100f
        coords
    },
    { view ->
        val coords = GeneralLocation.CENTER_RIGHT.calculateCoordinates(view)
        coords[0] *= 0.8f
        coords
    },
    Press.FINGER
)

fun swipeSlightlyRight(swiper: Swiper) = GeneralSwipeAction(
    swiper,
    { view ->
        val coords = GeneralLocation.CENTER_LEFT.calculateCoordinates(view)
        coords[0] = 100f
        coords
    },
    { view ->
        val coords = GeneralLocation.CENTER_RIGHT.calculateCoordinates(view)
        coords[0] *= 0.2f
        coords
    },
    Press.FINGER
)

fun isPageView() = ViewMatchers.withClassName(CoreMatchers.`is`(PageView::class.qualifiedName))

fun clickPercent(xPercent: Float, yPercent: Float) = GeneralClickAction(
    Tap.SINGLE,
    { view ->
        val (startX, startY) = GeneralLocation.TOP_LEFT.calculateCoordinates(view)
        val (endX, endY) = GeneralLocation.BOTTOM_RIGHT.calculateCoordinates(view)
        val spanX = endX - startX
        val spanY = endY - startY
        floatArrayOf(startX + xPercent * spanX, startY + spanY * yPercent)
    },
    Press.FINGER
)