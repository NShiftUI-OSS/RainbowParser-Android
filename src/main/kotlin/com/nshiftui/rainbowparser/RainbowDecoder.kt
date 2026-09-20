package com.nshiftui.rainbowparser

import com.nshiftui.rainbowparser.ast.RainbowDocument
import com.nshiftui.rainbowparser.lexer.RainbowLexer
import com.nshiftui.rainbowparser.parser.RainbowSyntaxParser

class RainbowDecoder {
    fun decode(source: String): RainbowDocument {
        val tokens = RainbowLexer(source).scanTokens()
        return RainbowSyntaxParser(tokens).parse()
    }
}
