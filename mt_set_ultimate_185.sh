#!/system/bin/sh
# MT 管理器：直接点“执行 / 以 root 方式执行”。作用：写入 终极 + 185 + 原生分辨率。
# 想改分辨率比例：把下面的 set 参数改成例如   set 5 11 67 1080 -y
unset DC_LOG
HERE=${0%/*}; [ "$HERE" = "$0" ] && HERE=.
LOG=${DC_LOG:-}
if [ -z "$LOG" ]; then
    if [ -d /sdcard ]; then LOG=/sdcard/dfm_perf.log; else LOG=/data/local/tmp/dfm_perf.log; fi
fi
if command -v tee >/dev/null 2>&1; then
    sh "$HERE/dfm_perf.sh" set 5 11 -y 2>&1 | tee -a "$LOG"
else
    sh "$HERE/dfm_perf.sh" set 5 11 -y >>"$LOG" 2>&1; cat "$LOG"
fi
