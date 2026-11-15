# 构建脚本（Windows / PowerShell 7）
# 依赖：JDK（javac/keytool）、Android SDK build-tools（aapt2/d8/apksigner/zipalign）、platforms/android-35/android.jar
# 用法：powershell -ExecutionPolicy Bypass -File build.ps1
#
# 注意：aapt2/d8 对含中文的路径可能报 “failed to open directory”，
# 因此先把源码复制到纯 ASCII 的临时目录再构建，最后把 APK 拷回 out\。
$ErrorActionPreference = 'Stop'

$SDK = 'E:\Software\Android_Studio_SDK'
$BT = Join-Path $SDK 'build-tools\36.1.0'
$AJAR = Join-Path $SDK 'platforms\android-35\android.jar'
$JAVA_HOME = 'E:\Software\Android Studio\jbr'
$env:JAVA_HOME = $JAVA_HOME
$JAVAC = Join-Path $JAVA_HOME 'bin\javac.exe'
$KEYTOOL = Join-Path $JAVA_HOME 'bin\keytool.exe'
$SEVENZ = 'C:\Windows\system32\7z.exe'

$ROOT = $PSScriptRoot
$STAGE = 'E:\dfm_apk_build'
$BUILD = Join-Path $STAGE 'build'
$OUT = Join-Path $ROOT 'out'

Write-Host "== 0. 复制到 ASCII 路径 =="
if (Test-Path $STAGE) { Remove-Item $STAGE -Recurse -Force }
New-Item -ItemType Directory -Force $STAGE | Out-Null
Copy-Item (Join-Path $ROOT 'src') $STAGE -Recurse
Copy-Item (Join-Path $ROOT 'stubs') $STAGE -Recurse
Copy-Item (Join-Path $ROOT 'res') $STAGE -Recurse
Copy-Item (Join-Path $ROOT 'assets') $STAGE -Recurse
Copy-Item (Join-Path $ROOT 'AndroidManifest.xml') $STAGE
New-Item -ItemType Directory -Force $BUILD, $OUT | Out-Null

Write-Host "== 1. 编译 Xposed 占位类（只用于编译，不进 APK） =="
$stubOut = Join-Path $BUILD 'stubs'
New-Item -ItemType Directory -Force $stubOut | Out-Null
$stubFiles = Get-ChildItem (Join-Path $STAGE 'stubs\de') -Recurse -Filter *.java | ForEach-Object FullName
& $JAVAC -encoding UTF-8 -source 8 -target 8 -nowarn -Xlint:-options -bootclasspath $AJAR -d $stubOut $stubFiles 2>&1 | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'stub 编译失败' }

Write-Host "== 2. 编译模块源码 =="
$classes = Join-Path $BUILD 'classes'
New-Item -ItemType Directory -Force $classes | Out-Null
$srcFiles = Get-ChildItem (Join-Path $STAGE 'src\io') -Recurse -Filter *.java | ForEach-Object FullName
& $JAVAC -encoding UTF-8 -source 8 -target 8 -nowarn -Xlint:-options -bootclasspath $AJAR -classpath $stubOut -d $classes $srcFiles 2>&1 | Out-Null
if ($LASTEXITCODE -ne 0) { throw '源码编译失败' }

Write-Host "== 3. 资源编译与链接 =="
$resZip = Join-Path $BUILD 'res.zip'
& (Join-Path $BT 'aapt2.exe') compile --dir (Join-Path $STAGE 'res') -o $resZip
if ($LASTEXITCODE -ne 0) { throw 'aapt2 compile 失败' }
$base = Join-Path $BUILD 'base.apk'
& (Join-Path $BT 'aapt2.exe') link -o $base -I $AJAR --manifest (Join-Path $STAGE 'AndroidManifest.xml') -A (Join-Path $STAGE 'assets') $resZip
if ($LASTEXITCODE -ne 0) { throw 'aapt2 link 失败' }

Write-Host "== 4. d8 生成 classes.dex =="
$dex = Join-Path $BUILD 'dex'
New-Item -ItemType Directory -Force $dex | Out-Null
$classFiles = Get-ChildItem $classes -Recurse -Filter *.class | ForEach-Object FullName
& (Join-Path $BT 'd8.bat') --release --min-api 26 --lib $AJAR --output $dex @classFiles 2>&1 | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'd8 失败' }

Write-Host "== 5. 组装 APK =="
$unsigned = Join-Path $BUILD 'app.unsigned.apk'
Copy-Item $base $unsigned -Force
Push-Location $dex
& $SEVENZ a -tzip $unsigned 'classes.dex' | Out-Null
Pop-Location
$aligned = Join-Path $BUILD 'app.aligned.apk'
& (Join-Path $BT 'zipalign.exe') -f 4 $unsigned $aligned
if ($LASTEXITCODE -ne 0) { throw 'zipalign 失败' }

Write-Host "== 6. 签名 =="
$ks = Join-Path $ROOT 'dfmperf.keystore'
if (-not (Test-Path $ks)) {
    & $KEYTOOL -genkeypair -keystore $ks -alias dfmperf -keyalg RSA -keysize 2048 -validity 10000 `
        -storepass dfmperf -keypass dfmperf -dname "CN=DFM Perf, OU=Reverse, O=Open, L=NA, S=NA, C=CN"
    if ($LASTEXITCODE -ne 0) { throw 'keystore 生成失败' }
}
$final = Join-Path $OUT 'DFM-Perf-v2.8.apk'
& (Join-Path $BT 'apksigner.bat') sign --ks $ks --ks-pass pass:dfmperf --key-pass pass:dfmperf --out $final $aligned
if ($LASTEXITCODE -ne 0) { throw 'apksigner 失败' }
& (Join-Path $BT 'apksigner.bat') verify --print-certs $final

Write-Host ""
Write-Host "== 完成 =="
Write-Host ("输出: " + $final + "  (" + [math]::Round((Get-Item $final).Length / 1024.0, 1) + " KB)")
