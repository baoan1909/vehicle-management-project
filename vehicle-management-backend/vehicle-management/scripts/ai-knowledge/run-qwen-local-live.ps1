param(
    [string]$OutputDirectory = 'target/qwen-live-new-run',
    [ValidateRange(1536, 2147483647)][int]$MinimumFreeRamMb = 1536,
    [ValidateRange(1, 50)][int]$CaseLimit = 6,
    [ValidateRange(0, 2147483647)][int]$CaseOffset = 0,
    [ValidateRange(0, 4)][int]$Batch = 0
)

$ErrorActionPreference = 'Stop'
$backendRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$os = Get-CimInstance Win32_OperatingSystem
$availableMb = [math]::Round($os.FreePhysicalMemory / 1024)
if ($availableMb -lt $MinimumFreeRamMb) {
    throw "Only $availableMb MB of physical RAM is free; live test not started. Need at least $MinimumFreeRamMb MB before launching the test. This check is a precaution, not a guarantee of runtime capacity."
}
& docker info --format '{{.ServerVersion}}' *> $null
if ($LASTEXITCODE -ne 0) { throw 'Docker is unavailable; isolated live test not started.' }

$previousLive = $env:AI_QWEN_LIVE
$previousOutput = $env:AI_QWEN_LIVE_OUTPUT
$previousMaven = $env:MAVEN_OPTS
$previousLimit = $env:AI_QWEN_LIVE_CASE_LIMIT
$previousOffset = $env:AI_QWEN_LIVE_CASE_OFFSET
$previousBatch = $env:AI_QWEN_LIVE_BATCH
Push-Location -LiteralPath $backendRoot
try {
    $env:AI_QWEN_LIVE = 'true'
    $env:AI_QWEN_LIVE_OUTPUT = $OutputDirectory
    $env:AI_QWEN_LIVE_CASE_LIMIT = [string]$CaseLimit
    $env:AI_QWEN_LIVE_CASE_OFFSET = [string]$CaseOffset
    $env:AI_QWEN_LIVE_BATCH = [string]$Batch
    # Run in the Maven JVM to avoid keeping a second JVM beside the local models.
    $env:MAVEN_OPTS = '-Xmx768m -Dfile.encoding=UTF-8'
    & .\mvnw.cmd -B -ntp '-Dmaven.repo.local=.m2-local' '-DforkCount=0' '-Dspring.jpa.show-sql=false' '-Dtest=QwenLocalAcceptanceIT' test
    if ($LASTEXITCODE -ne 0) { throw 'Live test did not pass. Preserve output and inspect the recorded failures.' }
} finally {
    $env:AI_QWEN_LIVE = $previousLive
    $env:AI_QWEN_LIVE_OUTPUT = $previousOutput
    $env:MAVEN_OPTS = $previousMaven
    $env:AI_QWEN_LIVE_CASE_LIMIT = $previousLimit
    $env:AI_QWEN_LIVE_CASE_OFFSET = $previousOffset
    $env:AI_QWEN_LIVE_BATCH = $previousBatch
    Pop-Location
}
