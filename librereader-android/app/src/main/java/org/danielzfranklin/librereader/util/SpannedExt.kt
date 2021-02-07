package org.danielzfranklin.librereader.util

import android.text.Spanned
import kotlin.math.ceil
import kotlin.math.floor

fun Spanned.toInspectString(): String {
    val spans = getSpans(0, length, Object::class.java)
        .joinToString("\n") { span ->
            val text = try {
                elideLongText(substring(getSpanStart(span)..getSpanEnd(span)))
            } catch (e: IndexOutOfBoundsException) {
                "{{Out of bounds: ${e.message}}}"
            }
            val cls = span.`class`
            val fields = cls.declaredFields
                .joinToString(", ") {
                    it.isAccessible = true
                    "${it.name} = ${it.get(span)}"
                }
            "${cls.simpleName}($fields)  [${text}]"
        }

    return "Spanned inspection: ${elideLongText(toString())}\n---\n$spans"
}

private fun elideLongText(text: String): String {
    val overage = text.length - 100
    if (overage > 0) {
        val start = text.substring(0..floor(overage.toFloat() / 2f).toInt())
        val end =
            text.substring(text.length - ceil(overage.toFloat() / 2f).toInt(), text.length)
        return "${start}...${end}"
    }
    return text
}