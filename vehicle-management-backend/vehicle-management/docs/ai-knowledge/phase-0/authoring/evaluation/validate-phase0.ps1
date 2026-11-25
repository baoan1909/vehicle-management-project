param(
    [string]$PhaseRoot = (Split-Path $PSScriptRoot -Parent),
    [string]$NodeExecutable = "node"
)

$ErrorActionPreference = 'Stop'
$errors = [Collections.Generic.List[string]]::new()
$warnings = [Collections.Generic.List[string]]::new()
$allowedModes = @('DIRECT','RAG','BUSINESS_TOOL','ACTION_CARD','HANDOFF','REFUSE')
$requiredFields = @(
    'caseId','domain','question','normalizedQuestion','variantType','expectedAnswerMode','expectedIntent',
    'expectedDocumentSlugs','expectedTool','confirmationRequired','expectedAccessScope','requiredPermission',
    'ownershipRequired','citationsRequired','handoffExpected','piiRisk','expectedBehavior','forbiddenBehavior','status'
)
$requiredKnowledgeHeadings = @(
    'Mục đích','Đối tượng','Phạm vi','Thuật ngữ','Điều kiện trước','Luồng chính','Luồng thay thế','Luồng lỗi',
    'Trạng thái và chuyển trạng thái','Câu hỏi khách hàng thường gặp','Điều chatbot được phép trả lời',
    'Điều chatbot phải dùng business tool','Điều chatbot phải từ chối','Khi nào chuyển nhân viên',
    'Nguồn đối chiếu','Nội dung TBD','Lịch sử phiên bản'
)
$requiredMetadata = @('title','slug','documentVersion','status','language','accessScope','ownerPermission','sourceOfTruth','effectiveFrom','reviewAt','lastVerifiedAt')

$knowledgePath = Join-Path $PhaseRoot 'knowledge'
$datasetPath = Join-Path $PSScriptRoot 'golden-questions.jsonl'
$intentPath = Join-Path $PhaseRoot 'intent-routing-matrix.md'
$knowledgeFiles = @(Get-ChildItem -LiteralPath $knowledgePath -Filter '*.md' -File)
$manifest = Get-Content -Raw -Encoding UTF8 -LiteralPath (Join-Path $PhaseRoot 'knowledge-manifest.json') | ConvertFrom-Json
if ($knowledgeFiles.Count -ne @($manifest.documents).Count) { $errors.Add("Knowledge count differs from manifest") }

$slugs = @{}
foreach ($file in $knowledgeFiles) {
    $content = Get-Content -Raw -Encoding UTF8 -LiteralPath $file.FullName
    foreach ($field in $requiredMetadata) {
        if ($content -notmatch "(?m)^$([regex]::Escape($field)):\s*") { $errors.Add("Missing metadata $field in $($file.Name)") }
    }
    foreach ($heading in $requiredKnowledgeHeadings) {
        if ($content -notmatch "(?m)^## $([regex]::Escape($heading))\s*$") { $errors.Add("Missing heading '$heading' in $($file.Name)") }
    }
    $slugMatch = [regex]::Match($content, '(?m)^slug:\s*(\S+)\s*$')
    if ($slugMatch.Success) { $slugs[$slugMatch.Groups[1].Value] = $file.Name }
}

$intentContent = Get-Content -Raw -Encoding UTF8 -LiteralPath $intentPath
$intentSet = [Collections.Generic.HashSet[string]]::new()
foreach ($match in [regex]::Matches($intentContent, '(?m)^\|\s*([A-Z][A-Z0-9_]+)\s*\|')) {
    [void]$intentSet.Add($match.Groups[1].Value)
}

