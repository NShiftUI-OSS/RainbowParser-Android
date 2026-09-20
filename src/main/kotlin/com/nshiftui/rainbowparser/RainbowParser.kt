package com.nshiftui.rainbowparser

import com.nshiftui.rainbowparser.ast.RainbowDocument

class RainbowParser {
    fun decode(source: String): RainbowDocument = RainbowDecoder().decode(source)

    fun encode(
        document: RainbowDocument,
        style: RainbowFormatStyle = RainbowFormatStyle.DEFAULT,
    ): String = RainbowEncoder(style).encode(document)
}
