#!/system/bin/sh
# dfm_perf.sh — Delta Force (CN, com.tencent.tmgp.dfm) graphics / frame-rate configurator
#
# Applies quality + frame-rate + render-resolution overrides to the game's own
# encrypted Engine.ini and plain Game.ini, with backup / verification / rollback.
#
# Requires: root (su), openssl, sha256sum, tr, od, awk, dd, base64 (openssl fallback ok)
# Tested host-side with the app's own ConfigCrypto implementation (see report).
#
# Third-party research tooling. Writing game config may violate the game's terms
# and can be detected by anti-cheat. Use at your own risk.

set -u

PKG=com.tencent.tmgp.dfm
REL="files/UE4Game/DeltaForce/DeltaForce/Saved/Config/Android"
USER_ID=${DC_USER_ID:-0}
SU=${DC_SU:-su -c}
# path overrides (host testing / unusual layouts)
DATA_DIR=${DC_DATA_DIR:-/data/data/$PKG}
ENGINE=${DC_ENGINE:-$DATA_DIR/$REL/Engine.ini}
GAME=${DC_GAME:-$DATA_DIR/$REL/Game.ini}
GAMEPLAY=${DC_GAMEPLAY:-/storage/emulated/$USER_ID/Android/data/$PKG/$REL/UserGameplay.ini}
GAMEPLAY_ALT=${DC_GAMEPLAY_ALT:-/data/media/$USER_ID/Android/data/$PKG/$REL/UserGameplay.ini}
LIB=${DC_LIB:-/data/app/placeholder/lib/arm64-v8a/libUE4.so}
BACKUP_ROOT=${DC_BACKUP_ROOT:-/data/adb/deltaconfig/backups/$USER_ID}

# libUE4.so SHA-256 -> "offset1:offset2" (16-byte material halves, verified by ARM64 scan)
KNOWN_LIB_MATERIAL="
686577e4acf4bdd45b113b579d72dd581e9beffb6f6d04d07871b0341ff470c1 0x1559300:0x157d9c0
"

QUAL_LABELS="流畅 标准 精致 高清 极致 终极"
FPS_LABELS="60 90 120 144 165 185"
FPS_ENUMS="2 7 8 9 10 11"

TMPD=
OPENSSL=
TXID=
BACKUP_DIR=
WROTE_ENGINE=0
WROTE_GAME=0

msg()  { printf '%s\n' "$*" >&2; }
die()  { printf 'ERROR: %s\n' "$*" >&2; exit 1; }

cleanup() {
    [ -n "$TMPD" ] && [ -d "$TMPD" ] && rm -rf "$TMPD" 2>/dev/null
}
trap cleanup EXIT HUP INT TERM

need_tools() {
    for t in awk tr od dd mkdir cp mv rm sha256sum cat; do
        command -v "$t" >/dev/null 2>&1 || die "missing required tool: $t"
    done
    detect_openssl || true
}

detect_openssl() {
    if [ -n "${DC_OPENSSL:-}" ]; then OPENSSL=$DC_OPENSSL; return 0; fi
    if command -v openssl >/dev/null 2>&1; then OPENSSL=openssl; return 0; fi
    if command -v busybox >/dev/null 2>&1 && busybox openssl version >/dev/null 2>&1; then
        OPENSSL="busybox openssl"; return 0
    fi
    OPENSSL=
    return 1
}

require_openssl() {
    [ -n "$OPENSSL" ] || die "缺少 openssl：无法做 AES/HMAC 认证与重新加密，已拒绝修改。请安装 Termux 后执行 pkg install openssl，或在带 openssl 的 busybox 环境运行（可用 DC_OPENSSL 指定路径）。"
}

# ---------------------------------------------------------------- root plumbing
asroot() { $SU "$1"; }

r_cat()  { asroot "cat '$1'"; }
r_size() { asroot "stat -c %s '$1' 2>/dev/null || wc -c < '$1'"; }
r_sha()  { asroot "sha256sum '$1'"; }
r_test() { asroot "test -f '$1'" >/dev/null 2>&1; }

r_put() { # r_put <localfile> <dest>
    asroot "umask 077; cp -p '$2' '$2.dc-tmp' 2>/dev/null; cat > '$2'" < "$1" || die "写入失败: $2"
}

r_mv() { asroot "mv -f '$1' '$2'"; }
r_sync() { asroot "sync"; }

check_root() {
    u=$(asroot "id -u" 2>/dev/null | tr -d '\r\n ')
    [ "$u" = "0" ] || die "需要 root（su 返回 UID='${u:-?}'）"
}

