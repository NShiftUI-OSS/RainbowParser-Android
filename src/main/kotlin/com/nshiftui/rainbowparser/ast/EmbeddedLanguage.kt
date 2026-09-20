package com.nshiftui.rainbowparser.ast

enum class EmbeddedLanguage(val rawValue: String) {
    JSON("JSON"),
    YAML("YAML"),
    XML("XML"),
    HTML("HTML"),
    MARKDOWN("MARKDOWN");

    val asString: String get() = rawValue

    companion object {
        fun parse(name: String): EmbeddedLanguage? =
            entries.firstOrNull { it.rawValue == name }
    }
}

sealed class RainbowTaggedBody {
    data class Text(val value: String) : RainbowTaggedBody()
    data class Placeholder(val name: String) : RainbowTaggedBody()

    val hasPlaceholder: Boolean
        get() = this is Placeholder
}