$lines = @(Get-Content -Encoding UTF8 -LiteralPath $datasetPath | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
$rows = [Collections.Generic.List[object]]::new()
foreach ($line in $lines) {
    try { $rows.Add(($line | ConvertFrom-Json)) }
    catch { $errors.Add("Invalid JSONL: $($_.Exception.Message)") }
}
$baseline = @($manifest.datasets | Where-Object { $_.kind -eq 'baseline' })[0]
if ($rows.Count -ne $baseline.expectedCount) { $errors.Add("Baseline dataset count differs from manifest") }

$duplicateIds = @($rows | Group-Object caseId | Where-Object Count -gt 1)
if ($duplicateIds.Count -gt 0) { $errors.Add('Duplicate caseId: ' + (($duplicateIds.Name) -join ', ')) }

foreach ($row in $rows) {
    foreach ($field in $requiredFields) {
        if ($row.PSObject.Properties.Name -notcontains $field) { $errors.Add("$($row.caseId): missing field $field") }
    }
    if ($allowedModes -notcontains $row.expectedAnswerMode) { $errors.Add("$($row.caseId): invalid answer mode $($row.expectedAnswerMode)") }
    if (-not $intentSet.Contains([string]$row.expectedIntent)) { $errors.Add("$($row.caseId): intent not in matrix: $($row.expectedIntent)") }
    foreach ($slug in @($row.expectedDocumentSlugs)) {
        if (-not $slugs.ContainsKey([string]$slug)) { $errors.Add("$($row.caseId): missing document slug $slug") }
    }
    if ($row.expectedAnswerMode -eq 'ACTION_CARD' -and -not $row.confirmationRequired) {
        $errors.Add("$($row.caseId): ACTION_CARD must require confirmation")
    }
    if ($row.expectedAnswerMode -eq 'RAG' -and -not $row.citationsRequired) {
        $errors.Add("$($row.caseId): RAG must require citations")
    }
}

foreach ($group in @($rows | Group-Object domain)) {
    $variants = @($group.Group.variantType)
    if ($variants -notcontains 'ACCENTED') { $errors.Add("Domain $($group.Name) lacks ACCENTED case") }
    if ($variants -notcontains 'UNACCENTED') { $errors.Add("Domain $($group.Name) lacks UNACCENTED case") }
}

$artifactFiles = @(Get-ChildItem -LiteralPath $PhaseRoot -Recurse -File | Where-Object { $_.Extension -in @('.md','.jsonl','.ps1') })
foreach ($file in $artifactFiles) {
    $content = Get-Content -Raw -Encoding UTF8 -LiteralPath $file.FullName
    if ($content -cmatch '\u00C3|\u00C2|\u00C4|\u00E2\u20AC') { $errors.Add("Mojibake marker in $($file.FullName)") }
    if ($content -match 'AIza[0-9A-Za-z_-]{20,}|eyJ[A-Za-z0-9_-]{20,}\.[A-Za-z0-9_-]{10,}\.|-----BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY-----') {
        $errors.Add("Possible secret in $($file.FullName)")
    }
    if ($file.Extension -eq '.jsonl' -and $content -match '(?<![0-9-])\d{13,19}(?![0-9-])') {
        $warnings.Add("Long numeric sequence requires review in $($file.FullName)")
    }
    if ($file.Extension -eq '.jsonl' -and $content -match '(?i)\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}\b') {
        $errors.Add("Email-like personal data in $($file.FullName)")
    }
    if ($file.Extension -eq '.jsonl' -and $content -match '(?<!\d)(?:\+84|0)\d{9,10}(?!\d)') {
        $errors.Add("Phone-like personal data in $($file.FullName)")
    }
}

$runtimeArtifacts = @(Get-ChildItem -LiteralPath $PhaseRoot -Recurse -File | Where-Object { $_.Extension -in @('.java','.kt','.ts','.tsx','.sql') })
if ($runtimeArtifacts.Count -gt 0) { $errors.Add('Phase 0 unexpectedly contains runtime source files') }

$syncScript = Join-Path $PhaseRoot '../../../../scripts/sync-ai-knowledge.mjs'
& $NodeExecutable $syncScript
if ($LASTEXITCODE -ne 0) { $errors.Add('Canonical knowledge/import copies, batch dataset or manifest checks failed') }
$summary = [ordered]@{
    knowledgeDocuments = $knowledgeFiles.Count
    goldenQuestions = $rows.Count
    uniqueCaseIds = @($rows.caseId | Sort-Object -Unique).Count
    intentsInMatrix = $intentSet.Count
    domains = @($rows.domain | Sort-Object -Unique).Count
    errors = $errors.Count
    warnings = $warnings.Count
}
$summary | ConvertTo-Json
if ($warnings.Count -gt 0) { $warnings | ForEach-Object { Write-Warning $_ } }
if ($errors.Count -gt 0) {
    $errors | ForEach-Object { Write-Output "ERROR: $_" }
    exit 1
}
Write-Output 'PHASE0_ARTIFACT_VALIDATION=PASS'
