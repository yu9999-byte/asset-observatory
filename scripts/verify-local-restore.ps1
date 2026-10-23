param([ValidateSet('observatory_test')][string]$Database = 'observatory_test')
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$rootHash = [Convert]::ToHexString([Security.Cryptography.SHA256]::HashData([Text.Encoding]::UTF8.GetBytes($projectRoot))).Substring(0,12)
$runtimeRoot = Join-Path $env:LOCALAPPDATA "AssetObservatory/workspace-$rootHash"
$mysqlBin = Join-Path $runtimeRoot '.tools/mysql-8.4.13-winx64/bin'
if (-not (Test-Path -LiteralPath (Join-Path $mysqlBin 'mysqldump.exe'))) { throw 'Start the isolated local MySQL first.' }
$runId = [DateTime]::UtcNow.ToString('yyyyMMddHHmmssfff')
$restoreDb = "observatory_restore_$runId"
$backupDir = Join-Path $projectRoot "backups/restore-$runId"
New-Item -ItemType Directory -Path $backupDir | Out-Null
$backup = Join-Path $backupDir 'before.sql'
$restored = Join-Path $backupDir 'after.sql'
$connection = @('--protocol=tcp','--host=127.0.0.1','--port=3307','--user=root','--default-character-set=utf8mb4')
$dumpOptions = @('--single-transaction','--no-tablespaces','--set-gtid-purged=OFF','--skip-comments','--skip-dump-date','--skip-extended-insert','--order-by-primary')
$fixtureOwner = "restore-drill-$runId"
$fixtureTime = [DateTime]::UtcNow.ToString('o')
$fixtureSql = "INSERT INTO watches(owner_id,kind,entity_key,created_at) VALUES('$fixtureOwner','asset','LINK','$fixtureTime'); INSERT INTO notes(owner_id,kind,entity_key,body,visible,version,updated_at,project_name,project_info) VALUES('$fixtureOwner','asset','LINK',CONVERT(0xE7A094E7A9B6F09F94AC USING utf8mb4),false,3,'$fixtureTime',CONVERT(0xE7A094E7A9B6E9A1B9E79BAE20F09F94AC USING utf8mb4),'Synthetic user context with <script>inert</script>');"
$fixtureConfig = '{"hidden":["type"],"order":["balance","address","type","actions"],"sortKey":"balance","desc":true,"pageSize":30}'
$fixtureSql += " INSERT INTO table_preferences(owner_id,table_id,config,version,updated_at) VALUES('$fixtureOwner','holders','$fixtureConfig',9,'$fixtureTime');"
$fixturePolicy = [ordered]@{
  provider = 'restore-test-only'; dataset = 'holders'; revision = $runId; audience = 'personal'
  evidenceUrl = 'https://example.org/restore-test-fixture'; evidenceSha256 = ('a' * 64)
  expiresAt = [DateTime]::UtcNow.AddDays(1).ToString('yyyy-MM-ddTHH:mm:ssZ'); retentionDays = 30
} | ConvertTo-Json -Compress
$policyFingerprint = [Convert]::ToHexString([Security.Cryptography.SHA256]::HashData([Text.Encoding]::UTF8.GetBytes($fixturePolicy))).ToLowerInvariant()
$maxUint256 = '115792089237316195423570985008687907853269984665640564039457584007913129639935'
$fixtureSql += @"
 INSERT INTO source_policies(id,provider,dataset,revision,audience,evidence_url,evidence_sha256,expires_at,retention_days,enabled)
 VALUES('$fixtureOwner','restore-test-only','holders','$runId','personal','https://example.org/restore-test-fixture',REPEAT('a',64),DATE_ADD(UTC_TIMESTAMP(),INTERVAL 1 DAY),30,false);
 INSERT INTO ingest_streams(policy_id,asset_id,chain_id,contract_address,decimals_value,checkpoint,version,policy_fingerprint)
 VALUES('$fixtureOwner','LINK','ethereum','0x514910771af9ca656af840dff83e8264ecf986ca',18,'cursor:restore-next',2,'$policyFingerprint');
 SET @restore_stream_id=LAST_INSERT_ID();
 INSERT INTO ingest_batches(stream_id,observation_key,payload_sha256,content_sha256,source_at,fetched_at,persisted_at,coverage,row_count,checkpoint_before,checkpoint_after,policy_evidence)
 VALUES(@restore_stream_id,'test-only-restore-observation',REPEAT('b',64),REPEAT('c',64),NULL,'2026-10-21 00:00:00.123456','2026-10-21 00:00:01.654321','Synthetic restore fixture; one page; continuity unknown',2,NULL,'cursor:restore-next','$fixturePolicy');
 SET @restore_batch_id=LAST_INSERT_ID();
 INSERT INTO holder_observations(batch_id,address,raw_quantity) VALUES
 (@restore_batch_id,'0x0000000000000000000000000000000000000001','$maxUint256'),
 (@restore_batch_id,'0x0000000000000000000000000000000000000002','0');