resolve_paths() {
    if ! r_test "$ENGINE"; then
        for alt in "/data/user/0/$PKG" "/data/data/$PKG"; do
            if r_test "$alt/$REL/Engine.ini"; then
                DATA_DIR=$alt
                ENGINE=$DATA_DIR/$REL/Engine.ini
                GAME=$DATA_DIR/$REL/Game.ini
                break
            fi
        done
    fi
    r_test "$ENGINE" || die "找不到 Engine.ini（请先启动一次游戏生成配置）: $ENGINE"
    r_test "$GAME"   || die "找不到 Game.ini: $GAME"
    if ! r_test "$GAMEPLAY"; then
        if r_test "$GAMEPLAY_ALT"; then GAMEPLAY=$GAMEPLAY_ALT
        else die "找不到 UserGameplay.ini（本机认证上下文）: $GAMEPLAY"; fi
    fi
    GAMEPLAY=$(asroot "readlink -f '$GAMEPLAY'" 2>/dev/null | tr -d '\r\n')
    [ -n "$GAMEPLAY" ] || die "无法解析 UserGameplay.ini 真实路径"
    if [ -z "${DC_LIB:-}" ]; then
        apk=$(asroot "pm path $PKG" 2>/dev/null | sed -n 's/^package://p' | head -1 | tr -d '\r')
        if [ -n "$apk" ]; then
            cand="${apk%/*}/lib/arm64-v8a/libUE4.so"
            if r_test "$cand"; then LIB=$cand; fi
        fi
        if [ "$LIB" = "/data/app/placeholder/lib/arm64-v8a/libUE4.so" ]; then
            found=$(asroot "ls -d /data/app/*/$PKG*/lib/arm64-v8a/libUE4.so 2>/dev/null | head -1" | tr -d '\r')
            [ -n "$found" ] && LIB=$found
        fi
    fi
}

# ------------------------------------------------------------------- primitives
hex2bin() { printf '%s' "$1" | sed 's/../& /g' | awk '{for(i=1;i<=NF;i++)printf "\\%03o", strtonum("0x"$i)}'; }
bin2hex() { od -An -tx1 -v | tr -d ' \r\n'; }
b64dec()  { $OPENSSL base64 -d -A 2>/dev/null || $OPENSSL enc -base64 -d -A; }
b64enc()  { $OPENSSL base64 -A; }
hmac1()   { $OPENSSL dgst -sha1 -mac HMAC -macopt "hexkey:$1" -binary; }
aes_ecb() { # aes_ecb <dec|enc> <keyhex>      (stdin -> stdout, no padding)
    if [ "$1" = "dec" ]; then
        $OPENSSL enc -aes-256-ecb -K "$2" -nopad -nosalt -d 2>/dev/null
    else
        $OPENSSL enc -aes-256-ecb -K "$2" -nopad -nosalt 2>/dev/null
    fi
}
xor32() { # xor32 <hex32> <hexN> -> hex32
    awk -v a="$1" -v b="$2" 'BEGIN{
        n=length(b)/2; s="";
        for(i=0;i<32;i++){
            ah=substr(a,i*2+1,2); bh=substr(b,(i%n)*2+1,2);
            s=s sprintf("%02x", xor(strtonum("0x"ah), strtonum("0x"bh)));
        }
        print s
    }'
}

