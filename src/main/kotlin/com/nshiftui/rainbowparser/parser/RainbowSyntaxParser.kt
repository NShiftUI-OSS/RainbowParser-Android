package com.nshiftui.rainbowparser.parser

import com.nshiftui.rainbowparser.ast.RainbowBlock
import com.nshiftui.rainbowparser.ast.RainbowDocument
import com.nshiftui.rainbowparser.ast.RainbowLeadingTrivia
import com.nshiftui.rainbowparser.ast.RainbowNode
import com.nshiftui.rainbowparser.ast.RainbowObjectEntry
import com.nshiftui.rainbowparser.ast.RainbowParameter
import com.nshiftui.rainbowparser.ast.RainbowSlot
import com.nshiftui.rainbowparser.ast.RainbowTrigger
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
                        nodes.add(node.copy(leading = leading))
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

        val hadParentheses = match(RainbowTokenKind.LeftParen)
        val parameters = if (hadParentheses) parseArguments() else emptyList()

        var anchor = previous.range.end.line
        val block = if (check(RainbowTokenKind.LeftBrace)) {
            if (!hadParentheses) {
                diagnostics.add(
                    RainbowDiagnostic(
                        code = "rainbow.parser.childrenBrace",
                        message = "A children block belongs after '()', on the same line as ')'.",
                        range = peek.range,
                    ),
                )
            } else if (peek.range.start.line != anchor) {
                diagnostics.add(
                    RainbowDiagnostic(
                        code = "rainbow.parser.childrenBrace",
                        message = "The children block must open on the same line as ')'.",
                        range = peek.range,
                    ),
                )
            }
            advance()
            val parsed = parseBlock()
            anchor = previous.range.end.line
            parsed
        } else {
            null
        }

        val modifiers = parseModifiers(anchor)
        return RainbowNode(
            name = nameKind.value,
            parameters = parameters,
            block = block,
            id = modifiers.id,
            style = modifiers.style,
            events = modifiers.events,
            slots = modifiers.slots,
        )
    }

    private class ParsedModifiers {
        var id: String? = null
        val style = mutableListOf<RainbowParameter>()
        val events = mutableListOf<RainbowTrigger>()
        val slots = mutableListOf<RainbowSlot>()
    }

    private fun parseModifiers(initialAnchor: Int): ParsedModifiers {
        var anchor = initialAnchor
        val parsed = ParsedModifiers()
        val seen = mutableListOf<String>()
        while (true) {
            val kind = peek.kind
            if (kind is RainbowTokenKind.Comment) {
                if (commentIntroducesModifier()) {
                    diagnostics.add(
                        RainbowDiagnostic(
                            code = "rainbow.parser.modifierSpacing",
                            message = "A comment cannot sit between a plugin and its view modifier.",
                            range = peek.range,
                        ),
                    )
                    advance()
                    continue
                }
                break
            }
            if (kind !is RainbowTokenKind.DotIdentifier) break
            val modifier = kind.value
            val known = modifier == "id" || modifier == "style" || modifier == "events" || modifier == "slots"
            val line = peek.range.start.line
            if (!known) {
                if (line <= anchor + 1) {
                    diagnostics.add(
                        RainbowDiagnostic(
                            code = "rainbow.parser.unknownModifier",
                            message = "Unknown view modifier '.$modifier'.",
                            range = peek.range,
                        ),
                    )
                    advance()
                    continue
                }
                break
            }
            if (line == anchor) {
                diagnostics.add(
                    RainbowDiagnostic(
                        code = "rainbow.parser.modifierSpacing",
                        message = "'.$modifier' must start on the line below the plugin.",
                        range = peek.range,
                    ),
                )
            } else if (line > anchor + 1) {
                diagnostics.add(
                    RainbowDiagnostic(
                        code = "rainbow.parser.modifierSpacing",
                        message = "'.$modifier' must be on the next line, with no blank line above it.",
                        range = peek.range,
                    ),
                )
            }
            if (modifier in seen) {
                diagnostics.add(
                    RainbowDiagnostic(
                        code = "rainbow.parser.duplicateModifier",
                        message = "'.$modifier' can be written only once on a plugin.",
                        range = peek.range,
                    ),
                )
            }
            seen.add(modifier)
            advance()
            when (modifier) {
                "id" -> parsed.id = parseIdModifier()
                "style" -> parsed.style.addAll(parseStyleModifier())
                "events" -> parsed.events.addAll(parseEventsModifier())
                "slots" -> parsed.slots.addAll(parseSlotsModifier())
            }
            anchor = previous.range.end.line
        }
        return parsed
    }

    private fun parseIdModifier(): String? {
        consume(RainbowTokenKind.LeftParen, "Expected '(' after '.id'.")
        val id = (peek.kind as? RainbowTokenKind.StringToken)?.value.also {
            if (it == null) appendUnexpectedToken("Expected a string inside '.id(\"...\")'.")
            else advance()
        }
        consume(RainbowTokenKind.RightParen, "Expected ')' after '.id'.")
        return id
    }

    private fun parseStyleModifier(): List<RainbowParameter> {
        if (!match(RainbowTokenKind.LeftParen)) {
            appendUnexpectedToken("Expected '(' after '.style'.")
            return emptyList()
        }
        return parseArguments()
    }

    private fun parseEventsModifier(): List<RainbowTrigger> {
        if (!braceOnSameLine("'.events'")) return emptyList()
        advance()
        val triggers = mutableListOf<RainbowTrigger>()
        while (!significantIsRightBrace && !isAtEnd) {
            val leading = takeLeadingTrivia()
            val trigger = parseTrigger()
            if (trigger != null) triggers.add(trigger.copy(leading = leading))
            else synchronizeNode()
        }
        takeLeadingTrivia()
        consume(RainbowTokenKind.RightBrace, "Expected '}' after '.events'.")
        return triggers
    }

    private fun parseTrigger(): RainbowTrigger? {
        val nameKind = peek.kind as? RainbowTokenKind.Identifier ?: run {
            appendUnexpectedToken("Expected a trigger name inside '.events'.")
            return null
        }
        advance()
        if (check(RainbowTokenKind.LeftParen)) {
            advance()
            parseArguments()
        }
        if (!braceOnSameLine("a trigger")) return null
        advance()
        return RainbowTrigger(name = nameKind.value, events = parseEventCalls())
    }

    private fun parseEventCalls(): List<RainbowNode> {
        val events = mutableListOf<RainbowNode>()
        while (!significantIsRightBrace && !isAtEnd) {
            val leading = takeLeadingTrivia()
            val event = parseNode()
            if (event != null) {
                if (event.block != null || event.style.isNotEmpty() || event.events.isNotEmpty() || event.slots.isNotEmpty()) {
                    diagnostics.add(
                        RainbowDiagnostic(
                            code = "rainbow.parser.eventShape",
                            message = "An event only accepts metadata in parentheses and an optional '.id'.",
                            range = previous.range,
                        ),
                    )
                }
                events.add(event.copy(leading = leading))
            } else {
                synchronizeNode()
            }
        }
        takeLeadingTrivia()
        consume(RainbowTokenKind.RightBrace, "Expected '}' after trigger.")
        return events
    }

    private fun parseSlotsModifier(): List<RainbowSlot> {
        if (!braceOnSameLine("'.slots'")) return emptyList()
        advance()
        val slots = mutableListOf<RainbowSlot>()
        while (!significantIsRightBrace && !isAtEnd) {
            val leading = takeLeadingTrivia()
            val slot = parseSlot()
            if (slot != null) slots.add(slot.copy(leading = leading))
            else synchronizeNode()
        }
        takeLeadingTrivia()
        consume(RainbowTokenKind.RightBrace, "Expected '}' after '.slots'.")
        return slots
    }

    private fun parseSlot(): RainbowSlot? {
        val nameKind = peek.kind as? RainbowTokenKind.Identifier ?: run {
            appendUnexpectedToken("Expected a slot name inside '.slots'.")
            return null
        }
        advance()
        if (!braceOnSameLine("a slot")) return null
        advance()
        return RainbowSlot(name = nameKind.value, plugins = parsePluginList())
    }

    private fun parsePluginList(): List<RainbowNode> {
        val plugins = mutableListOf<RainbowNode>()
        while (!significantIsRightBrace && !isAtEnd) {
            val leading = takeLeadingTrivia()
            val plugin = parseNode()
            if (plugin != null) plugins.add(plugin.copy(leading = leading))
            else synchronizeNode()
        }
        takeLeadingTrivia()
        consume(RainbowTokenKind.RightBrace, "Expected '}' after plugins.")
        return plugins
    }

    private fun braceOnSameLine(what: String): Boolean {
        if (!check(RainbowTokenKind.LeftBrace)) {
            appendUnexpectedToken("Expected '{' on the same line as $what.")
            return false
        }
        if (peek.range.start.line != previous.range.end.line) {
            diagnostics.add(
                RainbowDiagnostic(
                    code = "rainbow.parser.braceLine",
                    message = "The '{' of $what must stay on the same line as its name.",
                    range = peek.range,
                ),
            )
        }
        return true
    }

    private fun commentIntroducesModifier(): Boolean {
        var index = current
        while (index < tokens.size) {
            when (val kind = tokens[index].kind) {
                is RainbowTokenKind.Comment -> index += 1
                is RainbowTokenKind.DotIdentifier -> {
                    return kind.value == "id" || kind.value == "style" || kind.value == "events" || kind.value == "slots"
                }
                else -> return false
            }
        }
        return false
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
                children.add(child.copy(leading = leading))
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
            is RainbowTokenKind.LeftBrace -> {
                advance()
                RainbowValue.Plugins(parsePluginList())
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
