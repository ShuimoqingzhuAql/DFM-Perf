#!/system/bin/sh
# MT 管理器：直接对本文件点“执行 / 以 root 方式执行”即可（不需要参数）。
# 作用：只读查看当前画质、帧率与认证状态。
unset DC_LOG
HERE=${0%/*}; [ "$HERE" = "$0" ] && HERE=.
LOG=${DC_LOG:-}
if [ -z "$LOG" ]; then
    if [ -d /sdcard ]; then LOG=/sdcard/dfm_perf.log; else LOG=/data/local/tmp/dfm_perf.log; fi
fi
if command -v tee >/dev/null 2>&1; then
    sh "$HERE/dfm_perf.sh" status 2>&1 | tee -a "$LOG"
else
    sh "$HERE/dfm_perf.sh" status >>"$LOG" 2>&1; cat "$LOG"
fi
