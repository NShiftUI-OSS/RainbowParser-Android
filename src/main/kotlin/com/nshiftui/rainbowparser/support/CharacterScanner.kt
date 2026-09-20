package com.nshiftui.rainbowparser.support

import com.nshiftui.rainbowparser.diagnostics.RainbowSourceLocation

internal class CharacterScanner(private val source: SourceText) {
    data class Checkpoint(
        val index: Int,
        val location: RainbowSourceLocation,
    )

    private val graphemes: List<String> = context(source.contents) { currentGraphemes() }
    private var index: Int = 0
    var location: RainbowSourceLocation = RainbowSourceLocation(offset = 0, line = 1, column = 1)
        private set

    val isAtEnd: Boolean
        get() = index >= graphemes.size

    fun checkpoint(): Checkpoint = Checkpoint(index = index, location = location)

    fun restore(checkpoint: Checkpoint) {
        index = checkpoint.index
        location = checkpoint.location
    }

    fun peek(): String? = graphemes.getOrNull(index)

    fun peekNext(): String? = graphemes.getOrNull(index + 1)

    fun peekOffset(offset: Int): String? = graphemes.getOrNull(index + offset)

    fun startsWith(text: String): Boolean {
        val expected = context(text) { currentGraphemes() }
        if (index + expected.size > graphemes.size) return false
        for (offset in expected.indices) {
            if (graphemes[index + offset] != expected[offset]) return false
        }
        return true
    }

    fun advance(): String? {
        if (isAtEnd) return null
        val character = graphemes[index]
        index += 1
        location = if (character.isRainbowLineBreak()) {
            RainbowSourceLocation(
                offset = location.offset + 1,
                line = location.line + 1,
                column = 1,
            )
        } else {
            RainbowSourceLocation(
                offset = location.offset + 1,
                line = location.line,
                column = location.column + 1,
            )
        }
        return character
    }
}
