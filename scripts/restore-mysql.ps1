param(
  [Parameter(Mandatory=$true)][string]$DumpPath,
  [string]$Database = $(if ($env:DATABASE_NAME) { $env:DATABASE_NAME } else { 'observatory' }),
  [string]$HostName = $(if ($env:DATABASE_HOST) { $env:DATABASE_HOST } else { '127.0.0.1' }),
  [int]$Port = $(if ($env:DATABASE_PORT) { [int]$env:DATABASE_PORT } else { 3306 }),
  [string]$User = $(if ($env:DATABASE_USER) { $env:DATABASE_USER } else { 'observatory' }),
  [switch]$ConfirmRestore
)
$ErrorActionPreference = 'Stop'
if (-not $ConfirmRestore) { throw 'Restore is destructive. Re-run with -ConfirmRestore after stopping the application and checking the dump hash.' }
if (-not (Test-Path -LiteralPath $DumpPath -PathType Leaf)) { throw "Dump not found: $DumpPath" }
if ([string]::IsNullOrWhiteSpace($env:DATABASE_PASSWORD)) { throw 'Set DATABASE_PASSWORD in the process environment; it is never accepted as a command-line argument.' }
$mysql = Get-Command mysql -ErrorAction SilentlyContinue
if (-not $mysql) { throw 'mysql client must be installed and available on PATH.' }
$env:MYSQL_PWD = $env:DATABASE_PASSWORD
try {
  Get-Content -Raw -LiteralPath $DumpPath | & $mysql.Source --protocol=tcp --host=$HostName --port=$Port --user=$User --database=$Database --binary-mode
  if ($LASTEXITCODE -ne 0) { throw "mysql restore failed with exit code $LASTEXITCODE." }
} finally { Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue }
Write-Output "Restore completed into database '$Database'. Run Flyway/app health checks before accepting traffic."
