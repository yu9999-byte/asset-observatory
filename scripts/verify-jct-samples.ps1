param()
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$contract = '0xc477b6dfd26ec2460b3b92de18837fd476ea7549'
$address = '0xa68f0255c0eeb35e3cf558d58cfbbfa8811b9b84'
$probes = [Collections.Generic.List[object]]::new()
function Read-Probe([string]$Name, [string]$Url) {
  $body = Join-Path $projectRoot ".tools/$Name.json"
  $headers = Join-Path $projectRoot ".tools/$Name.headers"
  if (-not (Test-Path -LiteralPath $body) -or -not (Test-Path -LiteralPath $headers)) {
    throw "Missing captured public API response: $Name"
  }
  $statuses = [regex]::Matches((Get-Content -LiteralPath $headers -Raw), '(?m)^HTTP/\S+ (\d{3})')
  if ($statuses.Count -eq 0 -or $statuses[-1].Groups[1].Value -ne '200') { throw "Non-200 source response: $Name" }
  $probes.Add([ordered]@{
    name = $Name; url = $Url; httpStatus = 200
    fetchedAt = (Get-Item -LiteralPath $body).LastWriteTimeUtc.ToString('o')
    fetchedAtBasis = 'Local response file modification time immediately after curl completed'
    bodySha256 = (Get-FileHash -LiteralPath $body -Algorithm SHA256).Hash.ToLowerInvariant()
    transport = 'HTTPS curl through pre-existing local Windows network proxy 127.0.0.1:7897; TLS verification enabled'
  })
  return Get-Content -LiteralPath $body -Raw | ConvertFrom-Json -Depth 80 -DateKind String
}
$blockInfo = Read-Probe 'jct-blockscout-info' "https://eth.blockscout.com/api/v2/tokens/$contract"
$ethInfo = Read-Probe 'jct-ethplorer-info' "https://api.ethplorer.io/getTokenInfo/${contract}?apiKey=freekey"
$blockHolders = Read-Probe 'jct-blockscout-holders' "https://eth.blockscout.com/api/v2/tokens/$contract/holders"
$ethHolders = Read-Probe 'jct-ethplorer-holders' "https://api.ethplorer.io/getTopTokenHolders/${contract}?apiKey=freekey&limit=30"
$blockTransfers = Read-Probe 'jct-blockscout-transfers' "https://eth.blockscout.com/api/v2/addresses/0xA68f0255C0EEB35E3CF558d58cfBbfA8811b9B84/token-transfers?type=ERC-20&token=$contract"
$routeTransfers = Read-Probe 'jct-routescan-transfers' "https://api.routescan.io/v2/network/mainnet/evm/1/etherscan/api?module=account&action=tokentx&address=0xA68f0255C0EEB35E3CF558d58cfBbfA8811b9B84&contractaddress=$contract&page=1&offset=10&sort=desc"
if ($blockInfo.address_hash.ToLowerInvariant() -cne $contract -or $ethInfo.address -cne $contract -or
    "$($blockInfo.decimals)" -cne '18' -or "$($ethInfo.decimals)" -cne '18' -or
    $blockInfo.symbol -cne 'JCT' -or $ethInfo.symbol -cne 'JCT') { throw 'JCT identity or decimals mismatch.' }
