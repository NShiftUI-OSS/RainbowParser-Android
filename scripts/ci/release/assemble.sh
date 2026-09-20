#!/usr/bin/env bash
set -euo pipefail
source "$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)/helpers.sh"
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/lib.sh"

ci_banner "job assemble"
cd "$REPO_ROOT"
ci_overlay_versioned_source
ci_prepare_android_sdk
[[ -x ./gradlew ]] || ci_die "./gradlew is missing or not executable"
log_step "./gradlew assembleRelease"
./gradlew assembleRelease --stacktrace

log_step "locate release AAR"
mkdir -p build/outputs/aar
AAR="$(find build/outputs/aar -name '*-release.aar' -print -quit || true)"
if [[ -z "$AAR" ]]; then
  log_error "no *-release.aar under build/outputs/aar"
  ls -la build/outputs/aar || true
  ci_die "assembleRelease did not produce an AAR"
fi
log_ok "AAR ${AAR}"
