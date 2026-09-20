package com.nshiftui.rainbowparser.parser

import com.nshiftui.rainbowparser.ast.RainbowBlock
import com.nshiftui.rainbowparser.ast.RainbowDocument
import com.nshiftui.rainbowparser.ast.RainbowLeadingTrivia
import com.nshiftui.rainbowparser.ast.RainbowNode
import com.nshiftui.rainbowparser.ast.RainbowObjectEntry
import com.nshiftui.rainbowparser.ast.RainbowParameter
import com.nshiftui.rainbowparser.ast.RainbowUseDeclaration
import com.nshiftui.rainbowparser.ast.RainbowValue
import com.nshiftui.rainbowparser.diagnostics.RainbowDiagnostic
import com.nshiftui.rainbowparser.diagnostics.RainbowParseError
import com.nshiftui.rainbowparser.lexer.RainbowToken
import com.nshiftui.rainbowparser.lexer.RainbowTokenKind

internal class RainbowSyntaxParser(private val tokens: List<RainbowToken>) {
    private var current = 0
    private val diagnostics = mutableListOf<RainbowDiagnostic>()

    fun parse(): RainbowDocument {
        val uses = mutableListOf<RainbowUseDeclaration>()
        val nodes = mutableListOf<RainbowNode>()

        while (!isAtEnd) {
            val leading = takeLeadingTrivia()
            when (val kind = peek.kind) {
                is RainbowTokenKind.Identifier if kind.value == "use" -> {
                    val parsed = parseUseDeclaration()
                    if (parsed != null) {
                        uses.add(
                            RainbowUseDeclaration(
                                name = parsed.name,
                                version = parsed.version,
                                leading = leading,
                            ),
                        )
                    } else {
                        synchronizeNode()
                    }
                }
                else -> {
                    val node = parseNode()
                    if (node != null) {
                        nodes.add(
                            RainbowNode(
                                name = node.name,
                                parameters = node.parameters,
                                block = node.block,
                                leading = leading,
                            ),
                        )
                    } else {
                        synchronizeNode()
                    }
                }
            }
        }

        if (diagnostics.isEmpty()) {
            return RainbowDocument(uses = uses, nodes = nodes)
        }
        throw RainbowParseError(diagnostics)
    }

    private fun takeLeadingTrivia(): RainbowLeadingTrivia {
        val comments = mutableListOf<String>()
        while (true) {
            when (val kind = peek.kind) {
                is RainbowTokenKind.Comment -> {
                    comments.add(kind.value)
                    advance()
                }
                else -> break
            }
        }
        return RainbowLeadingTrivia(comments)
    }

    private fun parseUseDeclaration(): RainbowUseDeclaration? {
        val useKind = peek.kind
        if (useKind !is RainbowTokenKind.Identifier || useKind.value != "use") {
            return null
        }
        advance()

        val nameKind = peek.kind
        if (nameKind !is RainbowTokenKind.Identifier) {
            appendUnexpectedToken("Expected plugin or event name after 'use'.")
            return null
        }
        advance()

        if (!match(RainbowTokenKind.At)) {
            appendUnexpectedToken("Expected '@' before version in use declaration.")
            return null
        }

        val versionKind = peek.kind
        if (versionKind !is RainbowTokenKind.Version) {
            appendUnexpectedToken("Expected SemVer MAJOR.MINOR.PATCH after '@'.")
            return null
        }
        advance()

        return RainbowUseDeclaration(name = nameKind.value, version = versionKind.value)
    }

    private fun parseNode(): RainbowNode? {
        val nameKind = peek.kind
        if (nameKind !is RainbowTokenKind.Identifier) {
            appendUnexpectedToken("Expected node name.")
            advance()
            return null
        }
        advance()

        val parameters = if (match(RainbowTokenKind.LeftParen)) {
            parseArguments()
        } else {
            emptyList()
        }

        val block = if (match(RainbowTokenKind.LeftBrace)) {
            parseBlock()
        } else {
            null
        }

        return RainbowNode(name = nameKind.value, parameters = parameters, block = block)
    }