if ($blockInfo.total_supply -cne $ethInfo.totalSupply) { throw 'Observed deployment supply mismatch.' }
$holderChecks = @($ethHolders.holders | ForEach-Object {
  $holder = $_
  $same = @($blockHolders.items | Where-Object { $_.address.hash.ToLowerInvariant() -ceq $holder.address.ToLowerInvariant() })
  [ordered]@{
    address = $holder.address.ToLowerInvariant(); ethplorerRaw = $holder.rawBalance
    blockscoutRaw = if ($same.Count -eq 1) { $same[0].value } else { $null }
    exact = $same.Count -eq 1 -and $holder.rawBalance -is [string] -and $holder.rawBalance -ceq $same[0].value
  }
})
if (@($holderChecks | Select-Object -First 5 | Where-Object { -not $_.exact }).Count -gt 0) { throw 'Top-five sampled holder balances do not reconcile.' }
if ($routeTransfers.status -ne '1') { throw 'Alternative transfer source did not return a successful result.' }
$eventChecks = @($blockTransfers.items | ForEach-Object {
  $event = $_
  $same = @($routeTransfers.result | Where-Object {
    $_.hash -ceq $event.transaction_hash -and $_.from -ceq $event.from.hash.ToLowerInvariant() -and
    $_.to -ceq $event.to.hash.ToLowerInvariant() -and $_.contractAddress -ceq $contract -and
    $_.value -ceq $event.total.value -and $_.blockHash -ceq $event.block_hash -and
    $_.blockNumber -eq "$($event.block_number)" -and $_.tokenDecimal -eq '18' -and
    [DateTimeOffset]::FromUnixTimeSeconds([long]$_.timeStamp).UtcDateTime -eq ([DateTimeOffset]::Parse($event.timestamp)).UtcDateTime
  })
  [ordered]@{
    hash = $event.transaction_hash; logIndex = $event.log_index; blockNumber = $event.block_number; blockHash = $event.block_hash
    from = $event.from.hash.ToLowerInvariant(); to = $event.to.hash.ToLowerInvariant()
    rawQuantity = $event.total.value; eventTime = $event.timestamp
    exactFieldsMatched = $same.Count -eq 1; independentlyVerifiedLogIndex = $false
  }
})
if ($eventChecks.Count -eq 0 -or @($eventChecks | Where-Object { -not $_.exactFieldsMatched }).Count -gt 0) { throw 'Sampled transfer fields do not reconcile.' }
$report = [ordered]@{
  checkedAt = [DateTime]::UtcNow.ToString('o'); asset = 'Janction'; symbol = 'JCT'; chain = 'ethereum'; contract = $contract
  decision = 'Ethereum identity/18 decimals and selected real holder/transfer fields verified; runtime source permission remains disabled'
  probes = $probes.ToArray()
  metadata = [ordered]@{
    decimals = 18; deploymentRawSupply = $blockInfo.total_supply; supplyExactAcrossTwoIndexers = $true
    sourceAt = $null; holderCounts = @{blockscout = $blockInfo.holders_count; ethplorer = $ethInfo.holdersCount}
    caveat = 'Asynchronous unpinned indexers. Official global genesis allocation is not current single-deployment supply; no global supply, FDV or circulating denominator inferred.'
  }
  holders = [ordered]@{
    blockscoutRows = $blockHolders.items.Count; ethplorerRows = $ethHolders.holders.Count
    exactMatches = @($holderChecks | Where-Object { $_.exact }).Count; checked = $holderChecks; sourceAt = $null
    caveat = 'Sample agreement is not a block-consistent complete Top30, historical coverage, multisig ownership or finality claim. Only raw integer strings compared.'
  }
  transfers = [ordered]@{
    monitoredAddress = $address; checked = $eventChecks; blockscoutRows = $blockTransfers.items.Count
    routescanRows = $routeTransfers.result.Count
    caveat = 'Routescan does not supply log_index in this response. Hash/from/to/value/time/block agreement cannot independently prove log identity, continuous monitoring, finality or historical USD prices.'
  }
  runtimeAcceptance = 'Catalog and missing-source navigation only; no live Java-to-MySQL-to-UI acceptance, JCT market/OI/liquidation acceptance or production source approval'
  rights = 'Development verification only. Ethplorer freekey development limits apply. Blockscout legacy migration and Routescan production storage/display terms remain unresolved. No enabled collection policy created.'
}
$report | ConvertTo-Json -Depth 14 | Set-Content -LiteralPath (Join-Path $projectRoot 'docs/evidence/jct-sample-verification.json') -Encoding utf8
Write-Output "Verified JCT decimals and deployment supply; exact holder balances $($report.holders.exactMatches)/$($holderChecks.Count); transfers $($eventChecks.Count). Source permission and full-flow acceptance remain open."
