# JCT holder source recheck — 2026-10-22

This is a bounded, read-only transport check for the verified Ethereum JCT
contract `0xc477b6dfd26ec2460b3b92de18837fd476ea7549` (18 decimals). It does not
enable a runtime source or establish external use rights.

## Result

The repository's opt-in Java probe ran on JDK 21.0.12.1 with Maven 3.9.11.
Surefire reports 1 test run, 0 failures, 0 errors, and 0 skipped. The probe
selected only three previously documented GET endpoints and captured its report
at `evidence/acquisition/20261022T022203Z-holder-recheck/report.json`.

| UTC observation | Endpoint | Result |
| --- | --- | --- |
| 02:22:03–02:22:08 | Blockscout `/api/v2/tokens/{JCT}/holders` | `ConnectException`, no HTTP status or response body |
| 02:22:09–02:22:14 | Ethplorer `/getTokenInfo/{JCT}` | `ConnectException`, no HTTP status or response body |
| 02:22:15–02:22:20 | Ethplorer `/getTopTokenHolders/{JCT}?limit=30` | `ConnectException`, no HTTP status or response body |

These are host transport failures, not empty results and not evidence that the
providers lack JCT data. No holder balance, count, snapshot block, or page was
received, so this run cannot support a Top30 claim. Provider-side availability,
stable pagination, external display/storage rights and independent quantity
reconciliation remain unverified. The earlier samples are not refreshed by this
failed run, and no provider adapter was enabled.

## Reproduction

From the project root in PowerShell:

```powershell
$env:JAVA_HOME = (Resolve-Path '.tools/jdk-21.0.12.1+1').Path
& '.tools/apache-maven-3.9.11/bin/mvn.cmd' -s scripts/maven-settings.xml -q `
  '-Dtest=AcquisitionProbeLiveTest' '-Dprobe.live=true' `
  '-Dprobe.targets=blockscout-holders,ethplorer-token,ethplorer-holders' `
  '-Dprobe.output=target/acquisition-<new-unique-run-id>' test
```

Use a new output directory for every run. The raw report is local feasibility
evidence only; a successful HTTP response still would not grant permission or
prove completeness.
