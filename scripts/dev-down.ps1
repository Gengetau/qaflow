param(
  [switch]$Volumes
)

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot

$composeArgs = @("compose", "down")
if ($Volumes) {
  $composeArgs += "--volumes"
}

docker @composeArgs
