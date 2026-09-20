# Harness UPDATE log

## 2026-09-20

- Release e Delete: SemVer com ou sem `v`; delete falha se não houver Release nem tag (sem falso success).
- SDK CI: pacote real é `platforms;android-37.0` (não `android-37`); `compileSdk` usa `minorApiLevel = 0`.
- CI: YAML só orquestra; lógica em `scripts/ci/release/` e `scripts/ci/delete-release/` (um script por job). Cores/`ci_die` em `scripts/helpers.sh`. Removidos Python e overlay composite.
- GitHub Actions: `release.yml` (8 jobs) e `delete-release.yml` (3 jobs), `workflow_dispatch`, ator `ArthurPorto-PucMinas`.

## 2026-09-19

- Kotlin 2.4: when-guards (lexer/parser), explicit backing fields (`RainbowNode`), context parameters (`Graphemes`), `jvmTarget` 21, plugin `org.jetbrains.kotlin.android` com `android.builtInKotlin=false`, graphemes sem BreakIterator, coroutines no RoundTrip (test-only).
- Follow-up do bump: `compileSdk` 37 (teto AGP 9.4), configuration cache Gradle 9, `gradle/gradle-daemon-jvm.properties` + Foojay no Map.
- Toolchain: Gradle 9.7.1, AGP 9.4.1, Kotlin 2.4.20, Kotest 6.2.4. Gradle daemon aceita JDK 25.
- Testes migrados de JUnit 4 para Kotest FunSpec (runner JUnit 5).
- Biblioteca Kotlin implementada com paridade iOS: facades, AST, lexer, parser, printer, 52 testes Kotest.
- D8: furos `#{Name}` e expand continuam backend Rust; este AAR é Rainbow concreto.
- Gradle library minSdk 23 / compileSdk 37 / jvmTarget 21 / versão `0.1.0-beta.1`.
