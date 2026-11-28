param([switch]$Smoke, [switch]$FullRegression, [int]$CaseLimit = 0, [string]$ResumeFrom = '', [string]$CaseIdsFile = '')
$ErrorActionPreference = 'Stop'
$backendPath = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..\..')).Path
Set-Location -LiteralPath $backendPath
# Only configured Google credentials/project metadata enter the child process.
# Never print values or source arbitrary commands from .env.
$environmentFile = Join-Path $backendPath '.env'
if (Test-Path -LiteralPath $environmentFile) {
    foreach ($line in Get-Content -LiteralPath $environmentFile -Encoding UTF8) {
        if ($line -match '^\s*(GEMINI_(?:API_KEY|PROJECT_ID)(?:_\d+)?|GEMINI_BASE_URL)\s*=\s*(.*)$') {
            $variableName = $Matches[1]
            $variableValue = $Matches[2].Trim().Trim('"').Trim("'")
            [Environment]::SetEnvironmentVariable($variableName, $variableValue, 'Process')
        }
    }
}
$cohortName = 'cohort-' + [DateTime]::UtcNow.ToString('yyyyMMddTHHmmssZ')
$outputPath = Join-Path $backendPath "target\batch05-google-live\$cohortName"
New-Item -ItemType Directory -Path $outputPath -Force | Out-Null
$env:BATCH5_GOOGLE_LIVE = 'true'
$env:BATCH5_OUTPUT = $outputPath
$env:BATCH5_RESUME_FROM = $ResumeFrom
$env:BATCH5_CASE_IDS_FILE = $CaseIdsFile
if ($CaseLimit -le 0) { $CaseLimit = if ($FullRegression) { 1408 } else { 280 } }
$env:BATCH5_CASE_LIMIT = [string]$CaseLimit
$env:BATCH5_SMOKE = [string]$Smoke.IsPresent.ToString().ToLowerInvariant()
$env:BATCH5_FULL_REGRESSION = [string]$FullRegression.IsPresent.ToString().ToLowerInvariant()
Write-Output "Cohort: $outputPath"
& .\mvnw.cmd -q '-Dtest=Batch5ChatboxGoogleLiveIT' test *> (Join-Path $outputPath 'run.log')
$resultCode = $LASTEXITCODE
Write-Output "Maven exit: $resultCode; checkpoint and private logs: $outputPath"
exit $resultCode
