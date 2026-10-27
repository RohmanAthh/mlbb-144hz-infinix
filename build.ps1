# Build: .\build.ps1   Needs: JDK 17+, Android SDK (build-tools 34+, platforms;android-34).
# Signing: set KEYSTORE/KS_PASS/KEY_ALIAS env vars, else uses ~/.android/debug.keystore.
$ErrorActionPreference = 'Stop'
$sdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { "$env:LOCALAPPDATA\Android\Sdk" }
$bt  = (Get-ChildItem "$sdk\build-tools" | Sort-Object Name | Select-Object -Last 1).FullName
$jar = "$sdk\platforms\android-34\android.jar"
$out = "$PSScriptRoot\build"
Remove-Item $out -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory "$out\stubs","$out\classes","$out\dex" | Out-Null

$stubs = Get-ChildItem "$PSScriptRoot\stubs" -Recurse -Filter *.java | ForEach-Object FullName
& javac --release 8 -nowarn -d "$out\stubs" $stubs
if ($LASTEXITCODE) { throw 'javac stubs failed' }
$srcs = Get-ChildItem "$PSScriptRoot\src" -Recurse -Filter *.java | ForEach-Object FullName
& javac -source 8 -target 8 -nowarn -bootclasspath "$jar" -cp "$out\stubs" -d "$out\classes" $srcs
if ($LASTEXITCODE) { throw 'javac src failed' }
$classes = Get-ChildItem "$out\classes" -Recurse -Filter *.class | ForEach-Object FullName
& "$bt\d8.bat" --release --min-api 28 --lib $jar --output "$out\dex" $classes
if ($LASTEXITCODE) { throw 'd8 failed' }

& "$bt\aapt2.exe" compile --dir "$PSScriptRoot\res" -o "$out\res.zip"
& "$bt\aapt2.exe" link -I $jar --manifest "$PSScriptRoot\AndroidManifest.xml" -A "$PSScriptRoot\assets" -o "$out\base.apk" "$out\res.zip"
if ($LASTEXITCODE) { throw 'aapt2 link failed' }
Push-Location "$out\dex"; & jar uf0 "$out\base.apk" classes.dex; Pop-Location

& "$bt\zipalign.exe" -f -p 4 "$out\base.apk" "$out\aligned.apk"
$ks    = if ($env:KEYSTORE)  { $env:KEYSTORE }  else { "$env:USERPROFILE\.android\debug.keystore" }
$pass  = if ($env:KS_PASS)   { $env:KS_PASS }   else { 'android' }
$alias = if ($env:KEY_ALIAS) { $env:KEY_ALIAS } else { 'androiddebugkey' }
& "$bt\apksigner.bat" sign --ks $ks --ks-pass "pass:$pass" --ks-key-alias $alias --out "$PSScriptRoot\MLBB-144Hz-Infinix.apk" "$out\aligned.apk"
if ($LASTEXITCODE) { throw 'sign failed' }
Remove-Item "$PSScriptRoot\MLBB-144Hz-Infinix.apk.idsig" -ErrorAction SilentlyContinue
Write-Host "OK: $PSScriptRoot\MLBB-144Hz-Infinix.apk"


