package com.nshiftui.rainbowparser.printer

import com.nshiftui.rainbowparser.RainbowFormatStyle
import com.nshiftui.rainbowparser.ast.RainbowDocument
import com.nshiftui.rainbowparser.ast.RainbowLeadingTrivia
import com.nshiftui.rainbowparser.ast.RainbowNode
import com.nshiftui.rainbowparser.ast.RainbowParameter
import com.nshiftui.rainbowparser.ast.RainbowTaggedBody
import com.nshiftui.rainbowparser.ast.RainbowUseDeclaration
import com.nshiftui.rainbowparser.ast.RainbowValue
import com.nshiftui.rainbowparser.support.dropLastGrapheme
import com.nshiftui.rainbowparser.support.endsWithNewlineGrapheme
import com.nshiftui.rainbowparser.support.isRainbowPrintableIdentifier
import com.nshiftui.rainbowparser.support.onlyCodePoint
import com.nshiftui.rainbowparser.support.splitGraphemes
import com.nshiftui.rainbowparser.support.splitOnNewlineGrapheme

internal class RainbowPrinter(
    private val style: RainbowFormatStyle = RainbowFormatStyle.DEFAULT,
) {
    fun print(document: RainbowDocument): String {
        val sections = mutableListOf<String>()

        if (document.uses.isNotEmpty()) {
            sections.add(document.uses.joinToString(style.newline) { printUse(it) })
        }

        for (node in document.nodes) {
            sections.add(printNode(node, level = 0))
        }

        return sections.joinToString(style.newline + style.newline)
    }

    private fun printUse(declaration: RainbowUseDeclaration): String {
        val parts = mutableListOf<String>()
        pushLeading(parts, declaration.leading, level = 0)
        parts.add("use ${declaration.name}@${declaration.version}")
        return parts.joinToString(style.newline)
    }

    private fun printNode(node: RainbowNode, level: Int): String {
        val indent = indentation(level)
        val parts = mutableListOf<String>()
        pushLeading(parts, node.leading, level)

        var header = indent + node.name

        if (node.parameters.isNotEmpty()) {
            if (shouldBreakParameters(node)) {
                header += "("
                parts.add(header)
                val inner = indentation(level + 1)
                for ((index, parameter) in node.parameters.withIndex()) {
                    val parameterLines = printParameterLines(parameter, inner).toMutableList()
                    if (index != node.parameters.lastIndex && parameterLines.isNotEmpty()) {
                        val lastIndex = parameterLines.lastIndex
                        parameterLines[lastIndex] = parameterLines[lastIndex] + ","
                    }
                    parts.addAll(parameterLines)
                }
                parts.add("$indent)")
            } else {
                header += "("
                header += node.parameters.joinToString(", ") {
                    printParameterInline(it, indentation(level + 1))
                }
                header += ")"
                parts.add(header)
            }
        } else {
            parts.add(header)
        }

        val block = node.block ?: return parts.joinToString(style.newline)

        if (parts.isNotEmpty()) {
            val lastIndex = parts.lastIndex
            parts[lastIndex] = parts[lastIndex] + " {"
        }

        if (block.children.isEmpty()) {
            parts.add("$indent}")
        } else {
            val children = block.children.joinToString(style.newline + style.newline) {
                printNode(it, level + 1)
            }
            parts.add(children)
            parts.add("$indent}")
        }

        return parts.joinToString(style.newline)
    }

    private fun printParameterLines(parameter: RainbowParameter, indent: String): List<String> {
        val lines = mutableListOf<String>()
        pushLeadingRaw(lines, parameter.leading, indent)
        val value = printValue(parameter.value, indent)
        val valueLines = splitOnNewlineGrapheme(value)
        if (valueLines.size <= 1) {
            lines.add("$indent${parameter.name}: $value")
        } else {
            lines.add("$indent${parameter.name}: ${valueLines[0]}")
            lines.addAll(valueLines.drop(1))
        }
        return lines
    }

    private fun printParameterInline(parameter: RainbowParameter, continuationIndent: String): String =
        "${parameter.name}: ${printValue(parameter.value, continuationIndent)}"

    private fun pushLeading(parts: MutableList<String>, trivia: RainbowLeadingTrivia, level: Int) {
        pushLeadingRaw(parts, trivia, indentation(level))
    }

    private fun pushLeadingRaw(
        parts: MutableList<String>,
        trivia: RainbowLeadingTrivia,
        indent: String,
    ) {
        for (comment in trivia.comments) {
            parts.add("$indent//$comment")
        }
    }

    private fun printValue(value: RainbowValue, continuationIndent: String): String =
        when (value) {
            is RainbowValue.StringValue -> "\"${escape(value.value)}\""
            is RainbowValue.IntValue -> value.value.toString()
            is RainbowValue.DoubleValue -> value.value.toString()
            is RainbowValue.BoolValue -> if (value.value) "true" else "false"
            is RainbowValue.Identifier -> ".${value.value}"
            is RainbowValue.Tagged -> when (val body = value.body) {
                is RainbowTaggedBody.Text ->
                    printTaggedText(value.language.asString, body.value, continuationIndent)
                is RainbowTaggedBody.Placeholder ->
                    "@${value.language.asString}(#{${body.name}})"
            }
            is RainbowValue.ArrayValue ->
                "[" + value.values.joinToString(", ") { printValue(it, continuationIndent) } + "]"
            is RainbowValue.ObjectValue -> {
                val printedEntries = value.entries.joinToString(", ") { entry ->
                    "${printObjectKey(entry.key)}: ${printValue(entry.value, continuationIndent)}"
                }
                "($printedEntries)"
            }
            is RainbowValue.Null -> "null"
        }

    private fun printTaggedText(language: String, text: String, continuationIndent: String): String {
        val trimmed = if (text.endsWithNewlineGrapheme()) text.dropLastGrapheme() else text
        if (!trimmed.contains('\n')) {
            return "@$language($trimmed)"
        }

        val lines = splitOnNewlineGrapheme(trimmed).toMutableList()
        val first = if (lines.isEmpty()) "" else lines.removeAt(0)
        val output = StringBuilder("@$language($first")
        for (line in lines) {
            output.append(style.newline)
            output.append(continuationIndent)
            output.append(line)
        }
        output.append(")")
        return output.toString()
    }

    private fun printObjectKey(key: String): String =
        if (key.isRainbowPrintableIdentifier()) key else "\"${escape(key)}\""

    private fun indentation(level: Int): String = style.indentation.repeat(level)

    private fun escape(value: String): String {
        val output = StringBuilder()
        for (character in splitGraphemes(value)) {
            when (character) {
                "\"" -> output.append("\\\"")
                "\\" -> output.append("\\\\")
                "\n" -> output.append("\\n")
                "\r" -> output.append("\\r")
                "\t" -> output.append("\\t")
                "\u0000" -> output.append("\\0")
                else -> output.append(character)
            }
        }
        return output.toString()
    }
}

private fun shouldBreakParameters(node: RainbowNode): Boolean {
    if (isTriggerNodeName(node.name)) return false
    if (node.parameters.size >= 2) return true
    return node.parameters.any { parameter ->
        val value = parameter.value
        value is RainbowValue.Tagged &&
            value.body is RainbowTaggedBody.Text &&
            value.body.value.contains('\n')
    }
}

private fun isTriggerNodeName(name: String): Boolean {
    val graphemes = splitGraphemes(name)
    if (graphemes.size < 3) return false
    if (graphemes[0] != "O" || graphemes[1] != "n") return false
    val third = graphemes[2].onlyCodePoint() ?: return false
    if (third !in 65..90) return false
    return graphemes.drop(3).all { grapheme ->
        val codePoint = grapheme.onlyCodePoint() ?: return@all false
        (codePoint in 65..90) || (codePoint in 97..122) || (codePoint in 48..57)
    }
}
