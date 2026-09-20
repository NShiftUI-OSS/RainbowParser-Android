---
name: update-harness
description: Remaps this repository's AI harness (AGENTS.md + Docs/Harness) from the live codebase. Use after architecture/API/module changes, or when the user asks to update/refresh/remap the harness.
---

# update-harness

Remapeia o harness de agentes a partir do código vivo desta library Android. Use quando `src/`, Gradle ou `README.md` mudarem — ou quando o usuário pedir para atualizar o harness.

## Objetivo

Manter `AGENTS.md` + `Docs/Harness/` alinhados à verdade do repositório, com progressive disclosure e baixo custo de tokens.

## Escopo

**Escrever apenas** harness (`AGENTS.md`, `Docs/Harness/`, bridges). **Não** alterar lógica de `src/` salvo pedido explícito. **Não** commit/push a menos que o usuário peça.

## Fontes de verdade

1. `build.gradle.kts` / `settings.gradle.kts` — minSdk, versão, deps
2. `src/main/kotlin/com/nshiftui/rainbowparser/**` — API pública vs `internal`
3. `src/test/kotlin/**` — contagem e focos de teste
4. `README.md` — versão, regras DSL
5. `RainbowParserVersion.current`

## Passos

1. Inventariar arquivos, tipos públicos, testes `@Test`/`fun`, versão, deps (zero runtime).
2. Atualizar `Docs/Harness/` (INDEX, Architecture, Map, API, Conventions, Decisions se mudou, Update.md).
3. Manter `AGENTS.md` curto (≤120 linhas). Enfatizar Rainbow concreto e D8.
4. Preservar `CLAUDE.md` → `AGENTS.md` (symlink) e `.cursor/rules/concrete-rainbow.mdc`.
5. Verificar contagens/versão/API contra o código.
6. Append em `Docs/Harness/Update.md`.

## Estilo

Prosa em português; identificadores em inglês. Tabelas curtas; links relativos.
