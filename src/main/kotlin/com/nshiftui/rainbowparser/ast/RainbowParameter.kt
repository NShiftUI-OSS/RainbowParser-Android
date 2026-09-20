package com.nshiftui.rainbowparser.ast

data class RainbowParameter(
    val name: String,
    val value: RainbowValue,
    val leading: RainbowLeadingTrivia = RainbowLeadingTrivia(),
)
