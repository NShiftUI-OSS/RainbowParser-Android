package com.nshiftui.rainbowparser.lexer

import com.nshiftui.rainbowparser.ast.EmbeddedLanguage
import com.nshiftui.rainbowparser.ast.RainbowTaggedBody
import com.nshiftui.rainbowparser.diagnostics.RainbowDiagnostic
import com.nshiftui.rainbowparser.diagnostics.RainbowParseError
import com.nshiftui.rainbowparser.diagnostics.RainbowSourceLocation
import com.nshiftui.rainbowparser.diagnostics.RainbowSourceRange
import com.nshiftui.rainbowparser.support.CharacterScanner
import com.nshiftui.rainbowparser.support.SourceText
import com.nshiftui.rainbowparser.support.dropFirstGraphemes
import com.nshiftui.rainbowparser.support.endsWithNewlineGrapheme
import com.nshiftui.rainbowparser.support.isBlankIndentLine
import com.nshiftui.rainbowparser.support.isRainbowDigit
import com.nshiftui.rainbowparser.support.isRainbowIdentifierContinuation
import com.nshiftui.rainbowparser.support.isRainbowIdentifierStart
import com.nshiftui.rainbowparser.support.isRainbowWhitespace
import com.nshiftui.rainbowparser.support.leadingIndentCount
import com.nshiftui.rainbowparser.support.splitOnNewlineGrapheme

internal class RainbowLexer(source: String) {
    private val scanner = CharacterScanner(SourceText(source))
    private val diagnostics = mutableListOf<RainbowDiagnostic>()

    fun scanTokens(): List<RainbowToken> {
        val tokens = mutableListOf<RainbowToken>()

        while (!scanner.isAtEnd) {
            skipWhitespace()
            if (scanner.isAtEnd) break

            val start = scanner.location
            val character = scanner.advance() ?: break

            when (character) {
                "(" -> tokens.add(makeToken(RainbowTokenKind.LeftParen, start))
                ")" -> tokens.add(makeToken(RainbowTokenKind.RightParen, start))
                "{" -> tokens.add(makeToken(RainbowTokenKind.LeftBrace, start))
                "}" -> tokens.add(makeToken(RainbowTokenKind.RightBrace, start))
                "[" -> tokens.add(makeToken(RainbowTokenKind.LeftBracket, start))
                "]" -> tokens.add(makeToken(RainbowTokenKind.RightBracket, start))
                ":" -> tokens.add(makeToken(RainbowTokenKind.Colon, start))
                "," -> tokens.add(makeToken(RainbowTokenKind.Comma, start))
                "/" if scanner.peek() == "/" -> {
                    scanner.advance()
                    val text = StringBuilder()
                    while (true) {
                        val next = scanner.peek() ?: break
                        if (next == "\n") break
                        text.append(scanner.advance())
                    }
                    if (text.lastOrNull() == '\r') {
                        text.deleteCharAt(text.length - 1)
                    }
                    tokens.add(makeToken(RainbowTokenKind.Comment(text.toString()), start))
                }
                "/" -> appendUnexpectedCharacter("/", start)
                "@" -> {
                    val tagged = tryScanTagged(start)
                    if (tagged != null) {
                        tokens.add(tagged)
                    } else {
                        tokens.add(makeToken(RainbowTokenKind.At, start))
                    }
                }
                "." if scanner.peek()?.isRainbowIdentifierStart() == true ->
                    tokens.add(scanDotIdentifier(start))
                "." -> appendUnexpectedCharacter(character, start)
                "\"" -> {
                    val token = scanString(start)
                    if (token != null) tokens.add(token)
                }
                "-" if scanner.peek()?.isRainbowDigit() == true ->
                    tokens.add(scanNumber(start, character))
                "-" -> appendUnexpectedCharacter(character, start)
                else if character.isRainbowDigit() -> tokens.add(scanNumber(start, character))
                else if character.isRainbowIdentifierStart() ->
                    tokens.add(scanIdentifier(start, character))
                else -> appendUnexpectedCharacter(character, start)
            }
        }

        val eofLocation = scanner.location
        tokens.add(
            RainbowToken(
                kind = RainbowTokenKind.Eof,
                range = RainbowSourceRange(start = eofLocation, end = eofLocation),
            ),
        )

        if (diagnostics.isEmpty()) {
            return tokens
        }
        throw RainbowParseError(diagnostics)
    }

    private fun skipWhitespace() {
        while (true) {
            val character = scanner.peek() ?: break
            if (!character.isRainbowWhitespace()) break
            scanner.advance()
        }
    }

