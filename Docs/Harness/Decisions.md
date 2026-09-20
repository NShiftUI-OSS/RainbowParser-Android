# Decisions

## D8 — Placeholders `#{Name}` e expand são backend (Rust)

**Decisão:** esta biblioteca parseia **Rainbow concreto**. Substituição de furos `#{Name}` (`use #{Name}`, nome de nó, valor de parâmetro, `@LANG(#{Name})`) e a API `expand` ficam **exclusivas** de `RainbowParser-Rust`, a API global do backend.

**Por quê:** o frontend (iOS / Android) recebe payload já expandido. Furos de template não são runtime mobile.

**Consequência:**

- Não adicionar `RainbowName`, token de placeholder em use/nó/valor, nem API de expand.
- Gramática: `use Name@MAJOR.MINOR.PATCH`, nomes de nó identificadores, enum `.name`, `@LANG(...)` com body de linguagem real.
- O lexer aceita `@LANG(#{Name})` só para paridade de token com o iOS; não há API `expand` neste AAR.
- Testes golden futuros: `fixtures/valid` e invalid de parse (ex. enum bare). Não portar `fixtures/templates` nem `expand`.

Numeração D8 alinha com `RainbowParser-iOS` / `docs/PARITY.md` do Rust.
