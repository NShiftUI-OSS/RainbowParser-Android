package com.nshiftui.rainbowparser.lexer

import com.nshiftui.rainbowparser.ast.EmbeddedLanguage
import com.nshiftui.rainbowparser.ast.RainbowTaggedBody
import com.nshiftui.rainbowparser.diagnostics.RainbowSourceRange

internal sealed class RainbowTokenKind {
    data class Identifier(val value: String) : RainbowTokenKind()

    /** Enum value with required leading `.` in source (e.g. `.primary`). Payload omits the dot. */
    data class DotIdentifier(val value: String) : RainbowTokenKind()

    data class StringToken(val value: String) : RainbowTokenKind()
    data class IntToken(val value: Long) : RainbowTokenKind()
    data class DoubleToken(val value: Double) : RainbowTokenKind()
    data class Version(val value: String) : RainbowTokenKind()
    data class BoolToken(val value: Boolean) : RainbowTokenKind()
    data object Null : RainbowTokenKind()
    data object LeftParen : RainbowTokenKind()
    data object RightParen : RainbowTokenKind()
    data object LeftBrace : RainbowTokenKind()
    data object RightBrace : RainbowTokenKind()
    data object LeftBracket : RainbowTokenKind()
    data object RightBracket : RainbowTokenKind()
    data object Colon : RainbowTokenKind()
    data object Comma : RainbowTokenKind()
    data object At : RainbowTokenKind()
    data class Tagged(
        val language: EmbeddedLanguage,
        val body: RainbowTaggedBody,
        val bodyRange: RainbowSourceRange,
    ) : RainbowTokenKind()

    /** Line comment `//…` — payload is the text after `//`. */
    data class Comment(val value: String) : RainbowTokenKind()

    data object Eof : RainbowTokenKind()
}