"@
$fixtureSql = "START TRANSACTION; $fixtureSql COMMIT;"
& (Join-Path $mysqlBin 'mysql.exe') @connection $Database "--execute=$fixtureSql"
if ($LASTEXITCODE -ne 0) { throw 'Test-only recovery fixture creation failed.' }
& (Join-Path $mysqlBin 'mysqldump.exe') @connection @dumpOptions "--result-file=$backup" $Database
if ($LASTEXITCODE -ne 0) { throw 'Source dump failed.' }
& (Join-Path $mysqlBin 'mysql.exe') @connection "--execute=CREATE DATABASE $restoreDb CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci"
if ($LASTEXITCODE -ne 0) { throw 'Restore database creation failed.' }
# Copy SQL bytes directly; shell redirection can silently change Unicode encodings.
$start = [Diagnostics.ProcessStartInfo]::new((Join-Path $mysqlBin 'mysql.exe'))
$start.UseShellExecute = $false
$start.CreateNoWindow = $true
$start.RedirectStandardInput = $true
foreach ($argument in ($connection + @($restoreDb))) { $start.ArgumentList.Add($argument) }
$process = [Diagnostics.Process]::Start($start)
$stream = [IO.File]::OpenRead($backup)
try { $stream.CopyTo($process.StandardInput.BaseStream) } finally { $stream.Dispose(); $process.StandardInput.Close() }
$process.WaitForExit()
if ($process.ExitCode -ne 0) { throw 'Restore import failed.' }
& (Join-Path $mysqlBin 'mysqldump.exe') @connection @dumpOptions "--result-file=$restored" $restoreDb
if ($LASTEXITCODE -ne 0) { throw 'Restored database dump failed.' }
$beforeHash = (Get-FileHash -LiteralPath $backup -Algorithm SHA256).Hash
$afterHash = (Get-FileHash -LiteralPath $restored -Algorithm SHA256).Hash
$rowCounts = & (Join-Path $mysqlBin 'mysql.exe') @connection --batch --skip-column-names $restoreDb '--execute=SELECT ''watches'',COUNT(*) FROM watches UNION ALL SELECT ''notes'',COUNT(*) FROM notes UNION ALL SELECT ''table_preferences'',COUNT(*) FROM table_preferences UNION ALL SELECT ''source_policies'',COUNT(*) FROM source_policies UNION ALL SELECT ''ingest_streams'',COUNT(*) FROM ingest_streams UNION ALL SELECT ''ingest_batches'',COUNT(*) FROM ingest_batches UNION ALL SELECT ''holder_observations'',COUNT(*) FROM holder_observations UNION ALL SELECT ''migrations'',COUNT(*) FROM flyway_schema_history'
if ($LASTEXITCODE -ne 0) { throw 'Restore verification query failed.' }
$verifySql = @"
SELECT COUNT(*) FROM ingest_streams s
 JOIN source_policies p ON p.id=s.policy_id
 JOIN ingest_batches b ON b.stream_id=s.id
 JOIN holder_observations h ON h.batch_id=b.id
 WHERE p.id='$fixtureOwner' AND p.enabled=false AND s.version=2
 AND s.checkpoint='cursor:restore-next' AND s.lease_token IS NULL AND s.lease_until IS NULL
 AND s.policy_fingerprint='$policyFingerprint' AND b.observation_key='test-only-restore-observation'
 AND b.source_at IS NULL AND b.fetched_at='2026-10-21 00:00:00.123456'
 AND b.persisted_at='2026-10-21 00:00:01.654321' AND b.row_count=2
 AND b.checkpoint_before IS NULL AND b.checkpoint_after=s.checkpoint
 AND JSON_EXTRACT(b.policy_evidence,'$')=CAST('$fixturePolicy' AS JSON)
 AND ((h.address='0x0000000000000000000000000000000000000001' AND h.raw_quantity='$maxUint256')
 OR (h.address='0x0000000000000000000000000000000000000002' AND h.raw_quantity='0'));
