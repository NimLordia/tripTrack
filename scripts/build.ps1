param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$Tasks = @('assembleDebug', 'testDebugUnitTest', 'lintDebug')
)

$ErrorActionPreference = 'Stop'
$TripRoot = Split-Path -Parent $PSScriptRoot
$TripPreviousJava = $env:JAVA_HOME
$TripPreviousGradle = $env:GRADLE_USER_HOME
$TripPreviousAndroid = $env:ANDROID_USER_HOME

try {
    $TripLocalJdk = Join-Path $TripRoot '.tooling/jdk-17.0.16+8'
    if (-not $env:JAVA_HOME -and (Test-Path (Join-Path $TripLocalJdk 'bin/java.exe'))) {
        $env:JAVA_HOME = $TripLocalJdk
    }
    if (-not $env:GRADLE_USER_HOME) {
        $env:GRADLE_USER_HOME = Join-Path $TripRoot '.tooling/gradle-user'
    }
    if (-not $env:ANDROID_USER_HOME) {
        $env:ANDROID_USER_HOME = Join-Path $TripRoot '.tooling/android-user'
    }

    Push-Location $TripRoot
    try {
        & (Join-Path $TripRoot 'gradlew.bat') @Tasks
        $TripExitCode = $LASTEXITCODE
    } finally {
        Pop-Location
    }
} finally {
    $env:JAVA_HOME = $TripPreviousJava
    $env:GRADLE_USER_HOME = $TripPreviousGradle
    $env:ANDROID_USER_HOME = $TripPreviousAndroid
}

exit $TripExitCode
