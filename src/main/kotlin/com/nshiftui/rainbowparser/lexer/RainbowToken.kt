package com.nshiftui.rainbowparser.lexer

import com.nshiftui.rainbowparser.diagnostics.RainbowSourceRange

internal data class RainbowToken(
    val kind: RainbowTokenKind,
    val range: RainbowSourceRange,
)
