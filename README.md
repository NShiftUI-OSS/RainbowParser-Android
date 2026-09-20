# RainbowParser (Android)

Biblioteca Kotlin de **sintaxe** do DSL Rainbow. Alvo: **minSdk API 23** (Android 6.0), versão `0.1.0-beta.1`. Pacote `com.nshiftui.rainbowparser`. Zero dependências de runtime.

Não valida semântica NShiftUI (`Screen`, `Button`, `OnTap`, etc.). Isso vive em NShiftUI.

## Rainbow concreto

iOS e Android parseiam **Rainbow já expandido**. Furos `#{Name}` e `expand` pertencem só a **RainbowParser-Rust** (API global do backend).

```text
template .rbw  →  Rust expand  →  concrete Rainbow  →  this library
```

Não implemente substituição de templates neste AAR. Detalhes: `AGENTS.md` e `Docs/Harness/Decisions.md` (D8).

## Pipeline

```text
String → RainbowLexer → [RainbowToken] → RainbowSyntaxParser
      → RainbowDocument → RainbowPrinter → String
```

`()` = parâmetros; `{}` = filhos / fluxo. Enums na fonte usam `.name`; a AST guarda o identificador sem o ponto.

## API pública

```kotlin
val parser = RainbowParser()
val document = parser.decode(source)
val encoded = parser.encode(document)

val document2 = RainbowDecoder().decode(source)
val encoded2 = RainbowEncoder().encode(document2)
```

Versão: `RainbowParserVersion.current` → `"0.1.0-beta.1"`.

Erros de parse: `RainbowParseError` com `diagnostics`.

## Paridade

- API pública: mesmo papel da lib iOS (`decode` / `encode`, AST, diagnostics, `RainbowFormatStyle`).
- Gramática concreta: Rust `spec/grammar.md` (nós, `.enum`, `use Name@version`, `@LANG` com payload real).
- Sem CLI, LSP, NDK/JNI, registry ou validate semântico.
- `Int` do iOS (64-bit) vira `Long` no Kotlin.

## Build

Requer Android SDK (`ANDROID_HOME` ou `local.properties` com `sdk.dir`). Toolchain: Gradle 9.7.1, AGP 9.4.1, Kotlin 2.4.20 (`org.jetbrains.kotlin.android`), `compileSdk` 37, bytecode Java 21. O daemon usa JDK 25 (`gradle/gradle-daemon-jvm.properties`). Zero dependências de runtime; `kotlinx-coroutines-core` só nos testes.

```bash
./gradlew test
./gradlew assemble
```

Release: Actions → **Release** (branch + SemVer com ou sem `v`). Apagar: **Delete release** (SemVer com ou sem `v`). Só `ArthurPorto-PucMinas`.

Se o SemVer do input já está no código e **ainda não** existe tag/Release, bump e commit são pulados e o AAR é publicado na versão atual. Se a tag ou o GitHub Release já existem, o validate falha.

Os YAML em `.github/workflows/` só orquestram. A lógica de cada job está em `scripts/ci/release/` e `scripts/ci/delete-release/`. Cores e falhas com contexto: `scripts/helpers.sh`. Para reproduzir um job localmente:

```bash
ALLOWED_ACTOR=ArthurPorto-PucMinas GITHUB_ACTOR=ArthurPorto-PucMinas \
  VERSION_INPUT=0.1.0-beta.1 BRANCH_INPUT=main \
  bash scripts/ci/release/setup.sh
```

52 testes Kotest FunSpec (AST, Lexer, Parser, Printer, RoundTrip), espelhando o iOS.