# ------------------------------------------------------- container encryption
# container = base64( HMAC-SHA1(block) || AES-256-ECB( block ) )
# block     = magic:u16le | length:u32le | payload (each byte = char-1) | zero pad
# aes key   = key32 XOR tag (tag repeated)
enc_container() { # enc_container <keyhex> <magic> <plaintext-file> -> base64 on stdout
    key=$1; magic=$2; src=$3
    len=$(wc -c < "$src" | tr -d ' ')
    [ "$len" -gt 2097120 ] && die "配置过大"
    total=$(( (len + 6 + 15) / 16 * 16 ))
    block="$TMPD/block.bin"
    {
        printf "\\$(printf '%03o' $((magic % 256)))\\$(printf '%03o' $((magic / 256 % 256)))"
        printf "\\$(printf '%03o' $((len % 256)))\\$(printf '%03o' $((len / 256 % 256)))\\$(printf '%03o' $((len / 65536 % 256)))\\$(printf '%03o' $((len / 16777216 % 256)))"
        tr '\001-\377' '\000-\376' < "$src"
        dd if=/dev/zero bs=1 count=$((total - len - 6)) 2>/dev/null
    } > "$block"
    tag=$(hmac1 "$key" < "$block" | bin2hex)
    [ ${#tag} -eq 40 ] || die "HMAC 计算失败"
    aeskey=$(xor32 "$key" "$tag")
    tagbin=$(printf '%s' "$tag" | sed 's/../& /g' | awk '{for(i=1;i<=NF;i++)printf "\\%03o", strtonum("0x"$i)}')
    { printf "$tagbin"; aes_ecb enc "$aeskey" < "$block"; } | b64enc
}

dec_container() { # dec_container <keyhex> <b64value> <outfile> -> magic on stdout, fails on bad auth
    key=$1; val=$2; out=$3
    printf '%s' "$val" | b64dec > "$TMPD/ct.bin" 2>/dev/null || return 1
    sz=$(wc -c < "$TMPD/ct.bin" | tr -d ' ')
    [ "$sz" -gt 20 ] || return 1
    [ $(( (sz - 20) % 16 )) -eq 0 ] || return 1
    dd if="$TMPD/ct.bin" bs=1 count=20 2>/dev/null > "$TMPD/tag.bin"
    dd if="$TMPD/ct.bin" bs=1 skip=20 2>/dev/null > "$TMPD/body.bin"
    tag=$(bin2hex < "$TMPD/tag.bin")
    aeskey=$(xor32 "$key" "$tag")
    aes_ecb dec "$aeskey" < "$TMPD/body.bin" > "$TMPD/plain.bin" 2>/dev/null || return 1
    check=$(hmac1 "$key" < "$TMPD/plain.bin" | bin2hex)
    [ "$check" = "$tag" ] || return 1
    magic=$(od -An -tu2 -N2 "$TMPD/plain.bin" | tr -d ' \r\n')
    [ "$magic" = "17665" ] || [ "$magic" = "17666" ] || return 1
    len=$(od -An -tu4 -j2 -N4 "$TMPD/plain.bin" | tr -d ' \r\n')
    psz=$(wc -c < "$TMPD/plain.bin" | tr -d ' ')
    [ "$len" -le $((psz - 6)) ] || return 1
    dd if="$TMPD/plain.bin" bs=1 skip=6 count="$len" 2>/dev/null | tr '\000-\376' '\001-\377' > "$out"
    printf '%s' "$magic"
}

ini_get() { # ini_get <file> <section|__any__> <key>
    awk -v sec="$2" -v key="$3" '
        { line=$0; sub(/\r$/,"",line) }
        /^[ \t]*\[[^]]+\]/ {
            s=line; sub(/^[ \t]*\[/,"",s); sub(/\][ \t]*$/,"",s)
            if (sec=="__any__") insec=1; else insec=(tolower(s)==tolower(sec)); next
        }
        (sec=="__any__" || insec) {
            p=index(line,"="); if(p==0) next
            k=substr(line,1,p-1); gsub(/^[ \t]+|[ \t]+$/,"",k)
            if(tolower(k)==tolower(key)){
                v=substr(line,p+1); sub(/^[ \t]+/,"",v); sub(/[ \t]+$/,"",v)
                sub(/[;#].*$/,"",v); sub(/[ \t]+$/,"",v)
                print v; exit
            }
        }' "$1"
}

ini_set() { # ini_set <infile> <outfile> <section> <key> <value>
    awk -v sec="$3" -v key="$4" -v val="$5" '
        BEGIN{ done=0; insec=0; lastsecend=0 }
        { lines[NR]=$0 }
        END{
            n=NR
            for(i=1;i<=n;i++){
                line=lines[i]; bare=line; sub(/\r$/,"",bare)
                if(bare ~ /^[ \t]*\[[^]]+\]/){
                    s=bare; sub(/^[ \t]*\[/,"",s); sub(/\][ \t]*$/,"",s)
                    if(insec && lastsecend==0) lastsecend=i-1
                    insec=(tolower(s)==tolower(sec))
                    if(insec) lastsecend=i
                    continue
                }
                if(insec){
                    p=index(bare,"=")
                    if(p>0){
                        k=substr(bare,1,p-1); gsub(/^[ \t]+|[ \t]+$/,"",k)
                        if(tolower(k)==tolower(key)){
                            cr = (line ~ /\r$/) ? "\r" : ""
                            rest=substr(bare,p+1)
                            ind=substr(rest,1,match(rest,/[^ \t]/)-1)
                            lines[i]=substr(bare,1,p) ind val cr
                            done=1
                        }
                    }
                    lastsecend=i
                }
            }
            if(!done){
                if(lastsecend==0){
                    printf "\n[%s]\n%s=%s\n", sec, key, val
                } else {
                    for(i=1;i<=n;i++){
                        if(i==lastsecend){
                            cr=(lines[i] ~ /\r$/) ? "\r" : ""
                            print lines[i]
                            printf "%s=%s%s\n", key, val, cr
                        } else print lines[i]
                    }
                }
            } else {
                for(i=1;i<=n;i++) print lines[i]
            }
        }' "$1" > "$2"
}

ini_del() { # ini_del <infile> <outfile> <section> <key>...
    awk -v sec="$3" -v keys="$4" '
        BEGIN{ nk=split(keys,K," "); insec=0 }
        {
            line=$0; bare=line; sub(/\r$/,"",bare); drop=0
            if(bare ~ /^[ \t]*\[[^]]+\]/){
                s=bare; sub(/^[ \t]*\[/,"",s); sub(/\][ \t]*$/,"",s)
                insec=(tolower(s)==tolower(sec))
            } else if(insec){
                p=index(bare,"=")
                if(p>0){
                    k=substr(bare,1,p-1); gsub(/^[ \t]+|[ \t]+$/,"",k)
                    for(j=1;j<=nk;j++) if(tolower(k)==tolower(K[j])) drop=1
                }
            }
            if(!drop) print line
        }' "$1" > "$2"
}

# ---------------------------------------------------------------- materiaL
find_material() {
    if [ -n "${DC_MATERIAL_HEX:-}" ]; then
        [ ${#DC_MATERIAL_HEX} -eq 64 ] || die "DC_MATERIAL_HEX 必须是 64 位十六进制"
        printf '%s' "$DC_MATERIAL_HEX"
        return
    fi
    off1= off2=
    if [ -n "${DC_LIB_OFFSETS:-}" ]; then
        off1=${DC_LIB_OFFSETS%%:*}; off2=${DC_LIB_OFFSETS##*:}
    elif r_test "$LIB"; then
        h=$(r_sha "$LIB" | awk '{print $1}')
        for line in $KNOWN_LIB_MATERIAL; do
            case "$line" in \#*|"") continue;; esac
            if [ "$(printf '%s' "$line" | awk '{print $1}')" = "$h" ]; then
                pair=$(printf '%s' "$line" | awk '{print $2}')
                off1=${pair%%:*}; off2=${pair##*:}
            fi
        done
        [ -n "$off1" ] || die "未收录该 libUE4.so 的版本（sha256=$h）。请用 --material-hex/DC_MATERIAL_HEX 提供材料，或等待工具更新。"
    else
        die "找不到 libUE4.so 且未提供 DC_MATERIAL_HEX"
    fi
    a=$(asroot "dd if='$LIB' bs=1 skip=\$(( $off1 )) count=16 2>/dev/null" | bin2hex)
    b=$(asroot "dd if='$LIB' bs=1 skip=\$(( $off2 )) count=16 2>/dev/null" | bin2hex)
    [ ${#a} -eq 32 ] && [ ${#b} -eq 32 ] || die "读取 libUE4.so 材料失败"
    printf '%s%s' "$a" "$b"
}

derive_key() { # derive_key <material-hex> <gameplay-file> -> key hex (also sets KEY, MAGIC)
    mat=$1; gp=$2
    hashval=$(ini_get "$gp" "__any__" "Hash")
    [ -n "$hashval" ] || die "UserGameplay.ini 中没有 Hash 字段"
    hashval=$(printf '%s' "$hashval" | tr -d '"' | tr -d '\r')
    for e in 226 0 16 $(seq 3 255); do
        mh=$(awk -v m="$mat" -v e="$e" 'BEGIN{s="";for(i=0;i<64;i+=2){v=xor(strtonum("0x"substr(m,i+1,2)),e);s=s sprintf("%02x",v)}print s}')
        out="$TMPD/digest.txt"
        if dec_container "$mh" "$hashval" "$out" >/dev/null 2>&1; then
            dg=$(cat "$out" | tr -d '\r\n')
            case "$dg" in
                *[!0-9a-fA-F]*) continue;;
            esac
            [ ${#dg} -eq 40 ] || continue
            # key = material XOR digest20 (repeated)
            key=$(awk -v m="$mat" -v d="$dg" 'BEGIN{
                s="";
                for(i=0;i<32;i++){
                    mv=xor(strtonum("0x"substr(m,i*2+1,2)), strtonum("0x"substr(d,(i%20)*2+1,2)));
                    s=s sprintf("%02x", mv);
                }
                print s}')
            echo "$key" > "$TMPD/key"
            echo "$dg"  > "$TMPD/digest"
            echo "$mh"  > "$TMPD/material"
            echo "$e"   > "$TMPD/encoding"
            printf '%s' "$key"
            return 0
        fi
    done
    return 1
}

# ------------------------------------------------------------------ transaction
tx_begin() {
    TXID=$(date -u +%Y%m%d_%H%M%S)_$(cat /proc/sys/kernel/random/uuid 2>/dev/null || date +%s%N)
    BACKUP_DIR="$BACKUP_ROOT/$TXID"
    asroot "umask 077; mkdir -p '$BACKUP_DIR'" || die "创建备份目录失败"
    r_cat "$ENGINE"  > "$TMPD/Engine.ini.orig"  || die "备份 Engine.ini 失败"
    r_cat "$GAME"    > "$TMPD/Game.ini.orig"    || die "备份 Game.ini 失败"
    r_cat "$GAMEPLAY"> "$TMPD/UserGameplay.ini.orig" || die "备份 UserGameplay.ini 失败"
    asroot "umask 077; cat > '$BACKUP_DIR/Engine.ini'"       < "$TMPD/Engine.ini.orig"
    asroot "umask 077; cat > '$BACKUP_DIR/Game.ini'"         < "$TMPD/Game.ini.orig"
    asroot "umask 077; cat > '$BACKUP_DIR/UserGameplay.ini'" < "$TMPD/UserGameplay.ini.orig"
    ( cd "$TMPD" && sha256sum Engine.ini.orig Game.ini.orig UserGameplay.ini.orig ) > "$TMPD/manifest.sha"
    asroot "umask 077; cat > '$BACKUP_DIR/manifest.sha'" < "$TMPD/manifest.sha"
    asroot "umask 077; printf 'PREPARED\n' > '$BACKUP_DIR/state'"
    asroot "umask 077; printf '%s\n' '$TXID' > '$BACKUP_ROOT/pending'"
    msg "备份: $BACKUP_DIR"
}

stage_file() { # stage_file <src-local> <target-remote>
    tmp="$2.dc-$TXID.new"
    asroot "test -e '$tmp' && exit 48; cp -p '$2' '$tmp'" || die "无法创建暂存文件: $tmp"
    asroot "cat > '$tmp'" < "$1" || die "写入暂存失败: $tmp"
    got=$(r_sha "$tmp" | awk '{print $1}')
    want=$(sha256sum "$1" | awk '{print $1}')
    [ "$got" = "$want" ] || die "暂存文件校验失败: $tmp"
    printf '%s' "$tmp"
}

commit_file() { # commit_file <staged-tmp> <target>
    asroot "mv -f '$1' '$2'" || die "替换失败: $2"
    asroot "sync"
}

rollback_all() {
    msg "回滚中…"
    for pair in "$ENGINE:Engine.ini.orig" "$GAME:Game.ini.orig"; do
        tgt=${pair%%:*}; src="$TMPD/${pair##*:}"
        [ -s "$src" ] || continue
        tmp="$tgt.dc-$TXID.rollback"
        asroot "cp -p '$tgt' '$tmp'" 2>/dev/null
        asroot "cat > '$tmp'" < "$src" 2>/dev/null
        asroot "mv -f '$tmp' '$tgt'" 2>/dev/null
    done
    asroot "sync"
    ek=$(r_sha "$ENGINE" | awk '{print $1}')
    gk=$(r_sha "$GAME" | awk '{print $1}')
    e0=$(awk '/Engine.ini.orig/{print $1}' "$TMPD/manifest.sha")
    g0=$(awk '/Game.ini.orig/{print $1}' "$TMPD/manifest.sha")
    if [ "$ek" = "$e0" ] && [ "$gk" = "$g0" ]; then
        asroot "printf 'ROLLED_BACK\n' > '$BACKUP_DIR/state'"
        msg "已回滚并校验通过"
        return 0
    fi
    msg "回滚校验失败！请不要启动游戏，保留备份目录: $BACKUP_DIR"
    return 1
}

tx_commit() {
    asroot "printf 'COMMITTED\n' > '$BACKUP_DIR/state'"
    asroot "sync"
    asroot "rm -f '$BACKUP_ROOT/pending'"
    asroot "rm -f '$BACKUP_ROOT/latest'"
    asroot "umask 077; printf '%s\n' '$TXID' > '$BACKUP_ROOT/latest'"
    asroot "sync"
    for f in "$ENGINE" "$GAME"; do
        for s in .new .old .rollback; do asroot "rm -f '$f.dc-$TXID$s'"; done
    done
    msg "提交完成: $BACKUP_DIR"
}

# ------------------------------------------------------------------- commands
load_state() {
    TMPD=$(mktemp -d "${TMPDIR:-/tmp}/dfm.XXXXXX" 2>/dev/null || echo "/data/local/tmp/dfm.$$")
    [ -d "$TMPD" ] || { mkdir -p "$TMPD" || die "无法创建临时目录"; }
    check_root
    need_tools
    require_openssl
    resolve_paths
    r_cat "$ENGINE" > "$TMPD/engine.enc"
    r_cat "$GAME"   > "$TMPD/game.ini"
    r_cat "$GAMEPLAY" > "$TMPD/gameplay.ini"
    MAT=$(find_material)
    KEY=$(derive_key "$MAT" "$TMPD/gameplay.ini") || die "认证失败：无法用 UserGameplay.ini 的 Hash 派生密钥（材料/编码与设备不匹配）"
    MAGIC=$(dec_container "$KEY" "$(ini_get "$TMPD/engine.enc" __any__ Token | tr -d '\r')" "$TMPD/engine.ini" 2>/dev/null) \
        || die "Engine.ini Token 解密/认证失败"
    [ -s "$TMPD/engine.ini" ] || die "Engine.ini 解密结果为空"
}

cmd_status() {
    load_state
    q=$(ini_get "$TMPD/engine.ini" DFMPerfGearConfig DFMQualityLevel)
    f=$(ini_get "$TMPD/engine.ini" DFMPerfGearConfig DFMFPSLevel)
    u=$(ini_get "$TMPD/engine.ini" QualityFPSSetting Unlimit)
    printf '系统: Android %s\n' "$(getprop ro.build.version.release 2>/dev/null | head -1)"
    printf 'Engine.ini: %s\nGame.ini: %s\nUserGameplay.ini: %s\n' "$ENGINE" "$GAME" "$GAMEPLAY"
    printf '认证: 通过（材料编码 %s，magic %s）\n' "$(cat "$TMPD/encoding")" "$MAGIC"
    printf '当前画质枚举: %s (%s)\n' "${q:-未设置}" "$(qual_label "$q")"
    printf '当前帧率枚举: %s (%s)\n' "${f:-未设置}" "$(fps_label "$f")"
    printf 'Unlimit: %s\n' "${u:-未设置}"
    printf '渲染覆盖: %s\n' "$(ini_get "$TMPD/game.ini" /Script/PerfGear.PerfGearAndroidPadSizeConfig SmallPadConfig | cut -c1-90)"
    printf '最近备份: %s\n' "$(asroot "cat '$BACKUP_ROOT/latest' 2>/dev/null" | tr -d '\r\n' || true)"
}

qual_label() { case "$1" in 0) echo 流畅;; 1) echo 标准;; 2) echo 精致;; 3) echo 高清;; 4) echo 极致;; 5) echo 终极;; *) echo "未知";; esac; }
fps_label()  { case "$1" in 2) echo "60 帧";; 7) echo "90 帧";; 8) echo "120 帧";; 9) echo "144 帧";; 10) echo "165 帧";; 11) echo "185 帧";; *) echo "未识别";; esac; }
enum2fps()   { case "$1" in 2) echo 60;; 7) echo 90;; 8) echo 120;; 9) echo 144;; 10) echo 165;; 11) echo 185;; *) echo -1;; esac; }

