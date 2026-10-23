$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$toolsRoot = Join-Path $projectRoot '.tools'
New-Item -ItemType Directory -Force $toolsRoot | Out-Null
$jdkZip = Join-Path $toolsRoot 'jdk.zip'
$jdkUrl = 'https://aka.ms/download-jdk/microsoft-jdk-21.0.12.1-windows-x64.zip'
$jdkHash = '192441a9d27da813bada974bb88b4cf64d37a9589ed37f204374d411ca5ce07f'
if (-not (Test-Path (Join-Path $toolsRoot 'jdk-21.0.12.1+1/bin/java.exe'))) {
  & curl.exe --ssl-revoke-best-effort -fL --retry 2 --max-time 600 -o $jdkZip $jdkUrl
  if ($LASTEXITCODE -ne 0) { throw 'JDK download failed' }
  if ((Get-FileHash $jdkZip -Algorithm SHA256).Hash.ToLowerInvariant() -ne $jdkHash) { throw 'JDK checksum mismatch' }
  Expand-Archive -LiteralPath $jdkZip -DestinationPath $toolsRoot -Force
}
$mavenVersion = '3.9.11'
$mavenZip = Join-Path $toolsRoot 'maven.zip'
$mavenUrl = "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$mavenVersion/apache-maven-$mavenVersion-bin.zip"
if (-not (Test-Path (Join-Path $toolsRoot "apache-maven-$mavenVersion/bin/mvn.cmd"))) {
  & curl.exe --ssl-revoke-best-effort -fL --retry 2 --max-time 180 -o $mavenZip $mavenUrl
  if ($LASTEXITCODE -ne 0) { throw 'Maven download failed' }
  $mavenHash = (& curl.exe --ssl-revoke-best-effort -fsSL "$mavenUrl.sha512").Trim().Split(' ')[0]
  if ($LASTEXITCODE -ne 0 -or (Get-FileHash $mavenZip -Algorithm SHA512).Hash.ToLowerInvariant() -ne $mavenHash.ToLowerInvariant()) { throw 'Maven checksum mismatch' }
  Expand-Archive -LiteralPath $mavenZip -DestinationPath $toolsRoot -Force
}
$mysqlVersion = '8.4.13'
$mysqlHome = Join-Path $toolsRoot "mysql-$mysqlVersion-winx64"
$mysqlZip = Join-Path $toolsRoot 'mysql.zip'
$mysqlUrl = "https://dev.mysql.com/get/Downloads/MySQL-8.4/mysql-$mysqlVersion-winx64.zip"
$mysqlMd5 = '7b94489d5524dea9b27097068bea25a8'
if (-not (Test-Path (Join-Path $mysqlHome 'bin/mysqld.exe'))) {
  if (-not (Test-Path $mysqlZip)) { & curl.exe --ssl-revoke-best-effort -fL --retry 2 --max-time 900 -o $mysqlZip $mysqlUrl; if ($LASTEXITCODE -ne 0) { throw 'MySQL download failed' } }
  if ((Get-FileHash $mysqlZip -Algorithm MD5).Hash.ToLowerInvariant() -ne $mysqlMd5) { throw 'MySQL official checksum mismatch' }
  Expand-Archive -LiteralPath $mysqlZip -DestinationPath $toolsRoot -Force
}
Write-Output "JAVA_HOME=$(Join-Path $toolsRoot 'jdk-21.0.12.1+1')"
Write-Output "MAVEN=$(Join-Path $toolsRoot 'apache-maven-3.9.11/bin/mvn.cmd')"
Write-Output "MYSQL_HOME=$mysqlHome"
