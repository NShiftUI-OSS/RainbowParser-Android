# API pública

Pacote `com.nshiftui.rainbowparser` (e subpacotes `ast`, `diagnostics`). Decode lança `RainbowParseError`.

## Facades

```kotlin
val parser = RainbowParser()
val document = parser.decode(source)
val encoded = parser.encode(document)
val encoded2 = parser.encode(document, RainbowFormatStyle.DEFAULT)

val document2 = RainbowDecoder().decode(source)
val encoded3 = RainbowEncoder().encode(document2)
val encoded4 = RainbowEncoder(
    RainbowFormatStyle(indentation = "\t")
).encode(document2)
```

Versão: `RainbowParserVersion.current` → `"0.1.0-beta.1"`.

## AST

| Tipo | Campos / casos |
|------|----------------|
| `RainbowDocument` | `uses`, `nodes` |
| `RainbowUseDeclaration` | `name`, `version` — `use Screen@1.0.0` |
| `RainbowNode` | `name`, `parameters`, `block?`; computed `children`, `hasBlock` |
| `RainbowBlock` | `children` |
| `RainbowParameter` | `name`, `value` |
| `EmbeddedLanguage` | `JSON` / `YAML` / `XML` / `HTML` / `MARKDOWN` |
| `RainbowTaggedBody` | `Text(String)` \| `Placeholder(String)` |
| `RainbowObjectEntry` | `key`, `value` |
| `RainbowValue` | `StringValue`, `IntValue(Long)`, `DoubleValue`, `BoolValue`, `Identifier` (fonte `.name`, AST sem o ponto), `ArrayValue`, `ObjectValue`, `Null`, `Tagged` |

`RainbowNode` mantém `block` separado de `children` para o printer distinguir `Navigate(to: "Home")` de `OnTap {}`.

## Diagnostics

| Tipo | Papel |
|------|--------|
| `RainbowParseError` | `diagnostics`; `description` |
| `RainbowDiagnostic` | `code`, `message`, `severity`, `range` |
| `RainbowDiagnosticSeverity` | `ERROR`, `WARNING` |
| `RainbowSourceLocation` | line/column (`Comparable` por offset) |
| `RainbowSourceRange` | start/end |

```kotlin
try {
    RainbowDecoder().decode(source)
} catch (error: RainbowParseError) {
    error.diagnostics.forEach { println(it.description) }
}
```

## Format

`RainbowFormatStyle`: `indentation` (default `"  "`), `newline` (default `"\n"`).
Encode força blank entre irmãos e preserva leading `//` em `use` / nós / parâmetros.

## Interno (não público)

`RainbowLexer`, `RainbowToken`, `RainbowTokenKind`, `RainbowSyntaxParser`, `RainbowPrinter`, `CharacterScanner`, `SourceText`.
