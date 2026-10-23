param(
  [string]$Database = $(if ($env:DATABASE_NAME) { $env:DATABASE_NAME } else { 'observatory' }),
  [string]$HostName = $(if ($env:DATABASE_HOST) { $env:DATABASE_HOST } else { '127.0.0.1' }),
  [int]$Port = $(if ($env:DATABASE_PORT) { [int]$env:DATABASE_PORT } else { 3306 }),
  [string]$User = $(if ($env:DATABASE_USER) { $env:DATABASE_USER } else { 'observatory' }),
  [string]$OutputDirectory = (Join-Path (Split-Path $PSScriptRoot -Parent) 'backups')
)
$ErrorActionPreference = 'Stop'
if ([string]::IsNullOrWhiteSpace($env:DATABASE_PASSWORD)) { throw 'Set DATABASE_PASSWORD in the process environment; it is never accepted as a command-line argument.' }
$dump = Get-Command mysqldump -ErrorAction SilentlyContinue
if (-not $dump) { throw 'mysqldump must be installed and available on PATH.' }
New-Item -ItemType Directory -Force -Path $OutputDirectory | Out-Null
$stamp = (Get-Date).ToUniversalTime().ToString('yyyyMMddTHHmmssZ')
$path = Join-Path $OutputDirectory "$Database-$stamp.sql"
$env:MYSQL_PWD = $env:DATABASE_PASSWORD
try {
  & $dump.Source --protocol=tcp --host=$HostName --port=$Port --user=$User --single-transaction --routines --events --triggers --hex-blob --set-gtid-purged=OFF $Database | Out-File -LiteralPath $path -Encoding utf8
  if ($LASTEXITCODE -ne 0) { throw "mysqldump failed with exit code $LASTEXITCODE." }
} finally { Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue }
$hash = (Get-FileHash -Algorithm SHA256 -LiteralPath $path).Hash
[pscustomobject]@{database=$Database;createdAt=(Get-Date).ToUniversalTime().ToString('o');path=$path;sha256=$hash} | ConvertTo-Json | Set-Content -LiteralPath "$path.json" -Encoding utf8
Write-Output "Backup created: $path"
