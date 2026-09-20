package com.nshiftui.rainbowparser

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.util.Collections
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class RoundTripTests : FunSpec({
    test("canonicalRoundTripPreservesStructure") {
        val source = "Button( title:\"Entrar\", meta:(id:\"login\") ){OnTap{Navigate(to:\"Home\")}}"
        val parser = RainbowParser()

        val firstDocument = parser.decode(source)
        val encoded = parser.encode(firstDocument)
        val secondDocument = parser.decode(encoded)

        secondDocument shouldBe firstDocument
        encoded shouldBe """
            Button(
              title: "Entrar",
              meta: (id: "login")
            ) {
              OnTap {
                Navigate(to: "Home")
              }
            }
        """.trimIndent()
    }

    test("formatPreservesLeadingCommentsAndBlankSiblings") {
        val formatted = RainbowParser().encode(
            RainbowParser().decode(
                """
                use Screen@1.0.0
                // root comment
                Screen {
                  Text(text: "a")
                  // between plugins
                  Button(title: "b")
                }
                """.trimIndent(),
            ),
        )

        formatted shouldContain "use Screen@1.0.0\n\n// root comment\nScreen {"
        formatted shouldContain "Text(text: \"a\")\n\n  // between plugins\n  Button(title: \"b\")"
    }

    test("parserCanBeUsedConcurrently") {
        val parser = RainbowParser()
        val source = """
            Screen(name: "Home") {
              Button(title: "Entrar") {
                OnTap {
                  Navigate(to: "Dashboard")
                }
              }
            }
        """.trimIndent()
        val expected = parser.decode(source)
        val errors = Collections.synchronizedList(mutableListOf<Throwable>())
        runBlocking {
            coroutineScope {
                repeat(64) {
                    launch {
                        try {
                            parser.decode(source) shouldBe expected
                        } catch (error: Throwable) {
                            errors.add(error)
                        }
                    }
                }
            }
        }
        errors.shouldBeEmpty()
    }
})
