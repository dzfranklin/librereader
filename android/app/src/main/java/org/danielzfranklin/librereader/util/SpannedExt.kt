package org.danielzfranklin.librereader.util

import android.text.Spanned

fun Spanned.toInspectString(): String {
    val spans = getSpans(0, length, Object::class.java)
    val spanText = if (spans.isEmpty()) {
        "No spans"
    } else {
        spans.joinToString("\n") { span ->
            val text = try {
                elideLongText(substring(getSpanStart(span)..getSpanEnd(span)))
            } catch (e: IndexOutOfBoundsException) {
                "Out of bounds: ${e.message}"
            }
            val cls = span.`class`

            val fields = cls.methods
                .filter {
                    it.parameterCount == 0 &&
                            it.name.startsWith("get") &&
                            !it.name.endsWith("Internal") &&
                            arrayOf(
                                "getClass",
                                "getUnderlying",
                                "getSpanTypeId"
                            ).indexOf(it.name) == -1
                }
                .joinToString(", ") {
                    "${it.name.substring(3 until it.name.length)} = ${it.invoke(span)}"
                }

            "* ${cls.simpleName} [${text}] ($fields)"
        }
    }

    return "Spanned inspection:\n${elideLongText(toString())}\n---\n$spanText"
}

private const val maxLength = 200

private fun elideLongText(fullText: String): String {
    var text = fullText.replace("\n", "\\n")
    if (text.length > maxLength) {
        val start = text.substring(0..maxLength / 2)
        val end = text.substring(text.length - maxLength / 2, text.length)
        text = "${start}...${end}"
    }
    return text
}