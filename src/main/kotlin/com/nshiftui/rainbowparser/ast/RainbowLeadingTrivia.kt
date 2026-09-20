package com.nshiftui.rainbowparser.ast

data class RainbowLeadingTrivia(
    val comments: List<String> = emptyList(),
) {
    val isEmpty: Boolean get() = comments.isEmpty()
}
