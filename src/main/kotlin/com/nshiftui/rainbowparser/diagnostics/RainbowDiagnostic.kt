package com.nshiftui.rainbowparser.diagnostics

data class RainbowDiagnostic(
    val code: String,
    val message: String,
    val severity: RainbowDiagnosticSeverity = RainbowDiagnosticSeverity.ERROR,
    val range: RainbowSourceRange,
) {
    val description: String
        get() = "${severity.rawValue.uppercase()} $code at ${range.start.line}:${range.start.column}: $message"

    override fun toString(): String = description
}
