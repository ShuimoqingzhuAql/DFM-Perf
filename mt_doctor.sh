#!/system/bin/sh
# MT 管理器：直接点“执行 / 以 root 方式执行”。作用：环境自检（先跑这个）。
# 会把结果同时写到 /sdcard/dfm_perf.log，MT 里看不到输出时去打开该文件。
unset DC_LOG
HERE=${0%/*}; [ "$HERE" = "$0" ] && HERE=.
LOG=${DC_LOG:-}
if [ -z "$LOG" ]; then
    if [ -d /sdcard ]; then LOG=/sdcard/dfm_perf.log; else LOG=/data/local/tmp/dfm_perf.log; fi
fi
if command -v tee >/dev/null 2>&1; then
    sh "$HERE/dfm_perf.sh" doctor 2>&1 | tee -a "$LOG"
else
    sh "$HERE/dfm_perf.sh" doctor >>"$LOG" 2>&1; cat "$LOG"
fi
