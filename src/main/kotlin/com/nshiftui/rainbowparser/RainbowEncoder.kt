package com.nshiftui.rainbowparser

import com.nshiftui.rainbowparser.ast.RainbowDocument
import com.nshiftui.rainbowparser.printer.RainbowPrinter

class RainbowEncoder(
    private val style: RainbowFormatStyle = RainbowFormatStyle.DEFAULT,
) {
    fun encode(document: RainbowDocument): String =
        RainbowPrinter(style).print(document)
}
