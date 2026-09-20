package com.nshiftui.rainbowparser.diagnostics

class RainbowParseError(
    val diagnostics: List<RainbowDiagnostic>,
) : Exception(diagnostics.joinToString("\n") { it.description }) {
    constructor(diagnostic: RainbowDiagnostic) : this(listOf(diagnostic))

    val description: String
        get() = message ?: ""
}
