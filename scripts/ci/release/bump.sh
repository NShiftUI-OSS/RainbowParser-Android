#!/usr/bin/env bash
set -euo pipefail
source "$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)/helpers.sh"

ci_banner "job bump"
require_env VERSION
cd "$REPO_ROOT"

VERSION_KT="src/main/kotlin/com/nshiftui/rainbowparser/RainbowParserVersion.kt"
BUILD_GRADLE="build.gradle.kts"
PRINTER_TESTS="src/test/kotlin/com/nshiftui/rainbowparser/PrinterTests.kt"

log_step "patch working tree (no commit)"
ci_sed_inplace "s/const val current = \"[^\"]+\"/const val current = \"${VERSION}\"/" "$VERSION_KT"
ci_sed_inplace "s/^version = \"[^\"]+\"/version = \"${VERSION}\"/" "$BUILD_GRADLE"
ci_sed_inplace "s/RainbowParserVersion.current shouldBe \"[^\"]+\"/RainbowParserVersion.current shouldBe \"${VERSION}\"/" "$PRINTER_TESTS"
grep -F "const val current = \"${VERSION}\"" "$VERSION_KT" >/dev/null \
  || ci_die "failed to patch ${VERSION_KT}"
grep -F "version = \"${VERSION}\"" "$BUILD_GRADLE" >/dev/null \
  || ci_die "failed to patch ${BUILD_GRADLE}"
grep -F "RainbowParserVersion.current shouldBe \"${VERSION}\"" "$PRINTER_TESTS" >/dev/null \
  || ci_die "failed to patch ${PRINTER_TESTS}"
log_ok "patched ${VERSION_KT}"
log_ok "patched ${BUILD_GRADLE}"
log_ok "patched ${PRINTER_TESTS}"

log_step "stage artifact files"
mkdir -p .ci-version
cp "$VERSION_KT" .ci-version/RainbowParserVersion.kt
cp "$BUILD_GRADLE" .ci-version/build.gradle.kts
cp "$PRINTER_TESTS" .ci-version/PrinterTests.kt
log_ok "artifact dir .ci-version ready"
