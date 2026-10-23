$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$rootHash = [Convert]::ToHexString([Security.Cryptography.SHA256]::HashData([Text.Encoding]::UTF8.GetBytes($projectRoot))).Substring(0,12)
$runtimeRoot = Join-Path $env:LOCALAPPDATA "AssetObservatory/workspace-$rootHash"
$mysql = Join-Path $runtimeRoot '.tools/mysql-8.4.13-winx64/bin/mysql.exe'
if (-not (Test-Path -LiteralPath $mysql)) { throw 'Start the isolated local MySQL first.' }
$runId = [DateTime]::UtcNow.ToString('yyyyMMddHHmmssfff')
$migrationDb = "observatory_migration_$runId"
$connection = @('--protocol=tcp','--host=127.0.0.1','--port=3307','--user=root','--default-character-set=utf8mb4','--batch','--skip-column-names')
function Invoke-TestSql([string]$sql) {
  $rows = & $mysql @connection $migrationDb "--execute=$sql"
  if ($LASTEXITCODE -ne 0) { throw 'Isolated migration SQL failed.' }
  return $rows
}
function Snapshot-Hash {
  $rows = Invoke-TestSql "SELECT JSON_OBJECT('owner',owner_id,'kind',kind,'key',entity_key,'body',body,'visible',visible+0,'version',version,'updatedAt',updated_at) FROM notes ORDER BY owner_id,kind,entity_key;"
  return [Convert]::ToHexString([Security.Cryptography.SHA256]::HashData([Text.Encoding]::UTF8.GetBytes(($rows -join "`n")))).ToLowerInvariant()
}
& $mysql @connection "--execute=CREATE DATABASE $migrationDb CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"
if ($LASTEXITCODE -ne 0) { throw 'Could not create a new isolated migration database.' }
$migrations = Get-ChildItem -LiteralPath (Join-Path $projectRoot 'src/main/resources/db/migration') -Filter 'V*.sql' | Sort-Object Name
$migrationHashes = @()
foreach ($file in $migrations | Where-Object {$_.Name -match '^V[1-5]__'}) {
  $null = Invoke-TestSql (Get-Content -LiteralPath $file.FullName -Raw)
  $migrationHashes += @{file=$file.Name;sha256=(Get-FileHash -LiteralPath $file.FullName -Algorithm SHA256).Hash.ToLowerInvariant()}
}
$fixture = @"
INSERT INTO notes(owner_id,kind,entity_key,body,visible,version,updated_at) VALUES
('migration-fixture','asset','JCT',CONVERT(0xE7A094E7A9B6F09F94AC USING utf8mb4),true,7,'2026-10-21T00:00:00Z'),
('migration-fixture','address','0xa68f0255c0eeb35e3cf558d58cfbbfa8811b9b84','Independent address note',false,3,'2026-10-21T01:00:00Z');
"@
$null = Invoke-TestSql $fixture
$beforeHash = Snapshot-Hash
$upgrade = $migrations | Where-Object {$_.Name -eq 'V6__project_records.sql'}
if (@($upgrade).Count -ne 1) { throw 'Expected the exact V6 project migration.' }
$null = Invoke-TestSql (Get-Content -LiteralPath $upgrade.FullName -Raw)
$migrationHashes += @{file=$upgrade.Name;sha256=(Get-FileHash -LiteralPath $upgrade.FullName -Algorithm SHA256).Hash.ToLowerInvariant()}
$afterHash = Snapshot-Hash
$emptyRows = Invoke-TestSql "SELECT COUNT(*) FROM notes WHERE project_name='' AND project_info='';"
$null = Invoke-TestSql "UPDATE notes SET project_name=CONVERT(0xE7A094E7A9B6E9A1B9E79BAE20F09F94AC USING utf8mb4),project_info='Synthetic user context',version=version+1 WHERE owner_id='migration-fixture' AND kind='asset' AND entity_key='JCT' AND version=7;"
$updatedRows = Invoke-TestSql "SELECT COUNT(*) FROM notes WHERE owner_id='migration-fixture' AND kind='asset' AND entity_key='JCT' AND project_name=CONVERT(0xE7A094E7A9B6E9A1B9E79BAE20F09F94AC USING utf8mb4) AND project_info='Synthetic user context' AND body=CONVERT(0xE7A094E7A9B6F09F94AC USING utf8mb4) AND visible=true AND version=8 AND updated_at='2026-10-21T00:00:00Z';"
$report = [ordered]@{
  checkedAt=[DateTime]::UtcNow.ToString('o'); database=$migrationDb
  legacyNotes=2; originalColumnsBeforeSha256=$beforeHash; originalColumnsAfterSha256=$afterHash
  originalColumnsPreserved=($beforeHash -eq $afterHash); emptyDefaultCount=[int]$emptyRows
  newUnicodeProjectFieldsVerified=("$updatedRows".Trim() -eq '1'); migrations=$migrationHashes
  sourceDatabasesModified=$false
  coverage='Exact V1-V5 SQL in a new isolated schema, two synthetic legacy notes, exact V6 SQL and Unicode project update; not application traffic during upgrade, public deployment or production rollback.'
}
$evidence = Join-Path $projectRoot "docs/evidence/project-records-migration-$runId.json"
$report | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath $evidence -Encoding utf8
$report | ConvertTo-Json -Depth 5
if (-not $report.originalColumnsPreserved -or $report.emptyDefaultCount -ne 2 -or -not $report.newUnicodeProjectFieldsVerified) {
  throw 'Project migration verification failed; inspect retained isolated schema and report.'
}
