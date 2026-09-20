# RainbowParser-Android — Agent Harness

Biblioteca Kotlin de **sintaxe** do DSL Rainbow para Android (minSdk API 23 / Android 6.0). Versão `0.1.0-beta.1`. Pacote `com.nshiftui.rainbowparser`. Zero deps de runtime.

**Não** valida semântica NShiftUI. Parser **mobile**, mesmo contrato da lib iOS.

```text
template .rbw  →  RainbowParser-Rust expand  →  Rainbow concreto  →  iOS / Android
```

## Ordem de leitura

1. Este arquivo (`AGENTS.md`) — identidade e limites
2. `Docs/Harness/INDEX.md` — mapa do harness
3. Sob demanda: `Architecture`, `Map`, `API`, `Conventions`, `Decisions`
4. Código: `src/main/kotlin/`, `src/test/kotlin/`, `build.gradle.kts`, `README.md`

## Placeholders (obrigatório)

Furos `#{Name}` e a API de expand são **exclusivos do backend Rust**.

- Não adicione `RainbowName`, token de placeholder em `use` / nome de nó / valor, nem `expand()`.
- Aceite só Rainbow concreto: `use Name@MAJOR.MINOR.PATCH`, identificadores de nó, enum `.name` (fonte com ponto, AST sem ponto), `@LANG(...)` com body real.
- Substituição em runtime é problema do backend, não deste AAR.

## Pipeline

```text
String → Lexer → Tokens → Parser → RainbowDocument → Printer → String
```

`()` = parâmetros; `{}` = filhos / fluxo.

## API pública (resumo)

`RainbowParser`, `RainbowDecoder`, `RainbowEncoder`, `RainbowParserVersion`,
AST (`RainbowDocument`, `RainbowNode`, `RainbowBlock`, `RainbowParameter`,
`RainbowValue`, `RainbowObjectEntry`, `RainbowUseDeclaration`), diagnostics,
`RainbowFormatStyle`.

Detalhes: `Docs/Harness/API.md`.

## Comandos

```bash
./gradlew test
./gradlew assemble
```

52 testes Kotest (AST, Lexer, Parser, Printer, RoundTrip).

## Princípios

1. Sintaxe apenas — sem registry de componentes/triggers/actions.
2. AST genérica; encode canônico (não lossless).
3. Parse falha com `RainbowParseError`.
4. Não alterar escopo beta: sem macros, expressões, condicionais, loops.
5. Preferir `./gradlew test` após mudanças de sintaxe.
6. Rainbow **concreto** apenas. Furos `#{Name}` e expand ficam no crate Rust (backend). Ver D8.

## Atualizar o harness

Skill: `Agents/UpdateHarness/SKILL.md`

Após mudanças em `src/`, Gradle ou README, rode o agent `update-harness`.

## Cross-tool

| Tool        | Entrada                         |
|-------------|---------------------------------|
| Cursor      | `AGENTS.md` + `.cursor/rules/`  |
| Claude Code | `CLAUDE.md` → `AGENTS.md`       |
| Codex       | `AGENTS.md` + `.agents/skills/` |

Overrides locais (gitignored): `AGENTS.local.md`, `CLAUDE.local.md`.
