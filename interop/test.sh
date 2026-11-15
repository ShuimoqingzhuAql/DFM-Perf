#!/usr/bin/env bash
# Host-side end-to-end test of dfm_perf.sh against fixtures produced by the app's own ConfigCrypto.
# Usage (Git Bash / Linux): bash interop/test.sh
set -u
HERE=$(cd "$(dirname "$0")" && pwd)
ROOT="$HERE/run"
FIX="$HERE/fixture"
SCRIPT="$HERE/../dfm_perf.sh"
JAVA=${JAVA:-"/e/Software/Android Studio/jbr/bin/java.exe"}
JCP="$HERE/crypto"

chmod +x "$HERE/fakesu" 2>/dev/null

rm -rf "$ROOT"
D="$ROOT/data/files/UE4Game/DeltaForce/DeltaForce/Saved/Config/Android"
E="$ROOT/ext/files/UE4Game/DeltaForce/DeltaForce/Saved/Config/Android"
mkdir -p "$D" "$E"

cp "$FIX/Engine.ini" "$D/Engine.ini"
cp "$FIX/Game.ini" "$D/Game.ini"
cp "$FIX/UserGameplay.ini" "$E/UserGameplay.ini"
cp "$FIX/UserGameplay.ini" "$D/UserGameplay.ini"   # for the Java verifier

export DC_SU="$HERE/fakesu"
export DC_ENGINE="$D/Engine.ini"
export DC_GAME="$D/Game.ini"
export DC_GAMEPLAY="$E/UserGameplay.ini"
export DC_BACKUP_ROOT="$ROOT/backups"
export DC_MATERIAL_HEX="9f857c90d5d616b9542b49fbad60d70369dcd55ccb6497807baed636ab6c7e6d"

verify() { "$JAVA" -cp "$JCP" io.github.deltaconfig.core.Gen verify "$D" "$DC_MATERIAL_HEX"; }

echo "===== status (before) ====="
sh "$SCRIPT" status || { echo "STATUS FAILED"; exit 1; }

echo "===== set 终极 185 (dry run) ====="
sh "$SCRIPT" set 5 11

echo "===== set 终极 185 ratio 67 short 1080 -y ====="
sh "$SCRIPT" set 5 11 67 1080 -y

echo "===== status (after) ====="
sh "$SCRIPT" status

echo "===== java verify (app's own ConfigCrypto) ====="
verify

echo "===== game.ini pad line ====="
grep -o 'SmallPadConfig=(Level1Config=[^)]*)' "$D/Game.ini" | head -c 220; echo

echo "===== defaults -y ====="
sh "$SCRIPT" defaults -y
verify

echo "===== restore latest -y ====="
sh "$SCRIPT" list
sh "$SCRIPT" restore latest -y
verify

echo "===== tamper test: corrupt token then status must fail ====="
sed -i 's/^Token = .*/Token = AAAA/' "$D/Engine.ini"
sh "$SCRIPT" status && echo "UNEXPECTED SUCCESS" || echo "expected failure ok"
