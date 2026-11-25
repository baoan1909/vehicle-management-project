param(
    [ValidateSet('Compile', 'Offline', 'Db', 'Api')][string]$Phase = 'Offline',
    [ValidateSet('all', 'baseline', 'batch-01', 'batch-02', 'batch-03', 'batch-04', 'new')][string]$Dataset = 'all',
    [ValidateRange(0, 2147483647)][int]$Offset = 0,
    [ValidateRange(1, 200)][int]$Limit = 6,
    [string]$OutputDirectory = 'target/qwen-api-acceptance-new-run',
    [ValidateRange(0, 300)][int]$BrowserHoldSeconds = 0
)
$ErrorActionPreference = 'Stop'
$backendRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$outputRoot = [IO.Path]::GetFullPath((Join-Path $backendRoot $OutputDirectory))
$targetRoot = [IO.Path]::GetFullPath((Join-Path $backendRoot 'target'))
if (-not $outputRoot.StartsWith($targetRoot + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) {
    throw 'Runtime output must stay under backend/target.'
}
if ((Test-Path -LiteralPath $outputRoot) -and @(Get-ChildItem -LiteralPath $outputRoot -Force).Count -gt 0) {
    throw 'Output is not empty. Use a new directory to preserve earlier evidence.'
}
New-Item -ItemType Directory -Force -Path $outputRoot | Out-Null
$requiredRam = @{ Compile = 1024; Offline = 512; Db = 1024; Api = 1536 }[$Phase]
$freeRam = [math]::Floor((Get-CimInstance Win32_OperatingSystem).FreePhysicalMemory / 1024)
$preflight = [ordered]@{ phase = $Phase; freeRamMb = $freeRam; minimumRamMb = $requiredRam; started = $false; qualityAccepted = $false }
$preflight | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $outputRoot 'preflight.json') -Encoding utf8
if ($freeRam -lt $requiredRam) { throw "Only $freeRam MB free; need at least $requiredRam MB for $Phase. No workload started." }
$workRoot = Join-Path $targetRoot 'qwen-api-acceptance-work'
New-Item -ItemType Directory -Force -Path $workRoot | Out-Null
$stampFile = Join-Path $workRoot 'compiled-source.json'
$dependencyFile = Join-Path $workRoot 'test-dependencies.txt'

