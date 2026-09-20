package com.nshiftui.rainbowparser.ast

import com.nshiftui.rainbowparser.diagnostics.RainbowSourceRange

sealed class RainbowValue {
    data class StringValue(val value: String) : RainbowValue()
    data class IntValue(val value: Long) : RainbowValue()
    data class DoubleValue(val value: Double) : RainbowValue()
    data class BoolValue(val value: Boolean) : RainbowValue()

    /** Enum value. Source requires a leading `.` (e.g. `.primary`); the payload omits it. */
    data class Identifier(val value: String) : RainbowValue()

    data class ArrayValue(val values: List<RainbowValue>) : RainbowValue()
    data class ObjectValue(val entries: List<RainbowObjectEntry>) : RainbowValue()
    data object Null : RainbowValue()

    /** `@LANG(...)` parameter payload (raw language text or whole-blob placeholder). */
    data class Tagged(
        val language: EmbeddedLanguage,
        val body: RainbowTaggedBody,
        val bodyRange: RainbowSourceRange,
    ) : RainbowValue()
}
