package com.nshiftui.rainbowparser.support

context(text: String)
internal fun currentGraphemes(): List<String> {
    if (text.isEmpty()) return emptyList()
    val graphemes = ArrayList<String>()
    var index = 0
    while (index < text.length) {
        val start = index
        val codePoint = text.codePointAt(index)
        index += Character.charCount(codePoint)
        if (codePoint == CR && index < text.length && text.codePointAt(index) == LF) {
            index += 1
            graphemes.add(text.substring(start, index))
            continue
        }
        if (isRegionalIndicator(codePoint) && index < text.length) {
            val next = text.codePointAt(index)
            if (isRegionalIndicator(next)) {
                index += Character.charCount(next)
            }
            graphemes.add(text.substring(start, index))
            continue
        }
        if (isStandaloneGrapheme(codePoint)) {
            graphemes.add(text.substring(start, index))
            continue
        }
        while (index < text.length) {
            val next = text.codePointAt(index)
            if (isStandaloneGrapheme(next)) break
            if (!isGraphemeExtend(next)) break
            index += Character.charCount(next)
            if (next == ZWJ && index < text.length) {
                val afterJoin = text.codePointAt(index)
                if (!isStandaloneGrapheme(afterJoin)) {
                    index += Character.charCount(afterJoin)
                }
            }
        }
        graphemes.add(text.substring(start, index))
    }
    return graphemes
}

internal fun splitGraphemes(text: String): List<String> = context(text) { currentGraphemes() }

internal fun String.onlyCodePoint(): Int? {
    if (isEmpty()) return null
    val codePoint = codePointAt(0)
    return if (Character.charCount(codePoint) == length) codePoint else null
}

internal fun String.isRainbowWhitespace(): Boolean =
    this == " " || this == "\n" || this == "\r" || this == "\r\n" || this == "\t"

internal fun String.isRainbowLineBreak(): Boolean =
    this == "\n" || this == "\r" || this == "\r\n"

internal fun String.isRainbowDigit(): Boolean {
    val codePoint = onlyCodePoint() ?: return false
    return codePoint in 48..57
}

internal fun String.isRainbowIdentifierStart(): Boolean {
    val codePoint = onlyCodePoint() ?: return false
    return codePoint == 95 || codePoint in 65..90 || codePoint in 97..122
}

internal fun String.isRainbowIdentifierContinuation(): Boolean =
    isRainbowIdentifierStart() || isRainbowDigit()

internal fun String.isRainbowPrintableIdentifier(): Boolean {
    val graphemes = splitGraphemes(this)
    val first = graphemes.firstOrNull() ?: return false
    if (!first.isRainbowPrintableIdentifierStart()) return false
    return graphemes.drop(1).all { it.isRainbowPrintableIdentifierContinuation() }
}

internal fun String.isRainbowPrintableIdentifierStart(): Boolean {
    val codePoint = onlyCodePoint() ?: return false
    return codePoint == 95 || codePoint in 65..90 || codePoint in 97..122
}

internal fun String.isRainbowPrintableIdentifierContinuation(): Boolean =
    isRainbowPrintableIdentifierStart() || isRainbowDigit()

internal fun String.endsWithNewlineGrapheme(): Boolean =
    splitGraphemes(this).lastOrNull() == "\n"

internal fun String.dropLastGrapheme(): String =
    splitGraphemes(this).dropLast(1).joinToString("")

internal fun splitOnNewlineGrapheme(text: String): List<String> {
    val graphemes = splitGraphemes(text)
    val lines = ArrayList<String>()
    val current = StringBuilder()
    for (grapheme in graphemes) {
        if (grapheme == "\n") {
            lines.add(current.toString())
            current.setLength(0)
        } else {
            current.append(grapheme)
        }
    }
    lines.add(current.toString())
    return lines
}

internal fun isBlankIndentLine(line: String): Boolean =
    splitGraphemes(line).all { it == " " || it == "\t" }

internal fun leadingIndentCount(line: String): Int {
    var count = 0
    for (grapheme in splitGraphemes(line)) {
        if (grapheme == " " || grapheme == "\t") {
            count += 1
        } else {
            break
        }
    }
    return count
}

internal fun dropFirstGraphemes(text: String, count: Int): String =
    splitGraphemes(text).drop(count).joinToString("")

private const val CR = 0x0D
private const val LF = 0x0A
private const val ZWJ = 0x200D

private fun isRegionalIndicator(codePoint: Int): Boolean =
    codePoint in 0x1F1E6..0x1F1FF

private fun isStandaloneGrapheme(codePoint: Int): Boolean {
    if (codePoint == CR || codePoint == LF || codePoint == '\t'.code) return true
    val type = Character.getType(codePoint)
    return type == Character.CONTROL.toInt() ||
        type == Character.LINE_SEPARATOR.toInt() ||
        type == Character.PARAGRAPH_SEPARATOR.toInt()
}

private fun isGraphemeExtend(codePoint: Int): Boolean {
    if (codePoint == ZWJ || codePoint in 0xFE00..0xFE0F || codePoint in 0xE0100..0xE01EF) {
        return true
    }
    val type = Character.getType(codePoint)
    return type == Character.NON_SPACING_MARK.toInt() ||
        type == Character.ENCLOSING_MARK.toInt() ||
        type == Character.COMBINING_SPACING_MARK.toInt()
}