    private fun makeToken(kind: RainbowTokenKind, start: RainbowSourceLocation): RainbowToken =
        RainbowToken(
            kind = kind,
            range = RainbowSourceRange(start = start, end = scanner.location),
        )

    private fun scanString(start: RainbowSourceLocation): RainbowToken? {
        val value = StringBuilder()

        while (true) {
            val character = scanner.peek() ?: break
            if (character == "\"") {
                scanner.advance()
                return makeToken(RainbowTokenKind.StringToken(value.toString()), start)
            }

            if (character == "\n") {
                appendDiagnostic(
                    code = "rainbow.lexer.unterminatedString",
                    message = "Unterminated string literal.",
                    start = start,
                    end = scanner.location,
                )
                return null
            }

            if (character == "\\") {
                scanner.advance()
                val escaped = scanner.advance()
                if (escaped == null) {
                    appendDiagnostic(
                        code = "rainbow.lexer.unterminatedString",
                        message = "Unterminated string literal.",
                        start = start,
                        end = scanner.location,
                    )
                    return null
                }

                when (escaped) {
                    "\"" -> value.append("\"")
                    "\\" -> value.append("\\")
                    "n" -> value.append("\n")
                    "r" -> value.append("\r")
                    "t" -> value.append("\t")
                    "0" -> value.append('\u0000')
                    else -> appendDiagnostic(
                        code = "rainbow.lexer.invalidEscape",
                        message = "Invalid escape sequence \\$escaped.",
                        start = start,
                        end = scanner.location,
                    )
                }
            } else {
                value.append(character)
                scanner.advance()
            }
        }

        appendDiagnostic(
            code = "rainbow.lexer.unterminatedString",
            message = "Unterminated string literal.",
            start = start,
            end = scanner.location,
        )
        return null
    }

    private fun scanNumber(start: RainbowSourceLocation, first: String): RainbowToken {
        val text = StringBuilder(first)

        while (true) {
            val character = scanner.peek() ?: break
            if (!character.isRainbowDigit()) break
            text.append(character)
            scanner.advance()
        }

        var dotCount = 0
        while (scanner.peek() == ".") {
            val next = scanner.peekNext() ?: break
            if (!next.isRainbowDigit()) break
            dotCount += 1
            text.append(".")
            scanner.advance()
            while (true) {
                val character = scanner.peek() ?: break
                if (!character.isRainbowDigit()) break
                text.append(character)
                scanner.advance()
            }
        }

        val literal = text.toString()
        if (dotCount >= 2) {
            return makeToken(RainbowTokenKind.Version(literal), start)
        }
        if (dotCount == 1) {
            return makeToken(RainbowTokenKind.DoubleToken(literal.toDouble()), start)
        }

        val value = literal.toLongOrNull()
        if (value == null) {
            appendDiagnostic(
                code = "rainbow.lexer.invalidNumber",
                message = "Invalid number literal '$literal'.",
                start = start,
                end = scanner.location,
            )
            return makeToken(RainbowTokenKind.IntToken(0), start)
        }
        return makeToken(RainbowTokenKind.IntToken(value), start)
    }

    private fun scanIdentifier(start: RainbowSourceLocation, first: String): RainbowToken {
        val text = StringBuilder(first)
        while (true) {
            val character = scanner.peek() ?: break
            if (!character.isRainbowIdentifierContinuation()) break
            text.append(character)
            scanner.advance()
        }

        return when (val identifier = text.toString()) {
            "true" -> makeToken(RainbowTokenKind.BoolToken(true), start)
            "false" -> makeToken(RainbowTokenKind.BoolToken(false), start)
            "null" -> makeToken(RainbowTokenKind.Null, start)
            else -> makeToken(RainbowTokenKind.Identifier(identifier), start)
        }
    }

    private fun scanDotIdentifier(start: RainbowSourceLocation): RainbowToken {
        val first = scanner.advance()!!
        val text = StringBuilder(first)
        while (true) {
            val character = scanner.peek() ?: break
            if (!character.isRainbowIdentifierContinuation()) break
            text.append(character)
            scanner.advance()
        }
        return makeToken(RainbowTokenKind.DotIdentifier(text.toString()), start)
    }

