param(
    [ValidateSet('auto', 'software')]
    [string]$Graphics = 'software'
)

$ErrorActionPreference = 'Stop'
$TripRoot = Split-Path -Parent $PSScriptRoot
$TripSdk = Join-Path $TripRoot '.tooling/android-sdk'
$TripEmulator = Join-Path $TripSdk 'emulator/emulator.exe'
$TripAdb = Join-Path $TripSdk 'platform-tools/adb.exe'
$TripAvdManager = Join-Path $TripSdk 'cmdline-tools/19.0/bin/avdmanager.bat'
$TripImage = 'system-images;android-36;google_apis;x86_64'
$TripAvdName = 'TripTrack_API_36'
$TripSerial = 'emulator-5556'
$TripApk = Join-Path $TripRoot 'app/build/outputs/apk/debug/app-debug.apk'
$TripEnvironment = @{
    JAVA_HOME = Join-Path $TripRoot '.tooling/jdk-17.0.16+8'
    ANDROID_HOME = $TripSdk
    ANDROID_USER_HOME = Join-Path $TripRoot '.tooling/android-user'
    ANDROID_AVD_HOME = Join-Path $TripRoot '.tooling/avd'
}
$TripPrevious = @{}

try {
    foreach ($TripKey in $TripEnvironment.Keys) {
        $TripPrevious[$TripKey] = [Environment]::GetEnvironmentVariable($TripKey, 'Process')
        [Environment]::SetEnvironmentVariable($TripKey, $TripEnvironment[$TripKey], 'Process')
    }
    foreach ($TripFile in @($TripEmulator, $TripAdb, $TripAvdManager, $TripApk)) {
        if (-not (Test-Path -LiteralPath $TripFile)) {
            throw "Missing $TripFile. See docs/emulator.md for setup and build the APK first."
        }
    }
    $TripAcceleration = & $TripEmulator -accel-check 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "Android acceleration is unavailable: $TripAcceleration. Enable Windows Hypervisor Platform and restart Windows if requested."
    }
    New-Item -ItemType Directory -Path $env:ANDROID_AVD_HOME -Force | Out-Null
    $TripAvdConfig = Join-Path $env:ANDROID_AVD_HOME "$TripAvdName.avd/config.ini"
    if (-not (Test-Path -LiteralPath $TripAvdConfig)) {
        Write-Host 'Creating the project virtual phone...'
        'no' | & $TripAvdManager create avd --name $TripAvdName --package $TripImage --device pixel_7
        if ($LASTEXITCODE -ne 0) { throw 'Virtual phone creation failed. Check the Android 36 Google APIs x86_64 image is installed.' }
    }
    & $TripAdb start-server | Out-Host
    $TripDevices = & $TripAdb devices
    if ($TripDevices -match "^$TripSerial\s") {
        $TripRunningName = & $TripAdb -s $TripSerial emu avd name
        if ($TripRunningName -notcontains $TripAvdName) {
            throw "Port 5556 belongs to another emulator. Close it before starting TripTrack."
        }
        Write-Host 'Reusing the running TripTrack virtual phone...'
    } else {
        Write-Host 'Opening the TripTrack virtual phone...'
        # A visible window is intentional: this is the interactive phone requested by the user.
        $TripProcess = Start-Process -FilePath $TripEmulator -ArgumentList @(
            '-avd', $TripAvdName, '-port', '5556', '-gpu', $Graphics,
            '-memory', '3072', '-cores', '4'
        ) -WindowStyle Normal -PassThru `
            -RedirectStandardOutput (Join-Path $TripRoot '.tooling/emulator.stdout.log') `
            -RedirectStandardError (Join-Path $TripRoot '.tooling/emulator.stderr.log')
    }
    $TripDeadline = (Get-Date).AddMinutes(5)
    do {
        if ($TripProcess -and $TripProcess.HasExited) {
            throw 'Emulator exited. Inspect .tooling/emulator.stderr.log and .tooling/emulator.stdout.log.'
        }
        # Offline-device stderr is expected while booting, including in Windows PowerShell 5.1.
        $TripPollPreference = $ErrorActionPreference
        try {
            $ErrorActionPreference = 'SilentlyContinue'
            $TripBoot = & $TripAdb -s $TripSerial shell getprop sys.boot_completed 2>$null
        } finally {
            $ErrorActionPreference = $TripPollPreference
        }
        if ($TripBoot -eq '1') { break }
        if ((Get-Date) -gt $TripDeadline) { throw 'Emulator boot timed out. Inspect .tooling/emulator.stdout.log.' }
        Start-Sleep -Seconds 2
    } while ($true)
    & $TripAdb -s $TripSerial install -r $TripApk
    if ($LASTEXITCODE -ne 0) { throw 'APK installation failed.' }
    & $TripAdb -s $TripSerial shell am start -W -n com.triptrack.app/.MainActivity
    if ($LASTEXITCODE -ne 0) { throw 'TripTrack launch failed.' }
    Write-Host 'TripTrack is open. Close the virtual phone window when finished.'
} finally {
    foreach ($TripKey in $TripPrevious.Keys) {
        [Environment]::SetEnvironmentVariable($TripKey, $TripPrevious[$TripKey], 'Process')
    }
}
