package com.nshiftui.rainbowparser.ast

data class RainbowUseDeclaration(
    val name: String,
    val version: String,
    val leading: RainbowLeadingTrivia = RainbowLeadingTrivia(),
)
