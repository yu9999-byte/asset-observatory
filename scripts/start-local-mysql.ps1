$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
# MySQL's Windows native argument handling requires an ASCII runtime path.
$rootHash = [Convert]::ToHexString([Security.Cryptography.SHA256]::HashData([Text.Encoding]::UTF8.GetBytes($projectRoot))).Substring(0,12)
$aliasParent = Join-Path $env:LOCALAPPDATA 'AssetObservatory'
New-Item -ItemType Directory -Force $aliasParent | Out-Null
$runtimeRoot = Join-Path $aliasParent "workspace-$rootHash"
if (Test-Path -LiteralPath $runtimeRoot) {
  $link = Get-Item -LiteralPath $runtimeRoot -Force
  if ($link.LinkType -ne 'Junction' -or $link.Target -ne $projectRoot) { throw 'Runtime alias already exists with a different target.' }
} else {
  New-Item -ItemType Junction -Path $runtimeRoot -Target $projectRoot | Out-Null
}
$projectRoot = $runtimeRoot
$mysqlHome = Join-Path $projectRoot '.tools\mysql-8.4.13-winx64'
$dataDir = Join-Path $projectRoot '.tools\mysql-data'
$config = Join-Path $projectRoot '.tools\mysql-local.ini'
$log = Join-Path $projectRoot '.tools\mysql-local.log'
if (-not (Test-Path (Join-Path $mysqlHome 'bin/mysqld.exe'))) { throw 'Run scripts/bootstrap.ps1 first.' }
if (-not (Test-Path (Join-Path $dataDir 'auto.cnf'))) {
  New-Item -ItemType Directory -Force $dataDir | Out-Null
  & (Join-Path $mysqlHome 'bin/mysqld.exe') "--basedir=$mysqlHome" "--datadir=$dataDir" --initialize-insecure --console
  if ($LASTEXITCODE -ne 0) { throw 'Isolated MySQL initialization failed.' }
}
$mysqlConfigHome = $mysqlHome.Replace('\', '/')
$mysqlConfigData = $dataDir.Replace('\', '/')
$mysqlConfigLog = $log.Replace('\', '/')
$ini = "[mysqld]`r`nbasedir=$mysqlConfigHome`r`ndatadir=$mysqlConfigData`r`nport=3307`r`nbind-address=127.0.0.1`r`nmysqlx=0`r`ncharacter-set-server=utf8mb4`r`ncollation-server=utf8mb4_0900_ai_ci`r`nlog-error=$mysqlConfigLog`r`n"
[IO.File]::WriteAllText($config,$ini,[Text.UTF8Encoding]::new($false))
$existing = Get-NetTCPConnection -State Listen -LocalPort 3307 -ErrorAction SilentlyContinue
if ($existing) {
  foreach ($listener in $existing) {
    $listenerProcess = Get-Process -Id $listener.OwningProcess -ErrorAction Stop
    if ($listenerProcess.Path -ne (Join-Path $mysqlHome 'bin/mysqld.exe')) { throw 'Port 3307 belongs to another process; refusing to change its databases.' }
  }
}
if (-not $existing) {
  Start-Process -FilePath (Join-Path $mysqlHome 'bin/mysqld.exe') -ArgumentList @("--defaults-file=$config") -WorkingDirectory $mysqlHome -WindowStyle Hidden
  $deadline = (Get-Date).AddSeconds(40)
  do { Start-Sleep -Milliseconds 500; $ready = Get-NetTCPConnection -State Listen -LocalPort 3307 -ErrorAction SilentlyContinue } until ($ready -or (Get-Date) -gt $deadline)
  if (-not $ready) { Get-Content -LiteralPath $log -ErrorAction SilentlyContinue; throw 'MySQL did not become ready.' }
}
$sql = "CREATE DATABASE IF NOT EXISTS observatory CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci; CREATE DATABASE IF NOT EXISTS observatory_test CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci; CREATE USER IF NOT EXISTS 'observatory'@'127.0.0.1' IDENTIFIED BY 'local-only-not-for-deployment'; GRANT ALL PRIVILEGES ON observatory.* TO 'observatory'@'127.0.0.1'; GRANT ALL PRIVILEGES ON observatory_test.* TO 'observatory'@'127.0.0.1';"
$sql | & (Join-Path $mysqlHome 'bin/mysql.exe') '--protocol=tcp' '--host=127.0.0.1' '--port=3307' '--user=root'
if ($LASTEXITCODE -ne 0) { throw 'Local isolated MySQL database/user setup failed.' }
Write-Output 'Local isolated MySQL 8.4.13 ready at 127.0.0.1:3307. This no-password root instance is for development tests only.'
