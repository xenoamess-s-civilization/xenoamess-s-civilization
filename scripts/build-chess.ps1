param([switch]$SkipTests)
$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
Push-Location -LiteralPath $repoRoot
try {
    $buildArgs = @('-B', '-pl', 'src/xenoamess_chess', '-am', 'package', 'org.apache.maven.plugins:maven-dependency-plugin:3.8.1:copy-dependencies', '-Dmaven=3.8.7', '-Dmaven.javadoc.skip=true', '-Dmaven.source.skip=true', '-DincludeScope=runtime', '-DoutputDirectory=target/dependency', '-Dtest=ChessGameTest,ChessSessionTest,McpServerTest', '-Dsurefire.failIfNoSpecifiedTests=false')
    if ($SkipTests) { $buildArgs += '-DskipTests' }
    & mvn @buildArgs
    if ($LASTEXITCODE -ne 0) { throw 'Chess build failed.' }
} finally { Pop-Location }
