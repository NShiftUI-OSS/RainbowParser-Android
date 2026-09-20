package com.nshiftui.rainbowparser

import com.nshiftui.rainbowparser.diagnostics.RainbowParseError
import com.nshiftui.rainbowparser.lexer.RainbowLexer
import com.nshiftui.rainbowparser.lexer.RainbowToken
import io.kotest.assertions.throwables.shouldThrow

internal fun scan(source: String): List<RainbowToken> = RainbowLexer(source).scanTokens()

internal fun expectLexerError(source: String): RainbowParseError =
    shouldThrow<RainbowParseError> { scan(source) }

internal fun expectDecodeError(source: String): RainbowParseError =
    shouldThrow<RainbowParseError> { RainbowDecoder().decode(source) }
