param(
  [ValidateSet('observatory','observatory_test')][string]$Database = 'observatory',
  [ValidateRange(8080,8090)][int]$PreferredPort = 8080
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
& (Join-Path $PSScriptRoot 'start-local-mysql.ps1')
$jar = Join-Path $projectRoot 'target/asset-observatory-0.1.0.jar'
if (-not (Test-Path -LiteralPath $jar)) { throw 'Build the frontend and Maven package first.' }
$port = $PreferredPort
while (Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue) {
  $port++
  if ($port -gt 8090) { throw 'No free local application port in 8080-8090.' }
}
$env:DATABASE_URL = "jdbc:mysql://127.0.0.1:3307/${Database}?connectionTimeZone=UTC&characterEncoding=UTF-8"
$env:DATABASE_USER = 'observatory'
$env:DATABASE_PASSWORD = 'local-only-not-for-deployment'
$env:APP_BIND = '127.0.0.1'
$env:PORT = "$port"
$env:APP_EXTERNAL = 'false'
$env:APP_SOURCES_ENABLED = 'false'
$stdout = Join-Path $projectRoot ".tools/app-$port.log"
$stderr = Join-Path $projectRoot ".tools/app-$port.error.log"
$process = Start-Process -FilePath (Join-Path $projectRoot '.tools/jdk-21.0.12.1+1/bin/java.exe') -ArgumentList @('-jar',"`"$jar`"") -WorkingDirectory $projectRoot -WindowStyle Hidden -PassThru -RedirectStandardOutput $stdout -RedirectStandardError $stderr
$deadline = (Get-Date).AddSeconds(40)
do {
  if ($process.HasExited) { throw "Application exited; inspect $stdout and $stderr" }
  try { $health = Invoke-RestMethod "http://127.0.0.1:$port/actuator/health" -TimeoutSec 2 } catch { $health = $null }
  if ($health.status -eq 'UP') { break }
  Start-Sleep -Milliseconds 500
} until ((Get-Date) -gt $deadline)
if ($health.status -ne 'UP') { Stop-Process -Id $process.Id; throw "Application did not become ready; inspect $stdout" }
$stateFile = if ($Database -eq 'observatory_test') { '.tools/test-app.json' } else { '.tools/local-app.json' }
@{processId=$process.Id;url="http://127.0.0.1:$port";database=$Database;mode='Local research; sources disabled; not production approved'} | ConvertTo-Json | Set-Content (Join-Path $projectRoot $stateFile)
Write-Output "Local research preview: http://127.0.0.1:$port (sources disabled)"
