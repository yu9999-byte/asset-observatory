param(
 [ValidateSet('core','all')][string]$Scope = 'core',
 [ValidateSet('Direct','System')][string]$Transport = 'Direct'
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$javaHome = Join-Path $projectRoot '.tools/jdk-21.0.12.1+1'
$maven = Join-Path $projectRoot '.tools/apache-maven-3.9.11/bin/mvn.cmd'
if (-not (Test-Path -LiteralPath "$javaHome/bin/java.exe")) { throw 'Install the project JDK first.' }
$proxy = ''
if ($Transport -eq 'System') {
 $settings = Get-ItemProperty 'HKCU:\Software\Microsoft\Windows\CurrentVersion\Internet Settings'
 if ($settings.ProxyEnable -ne 1 -or -not $settings.ProxyServer) { throw 'No enabled existing Windows HTTP proxy. No proxy settings were changed.' }
 $server = [string]$settings.ProxyServer
 if ($server -match '(?:^|;)https?=([^;]+)') { $server = $Matches[1] }
 if ($server -notmatch '^http://') { $server = "http://$server" }
 $uri = [Uri]$server
 if ($uri.Scheme -ne 'http' -or $uri.UserInfo -or $uri.Port -lt 1 -or $uri.AbsolutePath -ne '/') { throw 'Existing proxy must be an uncredentialed HTTP host:port.' }
 $proxy = $uri.GetLeftPart([UriPartial]::Authority)
}
$run = (Get-Date).ToUniversalTime().ToString('yyyyMMddTHHmmssfffZ') + '-' + $Transport.ToLowerInvariant()
$output = Join-Path $projectRoot "docs/evidence/acquisition/$run"
$previousJavaHome = $env:JAVA_HOME
Push-Location $projectRoot
try {
 $env:JAVA_HOME = $javaHome
 $arguments = @('-s','scripts/maven-settings.xml','-Dtest=AcquisitionProbeLiveTest',
  '-Dprobe.live=true',"-Dprobe.scope=$Scope","-Dprobe.output=$output")
 if ($proxy) { $arguments += "-Dprobe.proxy=$proxy" }
 $arguments += @('test','-q')
 & $maven @arguments
 if ($LASTEXITCODE -ne 0) { throw "Probe runner failed. Inspect $output and target/surefire-reports." }
 Write-Output "Evidence report: $output/report.json"
 Write-Output 'This run does not approve or enable any runtime source.'
} finally {
 $env:JAVA_HOME = $previousJavaHome
 Pop-Location
}
