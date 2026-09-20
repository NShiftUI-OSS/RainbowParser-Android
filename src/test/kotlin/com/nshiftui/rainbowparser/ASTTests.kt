package com.nshiftui.rainbowparser

import com.nshiftui.rainbowparser.ast.RainbowBlock
import com.nshiftui.rainbowparser.ast.RainbowDocument
import com.nshiftui.rainbowparser.ast.RainbowNode
import com.nshiftui.rainbowparser.ast.RainbowParameter
import com.nshiftui.rainbowparser.ast.RainbowValue
import com.nshiftui.rainbowparser.diagnostics.RainbowDiagnostic
import com.nshiftui.rainbowparser.diagnostics.RainbowDiagnosticSeverity
import com.nshiftui.rainbowparser.diagnostics.RainbowParseError
import com.nshiftui.rainbowparser.diagnostics.RainbowSourceLocation
import com.nshiftui.rainbowparser.diagnostics.RainbowSourceRange
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ASTTests : FunSpec({
    test("nodeInitializersExposeChildrenAndBlockState") {
        val leaf = RainbowNode(name = "Spacer")
        leaf.children shouldBe emptyList()
        leaf.hasBlock shouldBe false

        val emptyBlock = RainbowNode(name = "OnTap", block = RainbowBlock())
        emptyBlock.children shouldBe emptyList()
        emptyBlock.hasBlock shouldBe true

        val child = RainbowNode(
            name = "Text",
            parameters = listOf(
                RainbowParameter(name = "value", value = RainbowValue.StringValue("Hello")),
            ),
        )
        val parent = RainbowNode(name = "Button", children = listOf(child))
        parent.children shouldBe listOf(child)
        parent.hasBlock shouldBe true
    }

    test("documentDefaultsToNoRootNodes") {
        RainbowDocument().nodes shouldBe emptyList()
    }

    test("parseErrorDescriptionIncludesAllDiagnostics") {
        val start = RainbowSourceLocation(offset = 0, line = 1, column = 1)
        val end = RainbowSourceLocation(offset = 1, line = 1, column = 2)
        val range = RainbowSourceRange(start = start, end = end)
        val diagnostic = RainbowDiagnostic(
            code = "test.code",
            message = "Example failure.",
            severity = RainbowDiagnosticSeverity.WARNING,
            range = range,
        )
        val error = RainbowParseError(diagnostic)

        (start < end) shouldBe true
        diagnostic.description shouldBe "WARNING test.code at 1:1: Example failure."
        error.description shouldBe diagnostic.description
    }
})