    /** After `@` has been consumed: try `@LANG(...)`. Restores scanner on mismatch. */
    private fun tryScanTagged(start: RainbowSourceLocation): RainbowToken? {
        val checkpoint = scanner.checkpoint()
        val first = scanner.peek()
        if (first == null || !first.isRainbowIdentifierStart()) {
            return null
        }
        scanner.advance()
        val languageName = StringBuilder(first)
        while (true) {
            val character = scanner.peek() ?: break
            if (!character.isRainbowIdentifierContinuation()) break
            languageName.append(character)
            scanner.advance()
        }

        if (scanner.peek() != "(") {
            scanner.restore(checkpoint)
            return null
        }
        scanner.advance()

        val parsedLanguage = EmbeddedLanguage.parse(languageName.toString())
        if (parsedLanguage == null) {
            appendDiagnostic(
                code = "rainbow.lexer.unknownEmbeddedLanguage",
                message = "Unknown embedded language '@$languageName'. Expected JSON, YAML, XML, HTML, or MARKDOWN.",
                start = start,
                end = scanner.location,
            )
            scanTaggedRawBody(tagStart = start, language = EmbeddedLanguage.JSON)
            return null
        }

        val bodyStart = scanner.location
        if (scanner.peek() == "#" && scanner.peekNext() == "{") {
            scanner.advance()
            val name = scanPlaceholderName(bodyStart) ?: return null
            val bodyEnd = scanner.location
            if (scanner.peek() != ")") {
                appendDiagnostic(
                    code = "rainbow.lexer.unterminatedTagged",
                    message = "Expected ')' after '@${parsedLanguage.asString}(#{$name})'.",
                    start = start,
                    end = scanner.location,
                )
                return null
            }
            scanner.advance()
            return RainbowToken(
                kind = RainbowTokenKind.Tagged(
                    language = parsedLanguage,
                    body = RainbowTaggedBody.Placeholder(name),
                    bodyRange = RainbowSourceRange(start = bodyStart, end = bodyEnd),
                ),
                range = RainbowSourceRange(start = start, end = scanner.location),
            )
        }

        val raw = scanTaggedRawBody(tagStart = start, language = parsedLanguage) ?: return null
        return RainbowToken(
            kind = RainbowTokenKind.Tagged(
                language = parsedLanguage,
                body = RainbowTaggedBody.Text(raw.first),
                bodyRange = RainbowSourceRange(start = bodyStart, end = raw.second),
            ),
            range = RainbowSourceRange(start = start, end = scanner.location),
        )
    }

    private fun scanPlaceholderName(start: RainbowSourceLocation): String? {
        if (scanner.peek() != "{") {
            appendDiagnostic(
                code = "rainbow.lexer.invalidPlaceholder",
                message = "Expected placeholder name after '#{'.",
                start = start,
                end = scanner.location,
            )
            return null
        }
        scanner.advance()

        val first = scanner.peek()
        if (first == null || !first.isRainbowIdentifierStart()) {
            appendDiagnostic(
                code = "rainbow.lexer.invalidPlaceholder",
                message = "Expected placeholder name after '#{'.",
                start = start,
                end = scanner.location,
            )
            return null
        }
        scanner.advance()
        val text = StringBuilder(first)
        while (true) {
            val character = scanner.peek() ?: break
            if (!character.isRainbowIdentifierContinuation()) break
            text.append(character)
            scanner.advance()
        }

        if (scanner.peek() != "}") {
            appendDiagnostic(
                code = "rainbow.lexer.unterminatedPlaceholder",
                message = "Unterminated placeholder; expected '}'.",
                start = start,
                end = scanner.location,
            )
            return null
        }
        scanner.advance()
        return text.toString()
    }