    private fun parseArguments(): List<RainbowParameter> {
        val parameters = mutableListOf<RainbowParameter>()

        if (significantIsRightParen) {
            takeLeadingTrivia()
            advance()
            return parameters
        }

        while (true) {
            if (significantIsRightParen || isAtEnd) break

            val parameter = parseParameter()
            if (parameter != null) {
                parameters.add(parameter)
            } else {
                synchronizeParameter()
            }

            if (!match(RainbowTokenKind.Comma)) break

            if (significantIsRightParen) {
                diagnostics.add(
                    RainbowDiagnostic(
                        code = "rainbow.parser.unexpectedToken",
                        message = "Trailing comma is not allowed after the last parameter.",
                        range = previous.range,
                    ),
                )
                break
            }
        }

        takeLeadingTrivia()
        consume(RainbowTokenKind.RightParen, "Expected ')' after parameter list.")
        return parameters
    }

    private fun parseParameter(): RainbowParameter? {
        val leading = takeLeadingTrivia()
        val nameKind = peek.kind
        if (nameKind !is RainbowTokenKind.Identifier) {
            appendUnexpectedToken("Expected parameter name.")
            return null
        }

        advance()
        consume(RainbowTokenKind.Colon, "Expected ':' after parameter name.")

        val value = parseValue()
        if (value == null) {
            synchronizeParameter()
            return null
        }

        return RainbowParameter(name = nameKind.value, value = value, leading = leading)
    }

    private fun parseBlock(): RainbowBlock {
        val children = mutableListOf<RainbowNode>()

        while (!significantIsRightBrace && !isAtEnd) {
            val leading = takeLeadingTrivia()
            val child = parseNode()
            if (child != null) {
                children.add(
                    RainbowNode(
                        name = child.name,
                        parameters = child.parameters,
                        block = child.block,
                        leading = leading,
                    ),
                )
            } else {
                synchronizeNode()
            }
        }

        takeLeadingTrivia()
        consume(RainbowTokenKind.RightBrace, "Expected '}' after block.")
        return RainbowBlock(children)
    }

    private fun parseValue(): RainbowValue? {
        return when (val kind = peek.kind) {
            is RainbowTokenKind.StringToken -> {
                advance()
                RainbowValue.StringValue(kind.value)
            }
            is RainbowTokenKind.IntToken -> {
                advance()
                RainbowValue.IntValue(kind.value)
            }
            is RainbowTokenKind.DoubleToken -> {
                advance()
                RainbowValue.DoubleValue(kind.value)
            }
            is RainbowTokenKind.BoolToken -> {
                advance()
                RainbowValue.BoolValue(kind.value)
            }
            is RainbowTokenKind.Null -> {
                advance()
                RainbowValue.Null
            }
            is RainbowTokenKind.DotIdentifier -> {
                advance()
                RainbowValue.Identifier(kind.value)
            }
            is RainbowTokenKind.Identifier -> {
                appendUnexpectedToken(
                    "Enum identifier values must start with '.'; did you mean '.${kind.value}'?",
                )
                advance()
                null
            }
            is RainbowTokenKind.Tagged -> {
                advance()
                RainbowValue.Tagged(
                    language = kind.language,
                    body = kind.body,
                    bodyRange = kind.bodyRange,
                )
            }
            is RainbowTokenKind.LeftBracket -> {
                advance()
                parseArray()
            }
            is RainbowTokenKind.LeftParen -> {
                advance()
                parseObject()
            }
            else -> {
                appendUnexpectedToken("Expected value.")
                null
            }
        }
    }

    private fun parseArray(): RainbowValue {
        val values = mutableListOf<RainbowValue>()

        if (match(RainbowTokenKind.RightBracket)) {
            return RainbowValue.ArrayValue(values)
        }

        do {
            if (check(RainbowTokenKind.RightBracket) || check(RainbowTokenKind.Eof)) break
            val value = parseValue()
            if (value != null) {
                values.add(value)
            } else {
                synchronizeValue()
            }
        } while (match(RainbowTokenKind.Comma))

        consume(RainbowTokenKind.RightBracket, "Expected ']' after array.")
        return RainbowValue.ArrayValue(values)
    }