pad_tuple() { # pad_tuple <pixels>
    n=$1
    printf '(Level1Config=(BackBuffer=8192,UltraLowResolution=%s,LowResolution=%s,MidResolution=%s,HighResolution=%s,UltraHighResolution=%s,SuperResolution=%s)' \
        "$n" "$n" "$n" "$n" "$n" "$n"
    for lvl in 2 3 4 5; do
        printf ',Level%sConfig=(BackBuffer=8192,UltraLowResolution=%s,LowResolution=%s,MidResolution=%s,HighResolution=%s,UltraHighResolution=%s,SuperResolution=%s)' \
            "$lvl" "$n" "$n" "$n" "$n" "$n" "$n"
    done
    printf ')'
}

cmd_set() {
    [ $# -ge 2 ] || die "用法: set <画质 0-5|流畅..终极> <帧率 2,7,8,9,10,11|60..185> [渲染比例 25-100] [短边像素] [-y]"
    yes=0; args=
    for a in "$@"; do case "$a" in -y|--yes) yes=1;; *) args="$args $a";; esac; done
    set -- $args
    q=$1; f=$2; ratio=${3:-100}; short=${4:-0}
    case "$q" in
        流畅) q=0;; 标准) q=1;; 精致) q=2;; 高清) q=3;; 极致) q=4;; 终极) q=5;;
    esac
    case "$q" in [0-5]) ;; *) die "画质档位无效: $q";; esac
    case "$f" in 60) f=2;; 90) f=7;; 120) f=8;; 144) f=9;; 165) f=10;; 185) f=11;; esac
    case "$f" in 2|7|8|9|10|11) ;; *) die "帧率档位无效: $f";; esac
    case "$ratio" in ''|*[!0-9]*) die "渲染比例无效";; esac
    [ "$ratio" -ge 25 ] && [ "$ratio" -le 100 ] || die "渲染比例须为 25-100"
    fps=$(enum2fps "$f")
    if [ "$ratio" -eq 100 ]; then
        px=8192
    else
        [ "$short" -ge 200 ] && [ "$short" -le 8192 ] || die "非 100% 渲染需提供 200-8192 的短边像素"
        px=$(( (short * ratio + 50) / 100 ))
        [ "$px" -lt 1 ] && px=1
        [ "$px" -gt 8192 ] && px=8192
    fi
    load_state
    # Engine.ini edits
    ini_set "$TMPD/engine.ini" "$TMPD/e1" DFMPerfGearConfig DFMQualityLevel "$q"
    ini_set "$TMPD/e1" "$TMPD/e2" DFMPerfGearConfig DFMFPSLevel "$f"
    ini_set "$TMPD/e2" "$TMPD/e3" QualityFPSSetting Unlimit 1
    ini_set "$TMPD/e3" "$TMPD/e4" SystemSettings PerfGear.SmallPadSize 0
    if [ "$fps" -ge 90 ]; then
        ini_set "$TMPD/e4" "$TMPD/e5" SystemSettings "DFMDeviceFrame.White${fps}fps" 1
    else
        cp "$TMPD/e4" "$TMPD/e5"
    fi
    # Game.ini edits
    tuple=$(pad_tuple "$px")
    ini_set "$TMPD/game.ini" "$TMPD/g1" /Script/PerfGear.PerfGearAndroidPadSizeConfig SmallPadConfig "$tuple"
    ini_set "$TMPD/g1" "$TMPD/g2" /Script/PerfGear.PerfGearAndroidPadSizeConfig MidPadConfig "$tuple"
    ini_set "$TMPD/g2" "$TMPD/g3" /Script/PerfGear.PerfGearAndroidPadSizeConfig BigPadConfig "$tuple"
    newtoken=$(enc_container "$KEY" "$MAGIC" "$TMPD/e5")
    [ -n "$newtoken" ] || die "重新加密 Engine.ini 失败"
    awk -v t="$newtoken" '{ if ($0 ~ /^[ \t]*Token[ \t]*=/) { cr=($0 ~ /\r$/)?"\r":""; print "Token = " t cr } else print }' "$TMPD/engine.enc" > "$TMPD/engine.new.enc"
    # verify by decrypting what we are about to write
    m2=$(dec_container "$KEY" "$(ini_get "$TMPD/engine.new.enc" __any__ Token)" "$TMPD/verify.ini") || die "写前回读认证失败"
    [ "$m2" = "$MAGIC" ] || die "写前回读 magic 不一致"
    grep -q "^DFMQualityLevel=$q" "$TMPD/verify.ini" || die "写前回读字段不一致"
    printf '目标: %s / %s / 渲染 %s%% (短边目标 %s px)\n' "$(qual_label "$q")" "$fps 帧" "$ratio" "$px"
    printf '将写入:\n  %s\n  %s\n' "$ENGINE" "$GAME"
    if [ "$yes" -ne 1 ]; then
        printf '这是预演。加 -y 才会写入。\n'
        return 0
    fi
    tx_begin
    set +e
    se=$(stage_file "$TMPD/engine.new.enc" "$ENGINE") || { rollback_all; set -e; exit 1; }
    sg=$(stage_file "$TMPD/g3" "$GAME") || { rollback_all; exit 1; }
    commit_file "$se" "$ENGINE"; WROTE_ENGINE=1
    commit_file "$sg" "$GAME"; WROTE_GAME=1
    # read-back
    r_cat "$ENGINE" > "$TMPD/rb.enc"
    rb=$(dec_container "$KEY" "$(ini_get "$TMPD/rb.enc" __any__ Token)" "$TMPD/rb.ini") || { rollback_all; exit 1; }
    [ "$rb" = "$MAGIC" ] || { rollback_all; exit 1; }
    [ "$(ini_get "$TMPD/rb.ini" DFMPerfGearConfig DFMQualityLevel)" = "$q" ] || { rollback_all; exit 1; }
    [ "$(ini_get "$TMPD/rb.ini" DFMPerfGearConfig DFMFPSLevel)" = "$f" ] || { rollback_all; exit 1; }
    r_cat "$GAME" > "$TMPD/rb.game"
    [ "$(sha256sum "$TMPD/rb.game" | awk '{print $1}')" = "$(sha256sum "$TMPD/g3" | awk '{print $1}')" ] || { rollback_all; exit 1; }
    r_cat "$GAMEPLAY" > "$TMPD/rb.gp"
    [ "$(sha256sum "$TMPD/rb.gp" | awk '{print $1}')" = "$(sha256sum "$TMPD/gameplay.ini" | awk '{print $1}')" ] || { rollback_all; exit 1; }
    tx_commit
    printf '写入并回读校验通过。进游戏后请手动确认档位；实际帧率受设备/显示模式/资源表限制。\n'
}