    private fun scanTaggedRawBody(
        tagStart: RainbowSourceLocation,
        language: EmbeddedLanguage,
    ): Pair<String, RainbowSourceLocation>? {
        var depth = 1
        val body = StringBuilder()
        var inString: String? = null
        var escape = false
        var inLineComment = false
        var inBlockComment = false
        var inCDATA = false
        var inMDFence = false

        val allowYAMLHashComments = language == EmbeddedLanguage.YAML
        val allowXMLMarkup = language == EmbeddedLanguage.XML || language == EmbeddedLanguage.HTML
        val allowSingleQuotes = language != EmbeddedLanguage.JSON
        val markdown = language == EmbeddedLanguage.MARKDOWN

        while (!scanner.isAtEnd) {
            val character = scanner.peek() ?: break

            if (
                inString == null &&
                !inLineComment &&
                !inBlockComment &&
                !inCDATA &&
                character == "#" &&
                scanner.peekNext() == "{"
            ) {
                appendDiagnostic(
                    code = "rainbow.lexer.embeddedPartialPlaceholder",
                    message = "Partial '#{...}' interpolation is not allowed inside @LANG(...); use '@LANG(#{Name})' for a whole-blob placeholder.",
                    start = scanner.location,
                    end = scanner.location,
                )
                return null
            }

            if (markdown && inString == null && !inMDFence && scanner.startsWith("```")) {
                repeat(3) { body.append(scanner.advance()) }
                inMDFence = true
                continue
            }
            if (markdown && inMDFence) {
                if (scanner.startsWith("```")) {
                    repeat(3) { body.append(scanner.advance()) }
                    inMDFence = false
                    continue
                }
                if (character == "(") {
                    depth += 1
                    body.append(character)
                    scanner.advance()
                    continue
                }
                if (character == ")") {
                    depth -= 1
                    if (depth == 0) {
                        val bodyEnd = scanner.location
                        scanner.advance()
                        return dedentEmbeddedBody(body.toString()) to bodyEnd
                    }
                    body.append(character)
                    scanner.advance()
                    continue
                }
                body.append(character)
                scanner.advance()
                continue
            }

            if (inLineComment) {
                body.append(character)
                scanner.advance()
                if (character == "\n") {
                    inLineComment = false
                }
                continue
            }

            if (inBlockComment) {
                body.append(character)
                scanner.advance()
                if (character == "-" && scanner.peek() == "-" && scanner.peekNext() == ">") {
                    body.append(scanner.advance())
                    body.append(scanner.advance())
                    inBlockComment = false
                }
                continue
            }

            if (inCDATA) {
                body.append(character)
                scanner.advance()
                if (character == "]" && scanner.peek() == "]" && scanner.peekNext() == ">") {
                    body.append(scanner.advance())
                    body.append(scanner.advance())
                    inCDATA = false
                }
                continue
            }

            val quote = inString
            if (quote != null) {
                body.append(character)
                scanner.advance()
                if (escape) {
                    escape = false
                    continue
                }
                if (character == "\\" && quote == "\"") {
                    escape = true
                    continue
                }
                if (character == quote) {
                    inString = null
                }
                continue
            }

            if (
                allowXMLMarkup &&
                character == "<" &&
                scanner.peekNext() == "!" &&
                scanner.peekOffset(2) == "-" &&
                scanner.peekOffset(3) == "-"
            ) {
                repeat(4) { body.append(scanner.advance()) }
                inBlockComment = true
                continue
            }

            if (allowXMLMarkup && scanner.startsWith("<![CDATA[")) {
                repeat("<![CDATA[".length) { body.append(scanner.advance()) }
                inCDATA = true
                continue
            }

            if (allowYAMLHashComments && character == "#") {
                inLineComment = true
                body.append(character)
                scanner.advance()
                continue
            }

            if (character == "\"" || (allowSingleQuotes && character == "'")) {
                inString = character
                body.append(character)
                scanner.advance()
                continue
            }

            if (character == "(") {
                depth += 1
                body.append(character)
                scanner.advance()
                continue
            }

            if (character == ")") {
                depth -= 1
                if (depth == 0) {
                    val bodyEnd = scanner.location
                    scanner.advance()
                    return dedentEmbeddedBody(body.toString()) to bodyEnd
                }
                body.append(character)
                scanner.advance()
                continue
            }

            body.append(character)
            scanner.advance()
        }

        appendDiagnostic(
            code = "rainbow.lexer.unterminatedTagged",
            message = "Unterminated @LANG(...); expected closing ')'.",
            start = tagStart,
            end = scanner.location,
        )
        return null
    }

    private fun appendUnexpectedCharacter(character: String, start: RainbowSourceLocation) {
        appendDiagnostic(
            code = "rainbow.lexer.unexpectedCharacter",
            message = "Unexpected character '$character'.",
            start = start,
            end = scanner.location,
        )
    }

    private fun appendDiagnostic(
        code: String,
        message: String,
        start: RainbowSourceLocation,
        end: RainbowSourceLocation,
    ) {
        diagnostics.add(
            RainbowDiagnostic(
                code = code,
                message = message,
                range = RainbowSourceRange(start = start, end = end),
            ),
        )
    }

    companion object {
        private fun dedentEmbeddedBody(body: String): String {
            if (!body.contains('\n')) return body

            val lines = splitOnNewlineGrapheme(body)
            val indents = lines.drop(1).mapNotNull { line ->
                if (isBlankIndentLine(line)) null else leadingIndentCount(line)
            }
            val indent = indents.minOrNull() ?: return body
            if (indent <= 0) return body

            val output = StringBuilder()
            for ((index, line) in lines.withIndex()) {
                if (index > 0) output.append('\n')
                if (index == 0 || isBlankIndentLine(line)) {
                    output.append(line)
                } else {
                    output.append(dropFirstGraphemes(line, indent))
                }
            }
            if (body.endsWithNewlineGrapheme()) {
                output.append('\n')
            }
            return output.toString()
        }
    }
}
