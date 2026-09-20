package com.nshiftui.rainbowparser

import com.nshiftui.rainbowparser.ast.EmbeddedLanguage
import com.nshiftui.rainbowparser.ast.RainbowNode
import com.nshiftui.rainbowparser.ast.RainbowObjectEntry
import com.nshiftui.rainbowparser.ast.RainbowParameter
import com.nshiftui.rainbowparser.ast.RainbowTaggedBody
import com.nshiftui.rainbowparser.ast.RainbowUseDeclaration
import com.nshiftui.rainbowparser.ast.RainbowValue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class ParserTests : FunSpec({
    test("parserDecodesUseDeclarationsAndNodes") {
        val document = RainbowDecoder().decode(
            """
            use Screen@1.0.0
            use Button@2.1.0
            use ShowToast@1.3.0

            Screen(name: "Home") {
              Button(title: "Hi") {
                OnTap {
                  ShowToast(message: "ok")
                }
              }
            }
            """.trimIndent(),
        )

        document.uses shouldBe listOf(
            RainbowUseDeclaration(name = "Screen", version = "1.0.0"),
            RainbowUseDeclaration(name = "Button", version = "2.1.0"),
            RainbowUseDeclaration(name = "ShowToast", version = "1.3.0"),
        )
        document.nodes.map { it.name } shouldBe listOf("Screen")
    }

    test("parserDecodesNestedRainbowDocument") {
        val document = RainbowDecoder().decode(
            """
            Screen(name: "Home") {
              Button(title: "Entrar", variant: .primary) {
                OnTap {
                  Navigate(to: "Dashboard")
                }
              }
            }
            """.trimIndent(),
        )

        document.uses shouldBe emptyList()
        document.nodes shouldBe listOf(
            RainbowNode(
                name = "Screen",
                parameters = listOf(
                    RainbowParameter(name = "name", value = RainbowValue.StringValue("Home")),
                ),
                children = listOf(
                    RainbowNode(
                        name = "Button",
                        parameters = listOf(
                            RainbowParameter(name = "title", value = RainbowValue.StringValue("Entrar")),
                            RainbowParameter(name = "variant", value = RainbowValue.Identifier("primary")),
                        ),
                        children = listOf(
                            RainbowNode(
                                name = "OnTap",
                                children = listOf(
                                    RainbowNode(
                                        name = "Navigate",
                                        parameters = listOf(
                                            RainbowParameter(
                                                name = "to",
                                                value = RainbowValue.StringValue("Dashboard"),
                                            ),
                                        ),
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )
    }

    test("parserDecodesAllSupportedValueShapes") {
        val document = RainbowDecoder().decode(
            """
            Node(
              text: "value",
              int: -1,
              double: 1.25,
              enabled: true,
              disabled: false,
              variant: .primary,
              items: ["a", 1, null],
              meta: (id: "home", "dash-key": false),
              emptyArray: [],
              emptyObject: ()
            )
            """.trimIndent(),
        )

        document.nodes.first().parameters shouldBe listOf(
            RainbowParameter(name = "text", value = RainbowValue.StringValue("value")),
            RainbowParameter(name = "int", value = RainbowValue.IntValue(-1)),
            RainbowParameter(name = "double", value = RainbowValue.DoubleValue(1.25)),
            RainbowParameter(name = "enabled", value = RainbowValue.BoolValue(true)),
            RainbowParameter(name = "disabled", value = RainbowValue.BoolValue(false)),
            RainbowParameter(name = "variant", value = RainbowValue.Identifier("primary")),
            RainbowParameter(
                name = "items",
                value = RainbowValue.ArrayValue(
                    listOf(
                        RainbowValue.StringValue("a"),
                        RainbowValue.IntValue(1),
                        RainbowValue.Null,
                    ),
                ),
            ),
            RainbowParameter(
                name = "meta",
                value = RainbowValue.ObjectValue(
                    listOf(
                        RainbowObjectEntry(key = "id", value = RainbowValue.StringValue("home")),
                        RainbowObjectEntry(key = "dash-key", value = RainbowValue.BoolValue(false)),
                    ),
                ),
            ),
            RainbowParameter(name = "emptyArray", value = RainbowValue.ArrayValue(emptyList())),
            RainbowParameter(name = "emptyObject", value = RainbowValue.ObjectValue(emptyList())),
        )
    }

    test("parserPreservesEmptyArgumentsAndBlocks") {
        val document = RainbowDecoder().decode(
            """
            Root() {
              EmptyBlock {}
              Leaf
            }
            """.trimIndent(),
        )

        val root = document.nodes.first()
        root.parameters shouldBe emptyList()
        root.hasBlock shouldBe true
        root.children[0].name shouldBe "EmptyBlock"
        root.children[0].hasBlock shouldBe true
        root.children[1].name shouldBe "Leaf"
        root.children[1].hasBlock shouldBe false
    }

    test("parserAllowsTrailingCommasInArrays") {
        val document = RainbowDecoder().decode(
            """
            Node(
              values: [.one, .two,],
              object: (first: 1, second: 2)
            )
            """.trimIndent(),
        )

        document.nodes.first().parameters shouldBe listOf(
            RainbowParameter(
                name = "values",
                value = RainbowValue.ArrayValue(
                    listOf(RainbowValue.Identifier("one"), RainbowValue.Identifier("two")),
                ),
            ),
            RainbowParameter(
                name = "object",
                value = RainbowValue.ObjectValue(
                    listOf(
                        RainbowObjectEntry(key = "first", value = RainbowValue.IntValue(1)),
                        RainbowObjectEntry(key = "second", value = RainbowValue.IntValue(2)),
                    ),
                ),
            ),
        )
    }

    test("parserRejectsTrailingCommaAfterLastObjectEntry") {
        val error = expectDecodeError("Node(object: (first: 1, second: 2,))")
        error.diagnostics.first().code shouldBe "rainbow.parser.unexpectedToken"
        error.diagnostics.first().message shouldBe
            "Trailing comma is not allowed after the last object entry."
    }

    test("parserRejectsTrailingCommaAfterLastParameter") {
        val error = expectDecodeError("Button(title: \"Entrar\",)")
        error.diagnostics.first().code shouldBe "rainbow.parser.unexpectedToken"
        error.diagnostics.first().message shouldBe
            "Trailing comma is not allowed after the last parameter."
    }

    test("parserReportsMissingParameterName") {
        expectDecodeError("Button(: \"Entrar\")").diagnostics.first().message shouldBe
            "Expected parameter name."
    }

    test("parserReportsMissingColon") {
        expectDecodeError("Button(title \"Entrar\")").diagnostics.first().message shouldBe
            "Expected ':' after parameter name."
    }

    test("parserReportsMissingValue") {
        expectDecodeError("Button(title: )").diagnostics.first().message shouldBe "Expected value."
    }

    test("parserRejectsBareEnumIdentifierWithoutDot") {
        val error = expectDecodeError("Button(variant: primary)")
        error.diagnostics.first().code shouldBe "rainbow.parser.unexpectedToken"
        error.diagnostics.first().message shouldBe
            "Enum identifier values must start with '.'; did you mean '.primary'?"
    }

    test("parserReportsMissingClosingParen") {
        expectDecodeError("Button(title: \"Entrar\"").diagnostics.first().message shouldBe
            "Expected ')' after parameter list."
    }

    test("parserReportsMissingClosingBlockBrace") {
        expectDecodeError("Screen { Button").diagnostics.first().message shouldBe
            "Expected '}' after block."
    }

    test("parserReportsMissingClosingArrayBracket") {
        expectDecodeError("Node(values: [.one, .two)").diagnostics.first().message shouldBe
            "Expected ']' after array."
    }

    test("parserReportsMissingObjectKeyAndClosingParen") {
        val messages = expectDecodeError("Node(meta: ( : true)").diagnostics.map { it.message }
        messages shouldContain "Expected object key."
        (
            messages.contains("Expected ')' after object.") ||
                messages.contains("Expected ')' after parameter list.")
            ) shouldBe true
    }

    test("parserReportsUnexpectedTokenAtDocumentRoot") {
        expectDecodeError("}").diagnostics.first().message shouldBe "Expected node name."
    }

    test("parserRecoversFromUnexpectedRootTokenBeforeNextNode") {
        expectDecodeError(") Screen").diagnostics.first().message shouldBe "Expected node name."
    }

    test("parserRecoversFromUnexpectedTokenInsideBlock") {
        expectDecodeError("Root { ) ( Child }").diagnostics.first().message shouldBe
            "Expected node name."
    }

    test("parserReportsInvalidArrayValueAndSynchronizesAtComma") {
        expectDecodeError("Node(values: [.one, , .two])").diagnostics.first().message shouldBe
            "Expected value."
    }

    test("parserReportsMissingObjectValueAndSynchronizesAtComma") {
        expectDecodeError("Node(meta: (id: , next: true))").diagnostics.first().message shouldBe
            "Expected value."
    }

    test("parserParameterSynchronizationCanReachEndOfFile") {
        val messages = expectDecodeError("Node(:").diagnostics.map { it.message }
        messages shouldContain "Expected parameter name."
        messages shouldContain "Expected ')' after parameter list."
    }

    test("parserDecodesTaggedEmbeddedValues") {
        val document = RainbowDecoder().decode(
            """
            Screen(
              json: @JSON({"key": "value"}),
              blob: @JSON(#{payload}),
              md: @MARKDOWN(# Title)
            )
            """.trimIndent(),
        )

        document.nodes[0].parameters.size shouldBe 3
        val json = document.nodes[0].parameters[0].value as RainbowValue.Tagged
        json.language shouldBe EmbeddedLanguage.JSON
        json.body shouldBe RainbowTaggedBody.Text("{\"key\": \"value\"}")

        val blob = document.nodes[0].parameters[1].value as RainbowValue.Tagged
        blob.body shouldBe RainbowTaggedBody.Placeholder("payload")

        val encoded = RainbowEncoder().encode(document)
        encoded shouldContain "@JSON({\"key\": \"value\"})"
        encoded shouldContain "@JSON(#{payload})"
        encoded shouldContain "@MARKDOWN("
    }
})