cmd_defaults() {
    yes=0
    for a in "$@"; do [ "$a" = "-y" ] && yes=1; done
    load_state
    ini_del "$TMPD/engine.ini" "$TMPD/e1" DFMPerfGearConfig "DFMQualityLevel DFMFPSLevel"
    ini_del "$TMPD/e1" "$TMPD/e2" QualityFPSSetting "Unlimit"
    ini_del "$TMPD/e2" "$TMPD/e3" SystemSettings "PerfGear.SmallPadSize DFMDeviceFrame.White90fps DFMDeviceFrame.White120fps DFMDeviceFrame.White144fps DFMDeviceFrame.White165fps DFMDeviceFrame.White185fps"
    ini_del "$TMPD/game.ini" "$TMPD/g1" /Script/PerfGear.PerfGearAndroidPadSizeConfig "SmallPadConfig MidPadConfig BigPadConfig"
    newtoken=$(enc_container "$KEY" "$MAGIC" "$TMPD/e3")
    awk -v t="$newtoken" '{ if ($0 ~ /^[ \t]*Token[ \t]*=/) print "Token = " t; else print }' "$TMPD/engine.enc" > "$TMPD/engine.new.enc"
    dec_container "$KEY" "$(ini_get "$TMPD/engine.new.enc" __any__ Token)" "$TMPD/verify.ini" >/dev/null || die "重置后回读认证失败"
    printf '将删除本工具的覆盖项（保留其余配置与认证信息）。\n'
    if [ "$yes" -ne 1 ]; then printf '这是预演。加 -y 才会写入。\n'; return 0; fi
    tx_begin
    se=$(stage_file "$TMPD/engine.new.enc" "$ENGINE") || { rollback_all; exit 1; }
    sg=$(stage_file "$TMPD/g1" "$GAME") || { rollback_all; exit 1; }
    commit_file "$se" "$ENGINE"
    commit_file "$sg" "$GAME"
    r_cat "$ENGINE" > "$TMPD/rb.enc"
    dec_container "$KEY" "$(ini_get "$TMPD/rb.enc" __any__ Token)" "$TMPD/rb.ini" >/dev/null || { rollback_all; exit 1; }
    tx_commit
    printf '已清除覆盖项，默认档位由游戏下次启动重建。\n'
}

