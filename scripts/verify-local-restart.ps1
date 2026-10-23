$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$runId = [DateTime]::UtcNow.ToString('yyyyMMddHHmmssfff')
$statePath = Join-Path $projectRoot '.tools/test-app.json'
$state = Get-Content -LiteralPath $statePath -Raw | ConvertFrom-Json
$uri = [Uri]$state.url
if ($state.database -ne 'observatory_test' -or $uri.Scheme -ne 'http' -or $uri.Host -ne '127.0.0.1' -or $uri.Port -lt 8080 -or $uri.Port -gt 8090) {
  throw 'Restart verification requires the managed loopback test database instance.'
}
$appProcess = Get-CimInstance Win32_Process -Filter "ProcessId=$($state.processId)"
$java = Join-Path $projectRoot '.tools/jdk-21.0.12.1+1/bin/java.exe'
$jar = Join-Path $projectRoot 'target/asset-observatory-0.1.0.jar'
$listener = Get-NetTCPConnection -State Listen -LocalAddress '127.0.0.1' -LocalPort $uri.Port
if (-not $appProcess -or $appProcess.ExecutablePath -ne $java -or -not $appProcess.CommandLine.Contains($jar) -or $listener.OwningProcess -ne $state.processId) {
  throw 'Recorded process does not match this project Java executable, jar and listening port.'
}
function Read-Snapshot {
  $workspace = Invoke-RestMethod "$($state.url)/api/workspace" -TimeoutSec 10
  $layout = Invoke-RestMethod "$($state.url)/api/table-preferences/markets" -TimeoutSec 10
  $canonical = [ordered]@{
    watches = @($workspace.watches | Sort-Object kind,key)
    notes = @($workspace.notes | Sort-Object kind,key)
    layout = $layout
  } | ConvertTo-Json -Depth 10 -Compress
  [pscustomobject]@{
    sha256 = [Convert]::ToHexString([Security.Cryptography.SHA256]::HashData([Text.Encoding]::UTF8.GetBytes($canonical)))
    watchCount = @($workspace.watches).Count
    noteCount = @($workspace.notes).Count
    projectRecordCount = @($workspace.notes | Where-Object { $_.projectName -or $_.projectInfo }).Count
    noteVersions = @($workspace.notes | Sort-Object kind,key | ForEach-Object { $_.version })
    layoutVersion = $layout.version
  }
}
$before = Read-Snapshot
if ($before.watchCount -lt 1 -or $before.noteCount -lt 1 -or $before.layoutVersion -lt 1) {
  throw 'Run browser acceptance first to create nonempty test watches, notes and layout.'
}
Stop-Process -Id $state.processId
Wait-Process -Id $state.processId -Timeout 15 -ErrorAction SilentlyContinue
& (Join-Path $PSScriptRoot 'start-local-app.ps1') -Database observatory_test -PreferredPort $uri.Port
$afterState = Get-Content -LiteralPath $statePath -Raw | ConvertFrom-Json
if ($afterState.url -ne $state.url) { throw 'Restart changed the expected test endpoint.' }
$after = Read-Snapshot
$report = [ordered]@{
  checkedAt = [DateTime]::UtcNow.ToString('o')
  database = 'observatory_test'; url = $state.url
  previousProcessId = $state.processId; restartedProcessId = $afterState.processId
  before = $before; after = $after
  equal = ($before.sha256 -eq $after.sha256)
  coverage = 'Application process termination/restart with MySQL kept running; not database crash or production recovery.'
}
$report | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath (Join-Path $projectRoot "docs/evidence/local-restart-verification-$runId.json") -Encoding utf8
$report | ConvertTo-Json -Depth 8
if (-not $report.equal) { throw 'Workspace or table preferences changed across application restart.' }