function Get-SourceFingerprint {
    $rows = [Collections.Generic.List[string]]::new()
    $sourceFiles = @((Get-Item -LiteralPath (Join-Path $backendRoot 'pom.xml')))
    $sourceFiles += @(Get-ChildItem -LiteralPath (Join-Path $backendRoot 'src/main'), (Join-Path $backendRoot 'src/test') -Recurse -File)
    foreach ($file in ($sourceFiles | Sort-Object FullName)) {
        $relative = $file.FullName.Substring($backendRoot.Length + 1).Replace('\', '/')
        $rows.Add($relative + ':' + (Get-FileHash -LiteralPath $file.FullName -Algorithm SHA256).Hash.ToLowerInvariant())
    }
    $hash = [Security.Cryptography.SHA256]::Create()
    try { return ([BitConverter]::ToString($hash.ComputeHash([Text.Encoding]::UTF8.GetBytes(($rows -join "`n"))))).Replace('-', '').ToLowerInvariant() }
    finally { $hash.Dispose() }
}

function Write-JavaArguments([string]$File, [string[]]$Arguments) {
    $lines = $Arguments | ForEach-Object { '"' + $_.Replace('\', '\\').Replace('"', '\"') + '"' }
    [IO.File]::WriteAllLines($File, $lines, [Text.UTF8Encoding]::new($false))
}

$environmentNames = @('MAVEN_OPTS', 'AI_QWEN_API_LIVE', 'AI_QWEN_API_DATASET', 'AI_QWEN_API_OFFSET', 'AI_QWEN_API_LIMIT', 'AI_QWEN_API_OUTPUT', 'AI_QWEN_API_BROWSER_HOLD_SECONDS')
$previous = @{}
foreach ($name in $environmentNames) { $previous[$name] = [Environment]::GetEnvironmentVariable($name, 'Process') }
Push-Location -LiteralPath $backendRoot
try {
    $fingerprint = Get-SourceFingerprint
    if ($Phase -eq 'Compile') {
        $env:MAVEN_OPTS = '-Xmx384m -Dfile.encoding=UTF-8'
        & .\mvnw.cmd -B -ntp '-Dmaven.repo.local=.m2-local' '-Dmaven.compiler.useIncrementalCompilation=false' '-DskipTests' test-compile dependency:build-classpath '-DincludeScope=test' "-Dmdep.outputFile=$dependencyFile" *> (Join-Path $outputRoot 'compile.log')
        if ($LASTEXITCODE -ne 0) { throw 'Compilation failed; inspect compile.log.' }
        # Resolve the launcher explicitly for a fresh clone; it need not be a project dependency.
        $compiledDependencies = (Get-Content -LiteralPath $dependencyFile -Raw).Trim()
        $compiledEngine = $compiledDependencies.Split([IO.Path]::PathSeparator) | Where-Object { [IO.Path]::GetFileName($_) -match '^junit-platform-engine-(.+)\.jar$' } | Select-Object -First 1
        if (-not $compiledEngine) { throw 'Compiled dependencies contain no JUnit platform engine.' }
        $compiledVersion = ([regex]::Match([IO.Path]::GetFileName($compiledEngine), '^junit-platform-engine-(.+)\.jar$')).Groups[1].Value
        $compiledLauncher = Join-Path $backendRoot ".m2-local/org/junit/platform/junit-platform-launcher/$compiledVersion/junit-platform-launcher-$compiledVersion.jar"
        if (-not (Test-Path -LiteralPath $compiledLauncher)) {
            & .\mvnw.cmd -B -ntp '-Dmaven.repo.local=.m2-local' dependency:get "-Dartifact=org.junit.platform:junit-platform-launcher:$compiledVersion" *> (Join-Path $outputRoot 'launcher-dependency.log')
            if ($LASTEXITCODE -ne 0 -or -not (Test-Path -LiteralPath $compiledLauncher)) { throw 'Cannot resolve the matching JUnit launcher; inspect launcher-dependency.log.' }
        }
        if ($fingerprint -ne (Get-SourceFingerprint)) { throw 'Source changed during compilation. Do not reuse this artifact.' }
        @{ sourceSha256 = $fingerprint; kind = 'MAVEN_TEST_COMPILE_NO_LIVE'; compiledAt = [DateTimeOffset]::UtcNow.ToString('o') } | ConvertTo-Json | Set-Content -LiteralPath $stampFile -Encoding utf8
        Write-Output 'COMPILE_PASS. Compiler exited; runtime phases use a separate Java VM.'
        return
    }
    if (-not (Test-Path -LiteralPath $stampFile) -or -not (Test-Path -LiteralPath $dependencyFile)) { throw 'Run Compile first on the final source.' }
    if ((Get-Content -LiteralPath $stampFile -Raw | ConvertFrom-Json).sourceSha256 -ne $fingerprint) { throw 'Build is stale. Run Compile again before evaluation.' }
    if ($Phase -in @('Db', 'Api')) {
        & docker info --format '{{.ServerVersion}}' *> $null
        if ($LASTEXITCODE -ne 0) { throw 'Docker unavailable. Isolated DB test not started.' }
    }
    if ($Phase -eq 'Api') {
        $modelsJson = & (Join-Path $env:USERPROFILE '.lmstudio/bin/lms.exe') ps --json
        if ($LASTEXITCODE -ne 0) { throw 'Cannot read loaded LM Studio models.' }
        $models = $modelsJson | ConvertFrom-Json
        $models | Select-Object identifier, contextLength, parallel, status, queued | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $outputRoot 'loaded-models.json') -Encoding utf8
        $chat = $models | Where-Object identifier -eq 'qwen3.5-4b' | Select-Object -First 1
        if (-not $chat -or $chat.contextLength -ne 2048 -or $chat.parallel -ne 1 -or $chat.status -ne 'idle' -or $chat.queued -gt 0) {
            throw 'The full HTTP harness currently requires loaded Qwen chat context2048/parallel1 and no pending requests. The earlier512 component run does not satisfy this gate.'
        }
        if (-not ($models | Where-Object identifier -eq 'text-embedding-qwen3-embedding-0.6b')) { throw 'Configured Qwen embedding model is not loaded.' }
        $env:AI_QWEN_API_LIVE = 'true'; $env:AI_QWEN_API_DATASET = $Dataset
        $env:AI_QWEN_API_OFFSET = [string]$Offset; $env:AI_QWEN_API_LIMIT = [string]$Limit
        $env:AI_QWEN_API_OUTPUT = $outputRoot; $env:AI_QWEN_API_BROWSER_HOLD_SECONDS = [string]$BrowserHoldSeconds
    }
    $java = (Get-Command java -ErrorAction Stop).Source
    $javac = (Get-Command javac -ErrorAction Stop).Source
    $dependencies = (Get-Content -LiteralPath $dependencyFile -Raw).Trim()
    $engine = $dependencies.Split([IO.Path]::PathSeparator) | Where-Object { [IO.Path]::GetFileName($_) -match '^junit-platform-engine-(.+)\.jar$' } | Select-Object -First 1
    if (-not $engine) { throw 'Cannot determine the actual JUnit platform version from compiled dependencies.' }
    $version = ([regex]::Match([IO.Path]::GetFileName($engine), '^junit-platform-engine-(.+)\.jar$')).Groups[1].Value
    $launcherJar = Join-Path $backendRoot ".m2-local/org/junit/platform/junit-platform-launcher/$version/junit-platform-launcher-$version.jar"
    if (-not (Test-Path -LiteralPath $launcherJar)) { throw "JUnit launcher $version is missing from the local Maven repository. Resolve matching test dependencies first." }
    $runtime = Join-Path $workRoot 'launcher-classes'
    New-Item -ItemType Directory -Force -Path $runtime | Out-Null
    $classpath = @($runtime, (Join-Path $targetRoot 'test-classes'), (Join-Path $targetRoot 'classes'), $dependencies, $launcherJar) -join [IO.Path]::PathSeparator
    $launcher = Join-Path $workRoot 'QwenApiJUnitLauncher.java'
    $source = @'
import java.io.*;
import org.junit.platform.launcher.core.*;
import org.junit.platform.launcher.listeners.*;
import static org.junit.platform.engine.discovery.DiscoverySelectors.*;
public class QwenApiJUnitLauncher {
 public static void main(String[] args) {
  var request=LauncherDiscoveryRequestBuilder.request();
  for(String name:args) request.selectors(selectClass(name));
  var listener=new SummaryGeneratingListener();
  LauncherFactory.create().execute(request.build(),listener);
  var summary=listener.getSummary();
  summary.printTo(new PrintWriter(System.out,true));
  summary.printFailuresTo(new PrintWriter(System.out,true));
  if(summary.getTestsFoundCount()==0||summary.getTestsSkippedCount()>0||summary.getTestsFailedCount()>0||summary.getContainersFailedCount()>0) System.exit(1);
 }
}
'@
    [IO.File]::WriteAllText($launcher, $source, [Text.UTF8Encoding]::new($false))
    $compileArgs = Join-Path $workRoot 'compile-launcher.args'
    Write-JavaArguments $compileArgs @('-proc:none', '-encoding', 'UTF-8', '-cp', $classpath, '-d', $runtime, $launcher)
    & $javac '-J-Xmx128m' "@$compileArgs" *> (Join-Path $outputRoot 'launcher-compile.log')
    if ($LASTEXITCODE -ne 0) { throw 'JUnit launcher compilation failed; inspect launcher-compile.log.' }
    $freeRam = [math]::Floor((Get-CimInstance Win32_OperatingSystem).FreePhysicalMemory / 1024)
    if ($freeRam -lt $requiredRam) { throw 'RAM dropped below the phase guard after launcher compilation. Runtime not started.' }
    $tests = switch ($Phase) {
        'Offline' { @('com.ban.vehicle_management.ai.golden.ReviewedFaqRegressionTest', 'com.ban.vehicle_management.ai.golden.QwenApiCaseSelectionTest', 'com.ban.vehicle_management.ai.golden.CommonKnowledgeCorpusTest') }
        'Db' { @('com.ban.vehicle_management.infrastructure.persistence.CommonKnowledgePersistenceTest') }
        'Api' { @('com.ban.vehicle_management.ai.golden.QwenChatApiAcceptanceIT') }
    }
    $heap = @{ Offline = 128; Db = 384; Api = 512 }[$Phase]
    $runArgs = Join-Path $workRoot ('run-' + $Phase.ToLowerInvariant() + '.args')
    Write-JavaArguments $runArgs (@('-Xms16m', "-Xmx${heap}m", '-Dfile.encoding=UTF-8', '-cp', $classpath, 'QwenApiJUnitLauncher') + $tests)
    $preflight.started = $true
    $preflight | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $outputRoot 'preflight.json') -Encoding utf8
    & $java "@$runArgs" *> (Join-Path $outputRoot 'run.log')
    if ($LASTEXITCODE -ne 0) { throw "Phase $Phase failed. Preserve this cohort and inspect run.log; quality is not accepted." }
    Write-Output "PHASE_PASS=$Phase (semantic grading remains separate)."
} finally {
    foreach ($name in $environmentNames) { [Environment]::SetEnvironmentVariable($name, $previous[$name], 'Process') }
    Pop-Location
}
