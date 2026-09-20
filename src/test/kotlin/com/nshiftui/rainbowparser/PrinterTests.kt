package com.nshiftui.rainbowparser

import com.nshiftui.rainbowparser.ast.RainbowBlock
import com.nshiftui.rainbowparser.ast.RainbowDocument
import com.nshiftui.rainbowparser.ast.RainbowNode
import com.nshiftui.rainbowparser.ast.RainbowObjectEntry
import com.nshiftui.rainbowparser.ast.RainbowParameter
import com.nshiftui.rainbowparser.ast.RainbowUseDeclaration
import com.nshiftui.rainbowparser.ast.RainbowValue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class PrinterTests : FunSpec({
    test("printerEncodesUseDeclarations") {
        val document = RainbowDocument(
            uses = listOf(
                RainbowUseDeclaration(name = "Screen", version = "1.0.0"),
                RainbowUseDeclaration(name = "Button", version = "2.0.0"),
            ),
            nodes = listOf(
                RainbowNode(
                    name = "Screen",
                    parameters = listOf(
                        RainbowParameter(name = "name", value = RainbowValue.StringValue("Home")),
                    ),
                ),
            ),
        )

        RainbowEncoder().encode(document) shouldBe """
            use Screen@1.0.0
            use Button@2.0.0

            Screen(name: "Home")
        """.trimIndent()
    }

    test("printerEncodesCanonicalRainbow") {
        val document = RainbowDocument(
            nodes = listOf(
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
                                RainbowNode(name = "OnTap", block = RainbowBlock()),
                            ),
                        ),
                    ),
                ),
            ),
        )

        RainbowEncoder().encode(document) shouldBe """
            Screen(name: "Home") {
              Button(
                title: "Entrar",
                variant: .primary
              ) {
                OnTap {
                }
              }
            }
        """.trimIndent()
    }

    test("printerEncodesAllValueShapes") {
        val document = RainbowDocument(
            nodes = listOf(
                RainbowNode(
                    name = "Node",
                    parameters = listOf(
                        RainbowParameter(
                            name = "text",
                            value = RainbowValue.StringValue("quote: \" slash: \\ newline: \n"),
                        ),
                        RainbowParameter(
                            name = "control",
                            value = RainbowValue.StringValue("tab: \t return: \r null: \u0000"),
                        ),
                        RainbowParameter(name = "int", value = RainbowValue.IntValue(-2)),
                        RainbowParameter(name = "double", value = RainbowValue.DoubleValue(2.5)),
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
                                    RainbowObjectEntry(key = "", value = RainbowValue.IntValue(0)),
                                    RainbowObjectEntry(key = "1bad", value = RainbowValue.IntValue(1)),
                                    RainbowObjectEntry(key = "a-", value = RainbowValue.IntValue(2)),
                                    RainbowObjectEntry(key = "😀", value = RainbowValue.IntValue(3)),
                                    RainbowObjectEntry(key = "_id", value = RainbowValue.IntValue(4)),
                                    RainbowObjectEntry(key = "A1", value = RainbowValue.IntValue(5)),
                                    RainbowObjectEntry(key = "a😀", value = RainbowValue.IntValue(6)),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        RainbowEncoder().encode(document) shouldBe """
            Node(
              text: "quote: \" slash: \\ newline: \n",
              control: "tab: \t return: \r null: \0",
              int: -2,
              double: 2.5,
              enabled: true,
              disabled: false,
              variant: .primary,
              items: ["a", 1, null],
              meta: (id: "home", "dash-key": false, "": 0, "1bad": 1, "a-": 2, "😀": 3, _id: 4, A1: 5, "a😀": 6)
            )
        """.trimIndent()
    }

    test("printerUsesCustomFormatStyle") {
        val document = RainbowDocument(
            nodes = listOf(
                RainbowNode(
                    name = "Root",
                    children = listOf(RainbowNode(name = "Child")),
                ),
            ),
        )
        val style = RainbowFormatStyle(indentation = "    ", newline = "\r\n")
        RainbowEncoder(style).encode(document) shouldBe "Root {\r\n    Child\r\n}"
    }

    test("parserFacadeEncodesAndDecodes") {
        val parser = RainbowParser()
        val source = "Button(title: \"Entrar\") { OnTap {} }"
        val document = parser.decode(source)

        RainbowParserVersion.current shouldBe "0.1.0-beta.1"
        parser.encode(document) shouldBe """
            Button(title: "Entrar") {
              OnTap {
              }
            }
        """.trimIndent()
    }

    test("emptyDocumentEncodesToEmptyString") {
        RainbowEncoder().encode(RainbowDocument()) shouldBe ""
    }
})
