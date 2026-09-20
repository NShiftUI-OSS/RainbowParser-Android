package com.nshiftui.rainbowparser

data class RainbowFormatStyle(
    val indentation: String = "  ",
    val newline: String = "\n",
) {
    companion object {
        @JvmField
        val DEFAULT = RainbowFormatStyle()
    }
}
