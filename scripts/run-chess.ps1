param([switch]$Headless, [switch]$Build, [string]$Session = 'src/xenoamess_chess/target/automation-session.json')
$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
if ($Build) { & (Join-Path $PSScriptRoot 'build-chess.ps1') }
Push-Location -LiteralPath $repoRoot
try {
    $classPath = @('src/xenoamess_chess/target/classes','src/engine-mcp/target/classes','src/base/target/classes','src/xenoamess_chess/target/dependency/*') -join [IO.Path]::PathSeparator
    if (-not (Test-Path -LiteralPath 'src/xenoamess_chess/target/dependency')) { throw 'Run scripts/build-chess.ps1 first.' }
    $javaArgs = @('-cp', $classPath, 'com.xenoamess.cyan_potion.xenoamess_chess.ChessDemo', '--session', $Session)
    if ($Headless) { $javaArgs += '--headless' }
    & java @javaArgs
    exit $LASTEXITCODE
} finally { Pop-Location }
