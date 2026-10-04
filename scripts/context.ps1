param(
    [ValidateSet('app', 'recording', 'capture', 'analysis', 'stops', 'storage', 'navigation', 'build', 'later', 'hardware')]
    [string]$Task,
    [switch]$List
)

$ErrorActionPreference = 'Stop'
$TripRoot = Split-Path -Parent $PSScriptRoot
$TripRoutes = [ordered]@{
    app = @('docs/architecture.md')
    recording = @('docs/features/recording.md', 'docs/architecture.md')
    capture = @('docs/features/capture.md', 'docs/features/capture-validation.md')
    analysis = @('docs/features/analysis.md')
    stops = @('docs/features/stops.md')
    storage = @('docs/features/storage.md', 'docs/architecture.md')
    navigation = @('docs/integrations/navigation.md', 'docs/features/storage.md')
    build = @('docs/build.md')
    later = @('docs/later/index.md')
    hardware = @('docs/later/hardware.md')
}

if ($List -or -not $Task) {
    foreach ($TripRoute in $TripRoutes.GetEnumerator()) {
        '{0}: docs/product.md, {1}' -f $TripRoute.Key, ($TripRoute.Value -join ', ')
    }
    return
}

$TripFiles = @('docs/product.md') + $TripRoutes[$Task]
foreach ($TripRelativePath in $TripFiles) {
    $TripPath = Join-Path $TripRoot $TripRelativePath
    if (-not (Test-Path -LiteralPath $TripPath -PathType Leaf)) {
        throw "Missing context file: $TripRelativePath"
    }
    Write-Output "# File: $TripRelativePath"
    Get-Content -LiteralPath $TripPath -Raw -Encoding utf8
}