cmd_list() {
    asroot "ls -1 '$BACKUP_ROOT' 2>/dev/null" | grep -E '^[0-9]{8}_' | sort | tail -20
}

cmd_restore() {
    id=${1:-latest}; yes=0
    for a in "$@"; do [ "$a" = "-y" ] && yes=1; done
    if [ "$id" = "latest" ] || [ -z "$id" ]; then
        id=$(asroot "cat '$BACKUP_ROOT/latest' 2>/dev/null" | tr -d '\r\n')
        [ -n "$id" ] || die "没有可恢复的备份"
    fi
    dir="$BACKUP_ROOT/$id"
    r_test "$dir/Engine.ini" || die "备份不存在: $dir"
    load_state
    r_cat "$dir/Engine.ini" > "$TMPD/r.enc"
    dec_container "$KEY" "$(ini_get "$TMPD/r.enc" __any__ Token)" "$TMPD/r.ini" >/dev/null || die "备份与本机认证不匹配"
    r_cat "$dir/Game.ini" > "$TMPD/r.game"
    gp=$(r_sha "$dir/UserGameplay.ini" | awk '{print $1}')
    cur=$(sha256sum "$TMPD/gameplay.ini" | awk '{print $1}')
    [ "$gp" = "$cur" ] || die "UserGameplay.ini 已变化，旧备份的认证上下文不再匹配"
    printf '将恢复 %s\n' "$dir"
    [ "$yes" -eq 1 ] || { printf '这是预演。加 -y 才会写入。\n'; return 0; }
    asroot "umask 077; printf 'PREPARED\n' > '$dir/state'"
    se=$(stage_file "$TMPD/r.enc" "$ENGINE") || die "暂存失败"
    sg=$(stage_file "$TMPD/r.game" "$GAME") || die "暂存失败"
    commit_file "$se" "$ENGINE"; commit_file "$sg" "$GAME"
    r_cat "$ENGINE" > "$TMPD/rb.enc"
    dec_container "$KEY" "$(ini_get "$TMPD/rb.enc" __any__ Token)" "$TMPD/rb.ini" >/dev/null || { rollback_all; exit 1; }
    asroot "printf 'COMMITTED\n' > '$dir/state'"
    asroot "sync"
    printf '已恢复并通过认证回读。\n'
}

