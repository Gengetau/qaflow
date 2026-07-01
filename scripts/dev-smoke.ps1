$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot

function Get-LocalEnvValue {
  param(
    [string]$Name,
    [string]$DefaultValue
  )

  $processValue = [Environment]::GetEnvironmentVariable($Name)
  if ($processValue) {
    return $processValue
  }

  $envFile = Join-Path $repoRoot ".env"
  if (Test-Path -LiteralPath $envFile) {
    $line = Get-Content -LiteralPath $envFile |
      Where-Object { $_ -match "^\s*$([regex]::Escape($Name))\s*=" } |
      Select-Object -First 1
    if ($line) {
      return ($line -split "=", 2)[1].Trim()
    }
  }

  return $DefaultValue
}

function Wait-HttpOk {
  param(
    [string]$Url,
    [int]$Attempts = 30
  )

  for ($attempt = 1; $attempt -le $Attempts; $attempt++) {
    try {
      Invoke-WebRequest -UseBasicParsing -Uri $Url -TimeoutSec 3 | Out-Null
      return
    } catch {
      if ($attempt -eq $Attempts) {
        throw
      }
      Start-Sleep -Seconds 2
    }
  }
}

$apiPort = Get-LocalEnvValue -Name "API_PORT" -DefaultValue "8080"
$webPort = Get-LocalEnvValue -Name "WEB_PORT" -DefaultValue "5173"

Wait-HttpOk "http://localhost:$apiPort/api/health"
Wait-HttpOk "http://localhost:$webPort/"

$loginBody = @{
  email = "owner@example.com"
  password = "password123"
} | ConvertTo-Json

$login = Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:$apiPort/api/auth/login" `
  -ContentType "application/json" `
  -Body $loginBody

if (-not $login.accessToken) {
  throw "Demo login did not return an access token."
}

Write-Host "QAFlow compose smoke check passed."