"@
$fixtureRows = & (Join-Path $mysqlBin 'mysql.exe') @connection --batch --skip-column-names $restoreDb "--execute=$verifySql"
if ($LASTEXITCODE -ne 0) { throw 'History restore verification query failed.' }
$journalVerified = "$fixtureRows".Trim() -eq '2'
$projectSql = "SELECT COUNT(*) FROM notes WHERE owner_id='$fixtureOwner' AND kind='asset' AND entity_key='LINK' AND body=CONVERT(0xE7A094E7A9B6F09F94AC USING utf8mb4) AND project_name=CONVERT(0xE7A094E7A9B6E9A1B9E79BAE20F09F94AC USING utf8mb4) AND project_info='Synthetic user context with <script>inert</script>' AND visible=false AND version=3 AND updated_at='$fixtureTime';"
$projectRows = & (Join-Path $mysqlBin 'mysql.exe') @connection --batch --skip-column-names $restoreDb "--execute=$projectSql"
if ($LASTEXITCODE -ne 0) { throw 'Project record restore query failed.' }
$projectVerified = "$projectRows".Trim() -eq '1'
$report = [ordered]@{
  checkedAt = [DateTime]::UtcNow.ToString('o'); sourceDatabase = $Database; restoreDatabase = $restoreDb
  engine = 'MySQL 8.4.13 local isolated development instance'; schemaAndDataEqual = ($beforeHash -eq $afterHash)
  beforeSha256 = $beforeHash; afterSha256 = $afterHash; restoredRowCounts = @($rowCounts)
  testFixtureOwner = $fixtureOwner; fixturePurpose = 'Synthetic test-only watch, hidden Unicode project/name/info/note v3, layout v9, disabled policy and holder journal with two balances; not source data or collection authorization.'
  projectRecordVerified = $projectVerified
  holderJournalVerified = $journalVerified
  holderJournalChecks = @('uint256 maximum and zero as exact strings','NULL source time','UTC microsecond timestamps','committed checkpoint and version','no active lease','immutable JSON policy evidence','disabled fixture policy')
  backupPath = $backup; sourceOverwritten = $false
  coverage = 'Local consistent dump and independent restore; not container deployment, production recovery, or live-source reconciliation.'
}
$evidence = Join-Path $projectRoot "docs/evidence/mysql-restore-verification-$runId.json"
$report | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath $evidence -Encoding utf8
$report | ConvertTo-Json -Depth 5
if (-not $report.schemaAndDataEqual) { throw 'Restored schema/data differs from source dump.' }
if (-not $journalVerified) { throw 'Restored holder journal fixture differs from expected fields.' }
if (-not $projectVerified) { throw 'Restored project record differs from expected fields.' }
