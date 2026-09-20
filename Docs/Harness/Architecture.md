# Architecture

## Papel

`RainbowParser` (pacote `com.nshiftui.rainbowparser`) é a camada de **sintaxe** do DSL Rainbow no Android. Converte texto ↔ AST genérica. Não conhece componentes, triggers ou actions do NShiftUI.

## Pipeline

```text
String (Rainbow)
  → RainbowLexer          (support/CharacterScanner + SourceText)
  → [RainbowToken]        (lexer/RainbowToken + RainbowTokenKind)
  → RainbowSyntaxParser
  → RainbowDocument       (ast)
  → RainbowPrinter        (printer + RainbowFormatStyle)
  → String (canônico)
```

Facades públicas:

- `RainbowDecoder.decode` — Lexer + SyntaxParser
- `RainbowEncoder.encode` — Printer
- `RainbowParser` — decode + encode

## Módulos (`src/main/kotlin/com/nshiftui/rainbowparser/`)

| Pasta | Responsabilidade |
|-------|------------------|
| raiz | Facades, versão, `RainbowFormatStyle` |
| `lexer/` | Tokens a partir do source (interno) |
| `parser/` | Tokens → `RainbowDocument` (interno) |
| `ast/` | Modelos sintáticos imutáveis |
| `printer/` | AST → texto canônico (interno) |
| `diagnostics/` | Erros, ranges, severidade |
| `support/` | Scanner, graphemes e texto (interno) |

Offsets de source seguem **grapheme clusters** (equivalente a `Character` do Swift), inclusive CRLF como uma unidade — implementação em `Graphemes.kt`, sem `java.text.BreakIterator`. Inteiros da AST são `Long` (Swift `Int` 64-bit).

## Concorrência

Data classes imutáveis. Facades não guardam estado mutável compartilhado; cada decode cria lexer/parser novos.

## Integração pretendida

```text
Rainbow source → RainbowParser → RainbowDocument
  → (NShiftUI semantic layer) → domain tree → render
```

## Rainbow concreto (mobile)

```text
template .rbw  →  rainbowparser-rust expand  →  Rainbow concreto  →  iOS / Android
```

Furos `#{Name}` e a API de expand **não** pertencem a este AAR (D8 em Decisions.md).
