# Conventions

## Escopo

- **Sim:** sintaxe válida? onde está o erro? qual a árvore genérica? como imprimir canonicamente?
- **Não:** validar se `Button` pode ter certo filho; registry; render nativo; runtime de eventos; **expand / furos `#{Name}`** (backend Rust).

Não coloque validação NShiftUI neste AAR. O payload mobile chega concreto.

## Regra do DSL

```text
() = parâmetros / configuração
{} = composição, filhos e fluxo
```

Parâmetros são nomeados: `Button(title: "Entrar", variant: .primary)`.

Não usar nós aninhados como valor de parâmetro nesta versão. Composição via blocks.

## Gramática concreta

A [especificação compartilhada](https://github.com/NShiftUI-OSS/RainbowParser-Rust/blob/main/spec/grammar.md) documenta os pins `use`, os modificadores, as listas de plugins e os corpos `@LANG`. Um nó com filhos e sem parâmetros é escrito `Screen { Spinner() }`. O parser preserva os comentários de linha associados ao próximo elemento.

## Código Kotlin

- Library Android, minSdk 23, compileSdk 37, Kotlin 2.4.20 (`org.jetbrains.kotlin.android`; `android.builtInKotlin=false` e `android.newDsl=false` porque o plugin não convive com o DSL novo do AGP 9), Gradle 9.7.1, daemon JDK 25, zero deps de runtime. Bytecode Java 21.
- Públicos: data classes / sealed types imutáveis; `RainbowNode.children`/`hasBlock` usam explicit backing fields.
- Erros de parse: `RainbowParseError`.
- Lexer/parser/printer/scanner: `internal`. Context parameters em `Graphemes.kt`; when-guards no lexer/parser.
- Inteiros: `Long`. Offsets: grapheme (cluster UAX-ish interno, sem `BreakIterator`), CRLF = 1.
- Preferir `./gradlew test` após mudanças (Kotest FunSpec, runner JUnit 5; `kotlinx-coroutines-core` só em teste).

## Encode

Saída canônica. Round-trip preserva **estrutura**, não formatação original.

## Commits / harness

Não commitar/push a menos que o usuário peça. Atualizar harness via `update-harness` quando a superfície mudar.
