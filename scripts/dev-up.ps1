param(
  [switch]$Detached
)

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot

$composeArgs = @("compose", "up", "--build")
if ($Detached) {
  $composeArgs += "-d"
}

docker @composeArgs
