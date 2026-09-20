# Map — arquivos

## Build

| Path | Notas |
|------|--------|
| `settings.gradle.kts` | Projeto `RainbowParser-Android`; Foojay resolver 1.0.0 (daemon toolchain) |
| `build.gradle.kts` | `com.android.library` 9.4.1, `org.jetbrains.kotlin.android` 2.4.20, Kotest 6.2.4, minSdk 23, compileSdk 37 (`minorApiLevel = 0`), jvmTarget 21, versão `0.1.0-beta.1` |
| `gradle/wrapper/` | Gradle 9.7.1 |
| `gradle/gradle-daemon-jvm.properties` | Daemon toolchain JDK 25 (JetBrains) |
| `gradle.properties` | configuration cache; `android.builtInKotlin=false`; `android.newDsl=false` |
| `README.md` | Docs humanas |
| `CODEOWNERS` | `* @arthur.porto` |
| `.github/workflows/release.yml` | Orquestra 8 jobs; SemVer com ou sem `v`; bump/commit só se input ≠ código; tag/Release existentes falham no validate |
| `.github/workflows/delete-release.yml` | Orquestra 3 jobs (input SemVer com ou sem `v`); falha se não existir Release nem tag |
| `.github/actions/setup-android-ci/` | Composite: Temurin 21 + Gradle cache (SDK no `lib.sh`) |
| `scripts/helpers.sh` | Cores ANSI, `log_*`, `ci_die` / trap ERR (debug local e Actions) |
| `scripts/ci/release/` | Um script por job: setup, validate, bump, test, assemble, commit, tag, release (+ `lib.sh` overlay/SDK) |
| `scripts/ci/delete-release/` | Um script por job: setup, delete-release, delete-tag |

## Sources (`src/main/kotlin/com/nshiftui/rainbowparser/`)

| Path | Tipo | Notas |
|------|------|--------|
| `RainbowParser.kt` | public | Facade decode/encode |
| `RainbowDecoder.kt` | public | Lexer → Parser |
| `RainbowEncoder.kt` | public | Printer |
| `RainbowParserVersion.kt` | public | `current = "0.1.0-beta.1"` |
| `RainbowFormatStyle.kt` | public | indent/newline |
| `ast/RainbowDocument.kt` | public | `uses`, `nodes` |
| `ast/RainbowNode.kt` | public | `name`, `parameters`, `block`; `children`/`hasBlock` (explicit backing fields) |
| `ast/RainbowBlock.kt` | public | `children` |
| `ast/RainbowParameter.kt` | public | `name` + `RainbowValue` |
| `ast/RainbowValue.kt` | public | string/int/double/bool/identifier/array/object/null/tagged `@LANG` |
| `ast/EmbeddedLanguage.kt` | public | JSON/YAML/XML/HTML/MARKDOWN + tagged body |
| `ast/RainbowObjectEntry.kt` | public | `key` + `value` |
| `lexer/RainbowLexer.kt` | internal | Scan de tokens |
| `lexer/RainbowToken.kt` | internal | Token + range |
| `lexer/RainbowTokenKind.kt` | internal | Kinds |
| `parser/RainbowSyntaxParser.kt` | internal | Parse + sync de erros |
| `printer/RainbowPrinter.kt` | internal | Encode canônico |
| `diagnostics/*` | public | Diagnostic, ParseError, Location, Range, Severity |
| `support/CharacterScanner.kt` | internal | Peek/advance por grapheme |
| `support/SourceText.kt` | internal | Buffer de source |
| `support/Graphemes.kt` | internal | Grapheme clusters (context parameter) + helpers |

## Tests (52)

| Arquivo | Foco | count |
|---------|------|-------|
| `ASTTests.kt` | init, block, error description | 3 |
| `LexerTests.kt` | tokens, escapes, CRLF, tagged, erros | 18 |
| `ParserTests.kt` | nested, values, trailing commas, diagnostics/sync | 22 |
| `PrinterTests.kt` | canônico, values, style, facade, empty | 6 |
| `RoundTripTests.kt` | estrutura, comments, concorrência (coroutines) | 3 |

## Harness

| Path | Papel |
|------|--------|
| `AGENTS.md` | Entrada curta |
| `Docs/Harness/` | Profundidade |
| `Agents/UpdateHarness/` | Skill canônica |
| `.cursor/rules/` | Regras sempre aplicadas |
