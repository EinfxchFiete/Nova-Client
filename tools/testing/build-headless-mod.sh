#!/usr/bin/env bash
# Builds a tiny TEST-ONLY mod that lets Minecraft 26.3 open a window on Xvfb (no sRGB framebuffer there).
# Needed by launcher/test/launch-test.js and launcher/test/ui-launch-test.js (pass the jar path as argument).
# Usage (repo root, after ./gradlew build so build/classes/java/gametest exists):
#   tools/testing/build-headless-mod.sh /some/dir/headless-test-mod.jar
# Never put this jar into a real mods folder.
set -euo pipefail
OUT="${1:?output jar path}"
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
TMP="$(mktemp -d)"
mkdir -p "$TMP/dev/novaclient/test/mixin"
cp "$ROOT/build/classes/java/gametest/dev/novaclient/test/mixin/HeadlessGlBackendMixin.class" "$TMP/dev/novaclient/test/mixin/"
cat > "$TMP/fabric.mod.json" <<'JSON'
{"schemaVersion":1,"id":"nova-headless-test","version":"1.0.0","environment":"client","mixins":["nova-headless.mixins.json"]}
JSON
cat > "$TMP/nova-headless.mixins.json" <<'JSON'
{"required":true,"package":"dev.novaclient.test.mixin","compatibilityLevel":"JAVA_25","client":["HeadlessGlBackendMixin"],"injectors":{"defaultRequire":1}}
JSON
(cd "$TMP" && jar cf "$OUT" .)
rm -rf "$TMP"
echo "built $OUT"
