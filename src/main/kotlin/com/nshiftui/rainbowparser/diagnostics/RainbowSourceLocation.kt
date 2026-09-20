package com.nshiftui.rainbowparser.diagnostics

data class RainbowSourceLocation(
    val offset: Int,
    val line: Int,
    val column: Int,
) : Comparable<RainbowSourceLocation> {
    override fun compareTo(other: RainbowSourceLocation): Int = offset.compareTo(other.offset)
}
