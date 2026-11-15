# Independent RE pass — `io.github.deltaconfig` (DeltaConfig) on base.apk

All facts below were derived in this pass from a fresh decompile, **not** from any prior analysis.

Artifacts produced by this pass (only files under `E:\三角洲行动\work\out\`):

- jadx (primary): `E:\三角洲行动\work\out\indep_jadx\sources\...` — command
  `jadx.bat -j 8 --no-res -d work\out\indep_jadx base.apk` (JAVA_HOME = `E:\Software\Android Studio\jbr`)
- jadx (fallback mode, used only for the 3 methods where the primary pass emitted
  `Method not decompiled`): `work\out\indep_jadx_fb\...` (ConfigPatcher) and
  `work\out\indep_jadx_fb2\...` (LibraryMaskResolver)
- baksmali was **not** usable: `work\tools\baksmali.jar` has no `Main-Class` and its
  `org.jf.baksmali.Main` needs `org/jf/util/jcommander/Command`, which is not shipped
  (`NoClassDefFoundError`). `work\out\indep_smali` is therefore empty. The fallback jadx
  register-level output was used instead as the "smali-equivalent" evidence.

Citation notation: `Class#method — File.java:line`. Unless a path is given, files live under
`work\out\indep_jadx\sources\io\github\deltaconfig\`. `…_fb2\…` means
`work\out\indep_jadx_fb2\sources\io\github\deltaconfig\core\`.

Only 13 classes exist in the dex (verified by the jadx output listing): `MainActivity`,
`GameRepository`, `GameTarget`, `RootShell`, `R`, `AppBackgroundDrawable`,
2 synthetic lambdas, and `core/{ConfigCompatibility, ConfigCrypto, ConfigPatcher,
LibraryMaskResolver, OperationFailure}`.

---

## 1. Package constants and supported distributions

| Fact | Value | Evidence |
|---|---|---|
| CN package constant | `com.tencent.tmgp.dfm` | `GameRepository.PACKAGE` — GameRepository.java:41 |
| Level Infinite (global) | `com.proxima.dfm` | `GameTarget.GLOBAL` — GameTarget.java:9 |
| Garena (global) | `com.garena.game.df` | `GameTarget.GARENA` — GameTarget.java:10 |
| CN target (label 国服（腾讯）, `international=false`) | built from `GameRepository.PACKAGE` | `GameTarget.CN` — GameTarget.java:8 |
| Global labels | `国际服（Level Infinite）`, `国际服（Garena）`, `international=true` | GameTarget.java:9-10 |
| Supported set | `{CN, GLOBAL, GARENA}` | `GameTarget.TARGETS` — GameTarget.java:11 |
| Lookup | exact `packageName.equals`, else `IllegalArgumentException` | `GameTarget#lookup` — GameTarget.java:23-30 |
| Default selection | `CN` when the stored package is unknown / 0 or ≥2 installed | `GameTarget#initialSelection` — GameTarget.java:32-52 |

`GameTarget.values()` returns a clone (GameTarget.java:19-21), so the array is effectively immutable.

Cross-distribution writes are refused: `GameRepository#verifySnapshotTarget` (GameRepository.java:644-649)
and `#verifyInstalledTarget` (GameRepository.java:652-657).

---

## 2. Quality tiers and FPS tiers

`ConfigPatcher.QUALITY_LABELS` / `QUALITY_VALUES` — ConfigPatcher.java:16-17
`ConfigPatcher.FPS_LABELS` / `FPS_VALUES` — ConfigPatcher.java:18-19

| Index | Quality label | Quality int (written to `DFMQualityLevel`) |
|---|---|---|
| 0 | 流畅 | 0 |
| 1 | 标准 | 1 |
| 2 | 精致 | 2 |
| 3 | 高清 | 3 |
| 4 | 极致 | 4 |
| 5 | 终极 | 5 |

| Index | FPS label | Internal enum (written to `DFMFPSLevel`) | Real fps |
|---|---|---|---|
| 0 | 60 帧 | 2 | 60 |
| 1 | 90 帧 | 7 | 90 |
| 2 | 120 帧 | 8 | 120 |
| 3 | 144 帧 | 9 | 144 |
| 4 | 165 帧 | 10 | 165 |
| 5 | 185 帧 | 11 | 185 |

Internal-enum → real-fps mapping: `ConfigPatcher#fpsForInternal` — ConfigPatcher.java:183-204.
`2→60`, `7→90`, `8→120`, `9→144`, `10→165`, `11→185`; **everything else (incl. 0,1,3,4,5,6)
returns `-1`**, which `#plan` treats as "unverified tier" and `ConfigCompatibility#validate`
treats as "unrecognised enum" (ConfigCompatibility.java:51-53).

UI wiring: spinner item 0 is the placeholder (`请选择目标画质` / `请选择目标帧率`) and every
index is shifted by `RISK_NOTICE_VERSION == 1`
(`MainActivity#spinner` — MainActivity.java:71, 1450-1455; selection use
`ConfigPatcher.QUALITY_VALUES[pos - 1]` — MainActivity.java:768-769).
`ConfigPatcher#qualityLabel` returns `未知档位` outside 0..5 (ConfigPatcher.java:206-208).

---

## 3. Exact INI fields written

### 3.1 Engine.ini (encrypted container — see §6)

`ConfigPatcher#plan` — ConfigPatcher.java:63-68:

| Section | Key | Value | Evidence |
|---|---|---|---|
| `[DFMPerfGearConfig]` | `DFMQualityLevel` | decimal quality int (`0`–`5`) | ConfigPatcher.java:63 |
| `[DFMPerfGearConfig]` | `DFMFPSLevel` | decimal internal enum (`2,7,8,9,10,11`) | ConfigPatcher.java:63 |
| `[QualityFPSSetting]` | `Unlimit` | `1` (always) | ConfigPatcher.java:63 |
| `[SystemSettings]` | `PerfGear.SmallPadSize` | `0` (always) | ConfigPatcher.java:68 |
| `[SystemSettings]` | `DFMDeviceFrame.White<N>fps` | `1`, **only when real fps ≥ 90**, `<N>` ∈ {90,120,144,165,185} | ConfigPatcher.java:64-66 |

So `DFMDeviceFrame.White90fps`, `...White120fps`, `...White144fps`, `...White165fps`,
`...White185fps` — exactly the set whitelisted in
`ConfigCompatibility.SYSTEM_KEYS` (ConfigCompatibility.java:29).
Section/key matching is case-insensitive; missing sections are appended, duplicate
sections/keys throw (`ConfigPatcher#setField` — ConfigPatcher.java:232-294).

### 3.2 Game.ini (plaintext UTF-8, BOM preserved)

Section constant: `ConfigPatcher.PAD_SECTION =
"/Script/PerfGear.PerfGearAndroidPadSizeConfig"` — ConfigPatcher.java:15,
applied at ConfigPatcher.java:96-100.

Keys: `SmallPadConfig`, `MidPadConfig`, `BigPadConfig` — **all three receive the identical
string**. Value grammar (ConfigPatcher.java:73-95):

```
(Level1Config=(BackBuffer=8192,UltraLowResolution=R,LowResolution=R,MidResolution=R,
 HighResolution=R,UltraHighResolution=R,SuperResolution=R),Level2Config=(…same…),
 Level3Config=(…),Level4Config=(…),Level5Config=(…))
```

- `BackBuffer=8192` — literal constant, first field of every level group (ConfigPatcher.java:74).
- The 6 resolution keys, in this exact order: `UltraLowResolution`, `LowResolution`,
  `MidResolution`, `HighResolution`, `UltraHighResolution`, `SuperResolution`
  (array at ConfigPatcher.java:73, appended at :81).
- `R` = `max`: if render ratio == 100 → **8192** (unconditionally, independent of the user's
  short-edge value); otherwise `max(1, round(shortEdge * ratio / 100.0))`
  (ConfigPatcher.java:72). Ratio is validated 25..100 (`:48-50`), short edge 1..8192
  (`:51-53`); the UI restricts the EditText to 200..8192 (MainActivity.java:760-763).
- All five `LevelNConfig` groups share the same `R` — one render target for every tier.
- Whitelists matching this grammar: `ConfigCompatibility.RESOLUTION_KEYS`
  (ConfigCompatibility.java:31) and the `PadReader` validator
  (ConfigCompatibility.java:236-268) which requires exactly 5 level groups and 7 keys each,
  values 1..8192.

The terminal `终极` (quality 5) case only adds a warning sentence to the summary
(ConfigPatcher.java:110); it does **not** write a separate field.

---

## 4. On-disk config directory, relative path, file names

| Constant | Value | Evidence |
|---|---|---|
| `RELATIVE` | `/UE4Game/DeltaForce/DeltaForce/Saved/Config/Android/` | GameRepository.java:42 |
| private config dir | `<ApplicationInfo.dataDir> + "/files" + RELATIVE` | `GameRepository#privateConfigDirectory` — GameRepository.java:675-681, used at :701 |
| `ROOT` | `/data/adb/deltaconfig` | GameRepository.java:43 |
| backup base | `/data/adb/deltaconfig/backups/<pkg>/<userId>` (CN: `/data/adb/deltaconfig/backups/<userId>`) | `GameRepository#backupBase` — GameRepository.java:659-665 |
| backup parent dirs | `{/data/adb/deltaconfig, /data/adb/deltaconfig/backups, [backups/<pkg>], backups/<pkg>/<userId>}` | `GameRepository#backupDirectories` — GameRepository.java:667-673 |
| userId | `Process.myUid() / 100000` | GameRepository.java:70 |

Resolved paths (`GameRepository#paths` — GameRepository.java:691-742); all are canonicalised
with `readlink -f` and rejected if the pre-canonical path is a symlink
(`#canonical` — GameRepository.java:744-752, `#regular` — :754-756):

- `engine` = `<dataDir>/files/UE4Game/DeltaForce/DeltaForce/Saved/Config/Android/Engine.ini`
- `game`   = `<dataDir>/files/UE4Game/DeltaForce/DeltaForce/Saved/Config/Android/Game.ini`
- `gameplay` = `UserGameplay.ini`, tried as
  `/storage/emulated/<userId>/Android/data/<pkg>/files<RELATIVE>UserGameplay.ini`
  then as `/data/media/<userId>/Android/data/<pkg>/files<RELATIVE>UserGameplay.ini`
  (`GameRepository#gameplayPath` — GameRepository.java:683-689, call sites :707-719)

File names used (all under the backup transaction directory `base/<txId>/`):

| Name | Purpose | Evidence |
|---|---|---|
| `Engine.ini`, `Game.ini`, `UserGameplay.ini` | original-file backups (`cp -p`, then `chown 0:0`, `chmod 600`) | `#backupOne` — GameRepository.java:446-450; :350-352 |
| `manifest.json` | backup manifest, format 2 | GameRepository.java:348-353, `#manifest` :575-587 |
| `state` | transaction state: `PREPARED\n` / `COMMITTED\n` / `ROLLED_BACK\n` | :358, :392-398, :527 |
| `state.commit-<uuid>` | staged COMMITTED marker before rename | :391-394 |
| `pending` | pointer to in-flight transaction id | :359, `#writePointer` :1263-1269 |
| `latest` | pointer to last committed backup id | :403, :307, :482 |
| `pending.tmp`, `latest.tmp` | pointer staging files | :1265 |
| `<file>.deltaconfig-<txId>.new` / `.old` / `.rollback` | per-file staging copies | `#temporary` — GameRepository.java:1470-1472 |
| `<path>.write-<uuid>` | generic atomic-write temp (`#writeRoot`) | :1272 |

txId format: `yyyyMMdd_HHmmss_SSS-<UUID>`, validated on read by
`[0-9]{8}_[0-9]{6}_[0-9]{3}-[a-f0-9-]{36}` (`#pointer` — GameRepository.java:1250-1261,
created at :341).

`UserGameplay.ini` and the game APK are never modified — the only file that is read back and
asserted unchanged is `UserGameplay.ini` (GameRepository.java:382, :386).

---

## 5. How the 32-byte authentication material is found inside `libUE4.so`

Entry point: `GameRepository#resolveNativeMask` (GameRepository.java:772-812) for
`<nativeLibraryDir>/libUE4.so`, fallback `#resolveApkMask` (GameRepository.java:814-841) for the
`lib/arm64-v8a/libUE4.so` entry inside the game APK; both funnel into
`GameRepository#resolveMask` (GameRepository.java:843-918) →
`LibraryMaskResolver#resolve` (LibraryMaskResolver.java:58-79) →
`#resolveWithHash` (LibraryMaskResolver.java:82-180). Prerequisite header check:
`GameRepository#validateArm64Header` (GameRepository.java:1021-1026) requires a 64-byte
little-endian ELF64 with `e_machine == 0xB7` (AArch64).

### 5.1 Hardcoded legacy offsets (tried first)

`LibraryMaskResolver.LEGACY_FIRST = 24240560`, `LEGACY_SECOND = 24385264`
(LibraryMaskResolver.java:20-21). If the file is long enough, read **16 bytes at each offset**
and concatenate first-then-second (`#join` — LibraryMaskResolver.java:424-429) into 32 bytes
(LibraryMaskResolver.java:88-101). The result is labelled `已认证的兼容区段` and is only tried on
the full-read path (`resolve(...,true)`); `resolveByCode` skips it
(LibraryMaskResolver.java:58-64, :88). The two 16-byte halves are **not** XOR-masked at this
level — masking happens once per candidate encoding in `#authenticate` (§5.3).

### 5.2 Dynamic discovery (ARM64 instruction relocation scan)

`Elf#parse` (…fb2\LibraryMaskResolver.java:85-308) parses program headers
(`e_phoff`@32, `e_phentsize`@54 in 56..256, `e_phnum`@56 in 1..1024) into PT_LOAD regions
`(p_offset, p_vaddr, p_filesz, p_flags)` — `p_filesz ≤ p_memsz` enforced — and section headers
(`e_shoff`@40, `e_shentsize`@58, `e_shnum`@60 ≤ 1024). At least one executable, non-empty LOAD
region is required.

`Elf#prepareCode` (LibraryMaskResolver.java:549-592) builds the scan set: PROGBITS sections
(`sh_type==1`) with `SHF_EXECINSTR` (`sh_flags & 4`), each verified to map onto an executable
PT_LOAD; if no usable section table, all executable PT_LOADs are used. Regions are sorted by file
offset and must not overlap. Total scan ≤ 512 MiB (`MAX_SCAN_BYTES`, :23, :303-307).

`#discover` (…fb2\LibraryMaskResolver.java:1063-1218 / LibraryMaskResolver.java:295-361) walks each
executable region in 1 MiB blocks with a 28-byte overlap, 4-byte aligned, over 32-byte windows:

1. Take `w = int32LE(window + 16)`. Accept when
   `(w ^ (0x01010101 * ((w >>> 24) ^ 82))) & 0xFFFFFFE0 == 0x52800020`
   — literal `16843009` at …fb2:1142-1146, constant `1384120352` (=`0x52800020`) at :1146.
   *Note:* the primary jadx pass renders the literal `16843009` as `R.attr.cacheColorHint`
   (LibraryMaskResolver.java:325) — that is a jadx artifact; the real operand is `0x01010101`
   (fallback output). This is a self-keying check: the top byte of `w` selects the single-byte
   XOR key replicating over all four bytes, so it only constrains bytes 1–3 of that word.
2. `#candidate` (…fb2:880-1061 / LibraryMaskResolver.java:363-405) XOR-decodes all 8 words with
   that same key `k = 0x01010101 * ((w5>>>24) ^ 0x52)` and accepts one of two prologues:
   - `ADRP, ADRP, LDR-q, LDR-q` → instruction base = window + 4
   - `ADRP, LDR-q, ADRP, LDR-q` → instruction base = window + 8
   with `ADRP = (i & 0x9F000000) == 0x90000000` (:407-409) and
   `LDR-q = (i & 0xFFC00000) == 0x3DC00000` (:411-413). Additional pattern gates
   (…fb2:944-979): `(w16 & ~31) == 0x52800020`, `(w20 & 0xFFC00000) == 0x39000000`,
   `(w24 & 0xFFC00000) == 0xAD000000`, `(w28 & 0xFC000000) == 0x14000000`, plus register-field
   binding: `LDR.Rn == ADRP.Rd` for both pairs, `w20.Rd == w16.Rd`, and the STP pair must name the
   two ADRP destination registers in either order. The final word must be an unconditional
   branch with a sign-extended offset that is negative and ≥ −4096 (`signExtend(w28 & 0x3FFFFFF, 26) << 2`
   in `[-4096, 0)`, :981-995) — i.e. a backward branch inside the same small routine.
3. Address computation: `ADRP` target = `(insn_addr & ~0xFFF) + (signExtend(((i>>>29)&3)|(((i>>>5)&0x7FFFF)<<2), 21) << 12)`
   (`#adprAddress` — LibraryMaskResolver.java:415-417), plus `((ldr >>> 10) & 0xFFF) * 16`
   (:397-398).
4. Each address is translated VA→file offset through PT_LOAD (`Elf#dataOffset` —
   LibraryMaskResolver.java:603-618); an offset landing in an executable/ambiguous segment is
   rejected, and the full 16 bytes must be inside the segment. The pair order is normalised by
   which STP register order matched (`new Candidate(first, second, xor)` at :404 /
   …fb2:1018-1032).
5. Candidates are de-duplicated by `"first:second"` and capped at 64
   (`MAX_CANDIDATES`, :22; error text at LibraryMaskResolver.java:341-343). All candidate halves
   are read once into a `TreeMap` (LibraryMaskResolver.java:106-124).
6. Result label: `ELF ARM64 指令重定位（原始编码|单字节 XOR 0xNN）`
   (`#encodingDescription` — LibraryMaskResolver.java:275-277).

### 5.3 Turning the 32 bytes into a key (encoding search + HMAC gate)

`#authenticate` (…fb2\LibraryMaskResolver.java:743-878 / LibraryMaskResolver.java:267-273) tries
every candidate encoding `e` from `ConfigCrypto#materialEncodingCandidates`
(ConfigCrypto.java:39-52): the array is exactly `[226, 0, 16, 3, 4, …, 255]`, i.e. **0xE2, 0x00,
0x10 first, then all other byte values in ascending order with those three omitted** (total 256).
For each `e`:

1. `material = join(first16, second16)` with every byte XORed by `e`
   (`material[d] ^= (byte)e`, …fb2:754-767).
2. `ConfigCrypto#deriveKeyFromHash(material, hashFieldBytes)` (ConfigCrypto.java:72-91):
   - decrypt the `Hash` value of `UserGameplay.ini` using `material` as key (§6);
   - payload must match `[0-9a-fA-F]{40}` (ConfigCrypto.java:77-79) → 20 raw bytes;
   - `deviceKey = material[i] ^ hash20[i % 20]` for `i` in 0..31
     (`#repeatXor` — ConfigCrypto.java:298-304).
3. `ConfigCrypto#decodeIni(Engine.ini "Token", deviceKey)` must decrypt **and** pass HMAC
   (…fb2:770-773). When a full snapshot is being validated the caller also passes the Engine bytes
   so both are checked in one pass (LibraryMaskResolver.java:70-78, `resolve(..., z)`).
4. First success wins and returns `mask = XORed 32 bytes`, `materialEncoding = e`,
   `codeEncoding = candidate.xor` (`Result` — LibraryMaskResolver.java:37-53; construction
   …fb2:828 / LibraryMaskResolver.java:208). A second, *different* authenticating mask is treated
   as ambiguity and aborts (LibraryMaskResolver.java:134-140).
5. Errors: `UserGameplay` matched but Engine HMAC failed → generic `GeneralSecurityException`
   (…fb2:784-787); Engine decrypted but header/format unsupported → wrapped
   `AuthenticatedFormatException` (…fb2:788-800) — this and the budget exception stop the
   APK-fallback path (`#stopLibraryFallback` — GameRepository.java:930-941).
6. Budget: 64 MiB (`MAX_MATERIAL_AUTH_BYTES = 67108864`, ConfigCrypto.java:18) of Hash-field HMAC
   work; each candidate consumes `hashField.length` bytes
   (`AuthenticationBudget#consume` — LibraryMaskResolver.java:279-293); exhaustion throws
   `MaterialAuthenticationBudgetException`.

Result caching: `GameRepository#resolveMask` stores `.a` (first offset), `.b` (second offset),
`.e` (encoding) in `SharedPreferences("verified-library-offsets")` under
`SHA-256(installation.cacheIdentity() + "|" + librarySourceId + "|" + length)`
(GameRepository.java:847-857, :906-912; `cacheIdentity` at :1531-1533 — package|versionName|
versionCode|lastUpdateTime|uid|sourceDir|nativeLibraryDir). On a cache hit the stored offsets are
re-read and re-authenticated before use (:858-893, `#authenticateCachedMask` :943-983).

Dead/unused residue in this build: `ConfigCrypto#decodeFixedMask` (XOR 0xE2, ConfigCrypto.java:54-61),
`GameRepository.mask` (XOR 226, GameRepository.java:1141-1151),
`BASELINE_CODE = 2207` / `BASELINE_VERSION = "1.201.37117.67"` (GameRepository.java:36-37) — no
call sites in the dex. **UNKNOWN**: their intended purpose.

---

## 6. INI cipher container format

Class: `ConfigCrypto` (ConfigCrypto.java:16-317). Everything below is verified from
`#decrypt` (:195-246) and `#encrypt` (:248-288).

### 6.1 Container layout (the Base64-wrapped blob)

```
value = Base64_std( tag(20) || AES-ECB-NoPadding(plaintext) )
```

- Base64 is `java.util.Base64` standard alphabet with `=` padding
  (`Base64.getDecoder()`/`getEncoder()`, :200, :282).
- `tag` = first 20 bytes = **HMAC-SHA1 over the *plaintext block*** (`Mac.getInstance("HmacSHA1")`,
  :211-212, :272-274), keyed with the **32-byte device/material key itself**.
- AES key = `tag XOR key` repeated: `#repeatXor(key, tag)` → 32 bytes (:205, :275).
- AES mode = **`AES/ECB/NoPadding`**, **AES-256** (key is always 32 bytes — `#requireKey`,
  :306-310). No IV, no GCM, no PKCS#7 — the plaintext block is already 16-byte padded.
- Total decoded length must satisfy `len > 20` and `(len - 20) % 16 == 0` (:201-203).
- Encryption: `#encrypt` builds `plaint`, HMACs it, XORs the tag into the AES key, encrypts, then
  concatenates `tag || ciphertext` and Base64-encodes.
- Base64 padding policy on rewrite: if the original `Token` value on disk did **not** end with
  `=` (code 61), trailing `=` are stripped from the new value, i.e. the tool preserves the
  existing padded/unpadded style (`#replaceIni` — ConfigCrypto.java:115-118).
- Tamper check: `MessageDigest.isEqual(tag, hmac(plaintext))`, error text
  `配置认证失败：设备、游戏版本或文件不匹配` (:213-215); non-Base64 → `认证封装不是有效的 Base64`
  (:243-245).

### 6.2 Plaintext layout (inside AES)

`#decrypt` :217-224 / `#encrypt` :251-271:

| Offset | Size | Meaning |
|---|---|---|
| 0 | 2 | version, **little-endian uint16**; accepted values **17665 (0x4501)** and **17666 (0x4502)**; re-encryption reuses the version it decrypted (`Decoded.magic` — :226, :113) |
| 2 | 4 | payload length `L`, **little-endian uint32** (4-byte loop, `length >>> (i*8)`, :254-256 / :219-221) |
| 6 | L | payload, one char per byte |
| 6+L | pad | zero bytes; total size = `(L + 6 + 15) & -16` (:251) |

- Char encoding: stored byte `b` = `char - 1`; decoded char = `(byte & 255) + 1`
  (`#payloadText` — :290-296; encode loop :257-263). So payload chars live in `U+0001..U+0100`;
  the encoder rejects `charAt < 1 || charAt > 256` (:260-262).
- Length sanity: `L > blocksize - 6` is rejected as unsupported header
  (`配置已通过 HMAC，但认证头的版本或长度不受支持`, :222-223).
- After a header check failure the exception type is `AuthenticatedFormatException` (a
  `GeneralSecurityException`), which is the type that aborts the APK fallback path.

### 6.3 Per-device key derivation (`ConfigCrypto#deriveKey`, :63-70)

1. `H = getField(UserGameplay.ini, "Hash")` — `#getField` (:148-173) matches
   `^Hash[ \t]*=[ \t]*([^\r\n]+)` under `MULTILINE` (:175-177), trims, strips one pair of
   surrounding quotes, rejects duplicates, empty values and any non-ASCII byte.
2. `digest20 = payload(decrypt(H, materialKey))` must be exactly 40 hex chars (:77-84).
3. `deviceKey[0..31] = material[i] ^ digest20[i % 20]` (:85-87).
4. Engine.ini's `Token` is decrypted with `deviceKey` (`#decodeIni` — :93-100); the same key
   re-encrypts it (`#replaceIni` — :102-146).

`#replaceIni` additionally re-reads and re-decodes its own output and throws
`配置重新编码回读不一致` if the round-trip is not byte-exact (:139-141); max INI size 2 MiB
(`MAX_INI_BYTES`, :17; `#checkSize` :312-316), max content length 2097120 (:104).

---

## 7. Write transaction order, and the two kinds of restore

Driver: `GameRepository#transact` (GameRepository.java:338-444). Helper steps cited inline.
Every filesystem action goes through `su -c` with `RootShell.quote` and a guard that re-asserts
`id -u == 0` and the mount namespace (`RootShell#guard` — RootShell.java:172-175; exit codes 49/50).
Exit codes 44/45/46/47/48 are produced by the local shell checks (`#checked` mapping,
GameRepository.java:1349-1378; guards at :754-765).

### 7.1 `apply` / `resetDefaults` / `restoreLatest` common transaction

1. **Pre-checks** — `checkSnapshot` (:340), which re-verifies installation identity, snapshot
   hashes and live file hashes (GameRepository.java:619-642).
2. **txId** `yyyyMMdd_HHmmss_SSS-<uuid>`; `txDir = base/<txId>` (:341-342).
3. **ensureStorage** — `noLink` on `ROOT`, `backups`, `backups/<pkg>`, `backups/<pkg>/<user>`;
   `umask 077; mkdir -p base`; `chown 0:0`; `chmod 700` (:343, `#ensureStorage` :1238-1248).
4. **mkdir txDir** `umask 077` (:344).
5. **Capture metadata** of both target files: `stat -c '%u %g %a'` + `ls -Zd` SELinux label,
   validated by regex (`#metadata` :1283-1298; `Metadata` :1548-1566).
6. **Write manifest** `format=2, package, versionName, versionCode, lastUpdateTime, sourceDir,
   nativeLibraryDir, userId, uid, device, enginePath, gamePath, gameplayPath, engineHash,
   gameHash, gameplayHash, updateLatest, engineMetadata, gameMetadata` (:348-349).
   `device = SHA-256(android_id + "|" + userId)` (`#deviceIdentity` :1332-1342).
7. **Backup** all three originals: `cp -p <src> txDir/<Name>` → `chown 0:0` → `chmod 600`
   (:350-352, `#backupOne` :446-450).
8. **Commit the manifest atomically**: `#writeRoot` = create `<path>.write-<uuid>` with
   `umask 077; cat >`, `chown 0:0`, `chmod 600`, read-back SHA-256 verify, `mv -f`, `sync`
   (:1271-1281). The same helper writes `state`, `state.commit-<uuid>` and the pointer temp files.
9. **Verify the backup** by re-reading all three copies and comparing SHA-256 to the manifest
   (:354, `#verifyBackup` :589-597).
10. **Stage** each target (`#prepare` :452-464): `regular()` (no symlink, exists, regular, readable);
    `absent(<f>.deltaconfig-<txId>.old)` and `absent(...new)`; `cp -p <f> <f>.old`;
    `cp -p <f> <f>.new`; `setMetadata(.old)`; `cat > .new` with the new bytes;
    `setMetadata(.new)` (`chown`, `chmod`, `chcon`, fallback `restorecon`, then re-verify
    — :1300-1330); read-back `.new` and verify its hash.
11. **Re-check snapshot** (`checkSnapshot`, :357).
12. **State `PREPARED\n`** (:358).
13. **Pointer `pending`** → txId (`#writePointer` :1263-1269: temp + `mv -f` + `sync`; :359).
14. **CommitBoundary phase 0** ⇒ rollback still allowed (:360, `CommitBoundary` :1417-1455).
15. **Swap files by rename**: `checkInstallation`; `mv -f <engine>.new <engine>`;
    `checkInstallation`; `mv -f <game>.new <game>`; `sync` (:364-379). *No rename of the
    authorisation source* — `UserGameplay.ini` is never staged.
16. **Read-back and verify** (:380-389): read Engine/Game/UserGameplay; `checkInstallation`;
    SHA-256 of Engine and Game must equal the intended bytes; SHA-256 of UserGameplay must equal
    the snapshot hash (unchanged); `authenticate()` re-derives the key and HMAC-verifies both new
    files (:1153-1175); `verifyMetadata` on both (:1325-1330).
17. **Commit marker**: write `state.commit-<uuid>` containing `COMMITTED\n` (via `#writeRoot`, so it
    is already read-back-verified and synced) → `markerAttempted()` → `mv -f state.commit-<uuid>
    state` → `markerConfirmed()` → `sync` → `markerSynced()` (:390-399).
18. **Only for `apply`/`resetDefaults`** (`z == true`): `writePointer("latest", txId)` (:402-404).
19. **Finalise**: `rm -f pending` (:405); `cleanup()` removes `.new`/`.old`/`.rollback` siblings
    (:406, :563-573). Success text includes the backup directory (:407).

Failure handling (:408-443), gated by the marker phase:

- marker not yet attempted (`canRollback()`, phase 0) → **automatic rollback**, then rethrow the
  original failure (:411-428). If rollback itself fails, both errors are reported, and
  `writeStatus`/`rollbackStatus` tell the user whether the originals were restored (:418-427).
- marker attempted but rename result unconfirmed → **no rollback**, pending index kept, next read
  reconciles (:430-434).
- marker confirmed but `sync` unconfirmed → **no rollback**, pending kept (:435-439).
- post-commit pointer/cleanup failure → **no rollback**, commit stands (:440-442).

### 7.2 Rollback (`#rollback` :510-532)

Read the three backups → `verifyBackup` against the manifest → verify the *current*
`UserGameplay.ini` still hashes to the manifest `gameplayHash` (else abort, §605-609) →
`checkInstallation` → `#restoreOne` for Engine then Game (:550-561: `regular`, `noLink` on
`<f>.deltaconfig-<txId>.rollback`, `cp -p` target→rollback, `cat >` rollback with backup bytes,
`setMetadata`, read-back verify, `checkInstallation`, `mv -f rollback → target`, `verifyMetadata`)
→ `checkInstallation` → read-back both and compare SHA-256 with manifest → `originalRestored=true`
→ write `state = ROLLED_BACK\n` → `sync` → `rm -f pending` → `cleanup`.

### 7.3 Crash recovery (`#recover` :466-508)

Called at the start of `load`, `apply`, `resetDefaults`, `restoreLatest`. Reads `pending`; if set,
reads `state` (first 128 bytes, ASCII, trimmed):

- `COMMITTED` → do **not** roll back; if the manifest's `updateLatest` is true, re-publish `latest`;
  `rm -f pending`; `cleanup` (:478-488).
- `PREPARED` or `ROLLED_BACK` → `rollback` (:489-505).
- anything else → hard stop with `事务状态不是 PREPARED、COMMITTED 或 ROLLED_BACK…` (:507).

### 7.4 "Restore defaults" vs "Restore latest backup"

**Restore defaults** (`GameRepository#resetDefaults` :268-297 → `ConfigPatcher#resetDefaults`,
…_fb\ConfigPatcher.java:620-759):

- Requires a live snapshot and re-authenticates the *current* files first (:278).
- **Generates new content** by *removing* this tool's fields, not by copying anything:
  - Engine.ini: `removeFields` for `[DFMPerfGearConfig]` keys `DFMQualityLevel`, `DFMFPSLevel`;
    `[QualityFPSSetting]` key `Unlimit`; `[SystemSettings]` keys `PerfGear.SmallPadSize`,
    `DFMDeviceFrame.White90fps/120/144/165/185fps` (…_fb:632-649), then re-encrypts/re-HMACs
    through `ConfigCrypto#replaceIni` (…_fb:650).
  - Game.ini: `removeFields` on `[/Script/PerfGear.PerfGearAndroidPadSizeConfig]` for
    `SmallPadConfig`, `MidPadConfig`, `BigPadConfig` (…_fb:702-708), BOM preserved
    (3 bytes prefixed back, …_fb:711-723), requires at least one INI section header and no NUL
    (…_fb:695-701, 733-737).
- Resulting bytes are re-authenticated (:284) and then passed to `transact(..., true)` (:289), so
  the pre-reset files become the new backup and `latest` **advances to this reset transaction**
  (:402-404) — i.e. "restore defaults" is undoable with "restore latest backup".
- If the files already contain none of those keys, nothing is written and the method returns early
  (:285-288).
- UI: button `恢复默认画质设置` (MainActivity.java:320), dialog `将清除画质、帧率和渲染比例覆盖设置，
  进入游戏后由游戏重新生成默认设置` (MainActivity.java:987).

**Restore latest backup** (`GameRepository#restoreLatest` :299-323):

- Does **not** require a snapshot argument: it calls `capture(paths, false)` (:306), the
  validation-light path — only `verifyGameplay` (key derivation succeeds against the *current*
  `UserGameplay.ini`), no Engine HMAC decode at that point (:173, :1208-1214).
- `pointer("latest")` → manifest → read `Engine.ini`/`Game.ini`/`UserGameplay.ini` from the backup
  → `verifyBackup` (:313-315) → current `UserGameplay.ini` hash must equal the manifest's
  `gameplayHash` (:316, `#verifyContext` :611-617) → `authenticate(capture.fixedMask,
  capture.gameplay, backupEngine, backupGame, ...)` (:317) → `transact(..., false)` (:318).
- Because `z == false`, it **does not** move `latest` (:402-404), so the same backup can be
  restored repeatedly; the restore itself still creates a fresh backup directory (a later
  "restore latest" would therefore still point at the original pre-modification snapshot).
- Return text: `已恢复原配置；备份仍保留在 <dir>` (:322). UI: button `还原最近备份`
  (MainActivity.java:300), dialog text "将恢复此发行版最近一次修改前保存的设置"
  (MainActivity.java:896).

Manifest compatibility gate for both paths: `#manifest` (:575-587) requires `format ∈ {1,2}`,
same package, same `versionName`+`versionCode`, and for format 2 also `lastUpdateTime`,
`sourceDir`, `nativeLibraryDir`; equal `userId`, `uid`, `device` (Android-ID hash), and identical
engine/game/gameplay paths.

Size limits: 4 MiB (`LIMIT`, :40) for writes and reads, 2 MiB for INI plaintext
(ConfigCrypto.java:17), 1 MiB per ELF read (`LIBRARY_READ_LIMIT`, :38), 8 GiB library length (:39),
1 GiB APK inflate budget (`APK_INFLATE_LIMIT`, :35).

---

## 8. UNKNOWNs / not verifiable in this pass

1. **`R.attr.cacheColorHint`** is referenced by the primary decompile
   (LibraryMaskResolver.java:325) but the generated `R.java` has no `attr` inner class
   (R.java:1-40). The fallback decompile shows the real operand `16843009` (0x01010101)
   (…fb2:1142-1143). Treated as a jadx rendering artifact, not a real resource read.
2. **Exact ARM64 mnemonics** for the mask constants `0x39000000` and `0xAD000000` are inferred
   (byte load / 128-bit store pair); only the bit patterns and register-field constraints are
   proven from code (…fb2:944-979).
3. **Whether the current shipping `libUE4.so` still contains the legacy bytes** at decimal
   offsets 24240560 / 24385264 cannot be determined without that library; the code path is only
   an optimisation tried before the scan (LibraryMaskResolver.java:88-101).
4. **Purpose of dead code** `ConfigCrypto#decodeFixedMask`, `GameRepository#mask`,
   `BASELINE_CODE`/`BASELINE_VERSION` — no call sites (ConfigCrypto.java:54-61,
   GameRepository.java:36-37, 1141-1151).
5. **Resource/manifest facts** (app versionName, permissions, min/target SDK) were not extracted
   because the decompile was run with `--no-res`; not needed for the questions asked.
6. `ConfigPatcher.qualityLabel`/`ConfigPatcher.getInt` handle `终极` (5) as a normal tier; whether
   the game engine actually honours a `DFMFPSLevel`-independent cap for tier 5 is a runtime
   property, and the tool only warns about it (ConfigPatcher.java:110).