cmd_doctor() {
    detect_openssl || true
    echo "=== 环境自检 ==="
    echo "脚本: $0"
    echo "shell: ${0##*/} ; uid=$(id -u 2>/dev/null) ; 时间=$(date 2>/dev/null)"
    echo "PATH: ${PATH:-<empty>}"
    echo "--- 依赖命令 ---"
    for t in openssl awk tr od dd cp mv mkdir rm sha256sum cat base64 sed grep; do
        p=$(command -v "$t" 2>/dev/null)
        printf '  %-10s %s\n' "$t" "${p:-缺失(MISSING)}"
    done
    echo "--- openssl ---"
    if [ -n "$OPENSSL" ]; then
        echo "  使用: $OPENSSL"
        $OPENSSL version 2>&1 | head -1
    else
        echo "  未找到 openssl：无法加解密，脚本不能改配置（只能备份）。"
        echo "  解决：安装 Termux 后执行 pkg install openssl，再在 Termux 里运行本脚本。"
        echo "        或使用带 openssl 的 busybox，并用 DC_OPENSSL 指定。"
    fi
    echo "--- root ---"
    if command -v su >/dev/null 2>&1; then
        u=$(asroot "id -u" 2>&1 | tr -d '\r\n')
        echo "  su -> id -u = ${u:-<无输出>}"
    else
        echo "  系统里没有 su 命令"
    fi
    echo "--- 配置路径 ---"
    for f in "$ENGINE" "$GAME" "$GAMEPLAY" "$GAMEPLAY_ALT"; do
        if r_test "$f"; then echo "  [有] $f"; else echo "  [无] $f"; fi
    done
    echo "--- 游戏库材料 ---"
    if r_test "$LIB"; then
        echo "  libUE4.so: $LIB"
        echo "  sha256: $(r_sha "$LIB" | awk '{print $1}')"
        case "$(r_sha "$LIB" | awk '{print $1}')" in
            686577e4acf4bdd45b113b579d72dd581e9beffb6f6d04d07871b0341ff470c1)
                echo "  -> 已收录该版本（材料偏移 0x1559300 / 0x157d9c0）";;
            *) echo "  -> 未收录；请用 DC_MATERIAL_HEX 或 DC_LIB_OFFSETS 提供";;
        esac
    else
        echo "  找不到 libUE4.so（可用 DC_LIB 指定）"
    fi
    echo "--- 备份根目录 ---"
    echo "  $BACKUP_ROOT"
    asroot "ls -1 '$BACKUP_ROOT' 2>/dev/null | head -5" 2>/dev/null
}