    private fun parseObject(): RainbowValue {
        val entries = mutableListOf<RainbowObjectEntry>()

        if (match(RainbowTokenKind.RightParen)) {
            return RainbowValue.ObjectValue(entries)
        }

        while (true) {
            if (check(RainbowTokenKind.RightParen) || check(RainbowTokenKind.Eof)) break

            val key = parseObjectKey()
            if (key == null) {
                synchronizeValue()
                if (!match(RainbowTokenKind.Comma)) break
                continue
            }

            consume(RainbowTokenKind.Colon, "Expected ':' after object key.")

            val value = parseValue()
            if (value == null) {
                synchronizeValue()
                if (!match(RainbowTokenKind.Comma)) break
                continue
            }

            entries.add(RainbowObjectEntry(key = key, value = value))

            if (!match(RainbowTokenKind.Comma)) break

            if (check(RainbowTokenKind.RightParen)) {
                diagnostics.add(
                    RainbowDiagnostic(
                        code = "rainbow.parser.unexpectedToken",
                        message = "Trailing comma is not allowed after the last object entry.",
                        range = previous.range,
                    ),
                )
                break
            }
        }

        consume(RainbowTokenKind.RightParen, "Expected ')' after object.")
        return RainbowValue.ObjectValue(entries)
    }

    private fun parseObjectKey(): String? {
        return when (val kind = peek.kind) {
            is RainbowTokenKind.Identifier -> {
                advance()
                kind.value
            }
            is RainbowTokenKind.StringToken -> {
                advance()
                kind.value
            }
            else -> {
                appendUnexpectedToken("Expected object key.")
                null
            }
        }
    }

    private fun consume(kind: RainbowTokenKind, message: String): RainbowToken? {
        if (check(kind)) return advance()
        appendUnexpectedToken(message)
        return null
    }

    private fun match(kind: RainbowTokenKind): Boolean {
        if (!check(kind)) return false
        advance()
        return true
    }

    private fun check(kind: RainbowTokenKind): Boolean = peek.kind == kind

    private val significantIsRightParen: Boolean
        get() = significantKindMatches { it == RainbowTokenKind.RightParen }

    private val significantIsRightBrace: Boolean
        get() = significantKindMatches { it == RainbowTokenKind.RightBrace }

    private fun significantKindMatches(predicate: (RainbowTokenKind) -> Boolean): Boolean {
        var index = current
        while (index < tokens.size) {
            when (tokens[index].kind) {
                is RainbowTokenKind.Comment -> index += 1
                else -> return predicate(tokens[index].kind)
            }
        }
        return false
    }

    private fun advance(): RainbowToken {
        if (!isAtEnd) current += 1
        return previous
    }

    private val isAtEnd: Boolean
        get() = peek.kind == RainbowTokenKind.Eof

    private val peek: RainbowToken
        get() = tokens[current]

    private val previous: RainbowToken
        get() = tokens[current - 1]

    private fun synchronizeNode() {
        while (!isAtEnd) {
            if (check(RainbowTokenKind.RightBrace) || isNodeStart(peek)) return
            advance()
        }
    }

    private fun synchronizeParameter() {
        while (!isAtEnd) {
            if (check(RainbowTokenKind.Comma) || check(RainbowTokenKind.RightParen)) return
            advance()
        }
    }

    private fun synchronizeValue() {
        while (!isAtEnd) {
            if (
                check(RainbowTokenKind.Comma) ||
                check(RainbowTokenKind.RightBracket) ||
                check(RainbowTokenKind.RightBrace) ||
                check(RainbowTokenKind.RightParen)
            ) {
                return
            }
            advance()
        }
    }

    private fun isNodeStart(token: RainbowToken): Boolean =
        token.kind is RainbowTokenKind.Identifier

    private fun appendUnexpectedToken(message: String) {
        diagnostics.add(
            RainbowDiagnostic(
                code = "rainbow.parser.unexpectedToken",
                message = message,
                range = peek.range,
            ),
        )
    }
}
