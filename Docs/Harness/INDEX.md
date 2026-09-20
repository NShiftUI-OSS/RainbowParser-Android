# Harness Index — RainbowParser Android

Índice de progressive disclosure. Leia só o que a tarefa exige.

| Doc | Quando abrir |
|-----|----------------|
| [Architecture.md](./Architecture.md) | Pipeline Lexer→Parser→AST→Printer e pastas Kotlin |
| [Map.md](./Map.md) | Localizar arquivo por responsabilidade |
| [API.md](./API.md) | Superfície pública e contratos de decode/encode |
| [Conventions.md](./Conventions.md) | Regras de DSL, estilo e o que não fazer |
| [Decisions.md](./Decisions.md) | D8: furos `#{Name}` / expand são backend Rust; esta lib é Rainbow concreto |
| [Update.md](./Update.md) | Changelog do harness |

## Fontes de verdade

| Artefato | Papel |
|----------|--------|
| `build.gradle.kts` | Library Android, minSdk 23, compileSdk 37, jvmTarget 21, versão `0.1.0-beta.1`, zero deps de runtime |
| `src/main/kotlin/com/nshiftui/rainbowparser/` | Implementação |
| `src/test/kotlin/com/nshiftui/rainbowparser/` | Comportamento esperado (52 testes) |
| `README.md` | Documentação humana |
| `AGENTS.md` | Entrada curta para agentes |

## Atualização

Remapear com `Agents/UpdateHarness/SKILL.md` quando `src/`, Gradle ou README mudarem.
