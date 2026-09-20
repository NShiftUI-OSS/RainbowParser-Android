package com.nshiftui.rainbowparser

import com.nshiftui.rainbowparser.ast.EmbeddedLanguage
import com.nshiftui.rainbowparser.ast.RainbowTaggedBody
import com.nshiftui.rainbowparser.lexer.RainbowTokenKind
import com.nshiftui.rainbowparser.support.CharacterScanner
import com.nshiftui.rainbowparser.support.SourceText
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class LexerTests : FunSpec({
    test("lexerScansPunctuationAndEof") {
        val tokens = scan("(){}[]:,@")
        tokens.map { it.kind } shouldBe listOf(
            RainbowTokenKind.LeftParen,
            RainbowTokenKind.RightParen,
            RainbowTokenKind.LeftBrace,
            RainbowTokenKind.RightBrace,
            RainbowTokenKind.LeftBracket,
            RainbowTokenKind.RightBracket,
            RainbowTokenKind.Colon,
            RainbowTokenKind.Comma,
            RainbowTokenKind.At,
            RainbowTokenKind.Eof,
        )
    }

    test("lexerScansSemVerAsVersionToken") {
        val tokens = scan("1.0.0 1.25")
        tokens.map { it.kind } shouldBe listOf(
            RainbowTokenKind.Version("1.0.0"),
            RainbowTokenKind.DoubleToken(1.25),
            RainbowTokenKind.Eof,
        )
    }

    test("lexerScansWhitespaceOnlyInputAsEof") {
        val tokens = scan(" \r\n\t")
        tokens.map { it.kind } shouldBe listOf(RainbowTokenKind.Eof)
        tokens[0].range.start.line shouldBe 2
        tokens[0].range.start.column shouldBe 2
    }

    test("lexerScansLiteralsAndKeywords") {
        val tokens = scan("Button title \"hello\\nworld\" -12 3.5 true false null .primary")
        tokens.map { it.kind } shouldBe listOf(
            RainbowTokenKind.Identifier("Button"),
            RainbowTokenKind.Identifier("title"),
            RainbowTokenKind.StringToken("hello\nworld"),
            RainbowTokenKind.IntToken(-12),
            RainbowTokenKind.DoubleToken(3.5),
            RainbowTokenKind.BoolToken(true),
            RainbowTokenKind.BoolToken(false),
            RainbowTokenKind.Null,
            RainbowTokenKind.DotIdentifier("primary"),
            RainbowTokenKind.Eof,
        )
    }

    test("lexerScansSupportedStringEscapes") {
        val tokens = scan("\"quote: \\\" slash: \\\\ tab: \\t return: \\r null: \\0\"")
        tokens.map { it.kind } shouldBe listOf(
            RainbowTokenKind.StringToken("quote: \" slash: \\ tab: \t return: \r null: \u0000"),
            RainbowTokenKind.Eof,
        )
    }

    test("lexerTracksLineAndColumn") {
        val tokens = scan("Screen\n  Button")
        tokens[0].range.start.line shouldBe 1
        tokens[0].range.start.column shouldBe 1
        tokens[1].range.start.line shouldBe 2
        tokens[1].range.start.column shouldBe 3
    }

    test("lexerReportsUnexpectedCharactersAndInvalidNumbers") {
        val error = expectLexerError("# -")
        error.diagnostics.map { it.code } shouldBe listOf(
            "rainbow.lexer.unexpectedCharacter",
            "rainbow.lexer.unexpectedCharacter",
        )
        error.diagnostics[0].message shouldBe "Unexpected character '#'."
        error.diagnostics[1].message shouldBe "Unexpected character '-'."
    }

    test("lexerReportsUnexpectedLoneDot") {
        val error = expectLexerError(".")
        error.diagnostics.map { it.code } shouldBe listOf("rainbow.lexer.unexpectedCharacter")
        error.diagnostics[0].message shouldBe "Unexpected character '.'."
    }

    test("lexerReportsIntegerOverflow") {
        val error = expectLexerError("999999999999999999999999999999999999")
        error.diagnostics.map { it.code } shouldBe listOf("rainbow.lexer.invalidNumber")
        error.diagnostics[0].message shouldBe
            "Invalid number literal '999999999999999999999999999999999999'."
    }

    test("lexerReportsUnterminatedStringAtEndOfFile") {
        val error = expectLexerError("\"missing")
        error.diagnostics.map { it.code } shouldBe listOf("rainbow.lexer.unterminatedString")
        error.diagnostics[0].range.start.line shouldBe 1
        error.diagnostics[0].range.start.column shouldBe 1
    }

    test("lexerReportsUnterminatedStringAfterTrailingEscape") {
        val error = expectLexerError("\"missing \\")
        error.diagnostics.map { it.code } shouldBe listOf("rainbow.lexer.unterminatedString")
    }

    test("lexerReportsUnterminatedStringAtLineBreak") {
        val error = expectLexerError("\"missing\nNext")
        error.diagnostics.map { it.code } shouldBe listOf("rainbow.lexer.unterminatedString")
    }

    test("lexerReportsInvalidEscape") {
        val error = expectLexerError("\"bad \\x escape\"")
        error.diagnostics.map { it.code } shouldBe listOf("rainbow.lexer.invalidEscape")
        error.diagnostics[0].message shouldBe "Invalid escape sequence \\x."
    }

    test("lexerReportsNonAsciiComposedCharacters") {
        val error = expectLexerError("🇧🇷")
        error.diagnostics.map { it.code } shouldBe listOf("rainbow.lexer.unexpectedCharacter")
    }

    test("characterScannerSupportsPeekingAndAdvancing") {
        val scanner = CharacterScanner(SourceText("A\nB"))

        scanner.peek() shouldBe "A"
        scanner.peekNext() shouldBe "\n"
        scanner.advance() shouldBe "A"
        scanner.location.line shouldBe 1
        scanner.location.column shouldBe 2
        scanner.advance() shouldBe "\n"
        scanner.location.line shouldBe 2
        scanner.location.column shouldBe 1
        scanner.advance() shouldBe "B"
        scanner.peek().shouldBeNull()
        scanner.peekNext().shouldBeNull()
        scanner.advance().shouldBeNull()

        val singleCharacterScanner = CharacterScanner(SourceText("Z"))
        singleCharacterScanner.peekNext().shouldBeNull()
    }

    test("lexerScansTaggedJsonAndPlaceholderBlob") {
        val tokens = scan("@JSON({\"a\":1}) @JSON(#{blob})")
        val first = tokens[0].kind as RainbowTokenKind.Tagged
        first.language shouldBe EmbeddedLanguage.JSON
        first.body shouldBe RainbowTaggedBody.Text("{\"a\":1}")

        val second = tokens[1].kind as RainbowTokenKind.Tagged
        second.body shouldBe RainbowTaggedBody.Placeholder("blob")
    }

    test("lexerRejectsPartialPlaceholderInsideTagged") {
        val error = expectLexerError("@JSON({\"a\": #{x}})")
        error.diagnostics[0].code shouldBe "rainbow.lexer.embeddedPartialPlaceholder"
    }

    test("lexerKeepsAtForUsePins") {
        val tokens = scan("use Screen@1.0.0")
        tokens.any { it.kind == RainbowTokenKind.At } shouldBe true
        tokens.any { it.kind is RainbowTokenKind.Tagged } shouldBe false
    }
})