usage() {
    cat <<'EOF'
dfm_perf.sh — 三角洲行动（国服 com.tencent.tmgp.dfm）画质/帧率配置工具

  status                               查看当前档位与认证状态（只读）
  set <画质> <帧率> [比例] [短边] [-y]  写入目标档位（默认预演，-y 才写）
     画质: 0-5 或 流畅/标准/精致/高清/极致/终极
     帧率: 2,7,8,9,10,11 或 60,90,120,144,165,185
     比例: 25-100（默认 100 = 原生 Surface 目标）
     短边: 非 100% 时的渲染基准短边像素（200-8192）
  defaults [-y]                        删除本工具写入的覆盖项（恢复默认）
  list                                 列出备份
  restore [txid|latest] [-y]           还原指定/最近备份
  backup                               仅备份当前配置
  doctor                               环境自检（MT 管理器里先跑这个）

环境变量: DC_SU, DC_DATA_DIR, DC_ENGINE, DC_GAME, DC_GAMEPLAY, DC_LIB,
          DC_BACKUP_ROOT, DC_USER_ID, DC_MATERIAL_HEX, DC_LIB_OFFSETS, DC_LOG
提示: 所有输出都可写入日志——先 export DC_LOG=/sdcard/dfm_perf.log 再执行。
EOF
}

main() {
    if [ -n "${DC_LOG:-}" ]; then
        case "$DC_LOG" in
            */*) mkdir -p "${DC_LOG%/*}" 2>/dev/null;;
        esac
        exec >>"$DC_LOG" 2>&1
        printf '\n===== %s | %s =====\n' "$(date 2>/dev/null)" "$*"
    fi
    cmd=${1:-help}; shift 2>/dev/null || true
    case "$cmd" in
        status)   cmd_status "$@";;
        set)      cmd_set "$@";;
        defaults) cmd_defaults "$@";;
        list)     cmd_list "$@";;
        restore)  cmd_restore "$@";;
        doctor)   cmd_doctor "$@";;
        backup)   TMPD=$(mktemp -d 2>/dev/null); check_root; resolve_paths
                  ab=$(date -u +%Y%m%d_%H%M%S); asroot "umask 077; mkdir -p '$BACKUP_ROOT/$ab'"
                  r_cat "$ENGINE" | asroot "cat > '$BACKUP_ROOT/$ab/Engine.ini'"
                  r_cat "$GAME"   | asroot "cat > '$BACKUP_ROOT/$ab/Game.ini'"
                  r_cat "$GAMEPLAY" | asroot "cat > '$BACKUP_ROOT/$ab/UserGameplay.ini'"
                  msg "已备份到 $BACKUP_ROOT/$ab";;
        help|-h|--help) usage;;
        *) usage; exit 1;;
    esac
}
main "$@"
