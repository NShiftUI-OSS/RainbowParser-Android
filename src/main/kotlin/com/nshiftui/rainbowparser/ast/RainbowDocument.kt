package com.nshiftui.rainbowparser.ast

data class RainbowDocument(
    val uses: List<RainbowUseDeclaration> = emptyList(),
    val nodes: List<RainbowNode> = emptyList(),
)
