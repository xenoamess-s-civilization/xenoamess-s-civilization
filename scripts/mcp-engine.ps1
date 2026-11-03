param([string]$Session = 'src/xenoamess_chess/target/automation-session.json')
$ErrorActionPreference = 'Stop'
[Console]::InputEncoding = [Text.UTF8Encoding]::new($false)
[Console]::OutputEncoding = [Text.UTF8Encoding]::new($false)
$OutputEncoding = [Console]::OutputEncoding
$repoRoot = Split-Path -Parent $PSScriptRoot
Push-Location -LiteralPath $repoRoot
try {
    $classPath = @('src/engine-mcp/target/classes','src/engine-mcp/target/dependency/*') -join [IO.Path]::PathSeparator
    if (-not (Test-Path -LiteralPath 'src/engine-mcp/target/dependency')) { throw 'Build chess before starting the MCP adapter.' }
    & java -cp $classPath com.xenoamess.cyan_potion.mcp.McpServer $Session
    exit $LASTEXITCODE
} finally { Pop-Location }
