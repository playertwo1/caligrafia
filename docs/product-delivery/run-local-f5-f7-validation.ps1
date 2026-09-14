param(
    [switch]$SkipAudit,
    [switch]$SkipLint
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

function Invoke-Step {
    param(
        [string]$Name,
        [scriptblock]$Command,
        [string]$LogPath
    )

    Write-Host "`n=== $Name ===" -ForegroundColor Cyan
    try {
        & $Command *>&1 | Tee-Object -FilePath $LogPath
        $exitCodeVariable = Get-Variable LASTEXITCODE -ErrorAction SilentlyContinue
        $exitCode = if ($null -ne $exitCodeVariable) { [int]$exitCodeVariable.Value } else { 0 }
    } catch {
        $_ | Out-String | Tee-Object -FilePath $LogPath -Append | Write-Host
        $exitCode = 1
    }

    if ($exitCode -ne 0) {
        Write-Host "FALHOU: $Name (exit $exitCode)" -ForegroundColor Red
        return $false
    }

    Write-Host "OK: $Name" -ForegroundColor Green
    return $true
}

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "../..")).Path
Set-Location $repoRoot

if (-not (Test-Path ".git")) {
    throw "Execute este script dentro de um checkout Git do repositorio."
}

$branch = (git branch --show-current).Trim()
$sha = (git rev-parse HEAD).Trim()
$shortSha = $sha.Substring(0, [Math]::Min(12, $sha.Length))
$statusBefore = @(git status --porcelain)

if ($branch -ne "phase-f6-f7-finalization") {
    Write-Warning "Branch atual: $branch. A branch esperada para esta validacao e phase-f6-f7-finalization."
}

if ($statusBefore.Count -gt 0) {
    Write-Warning "Working tree nao esta limpo ANTES dos testes. Nao use este run como evidencia final ate entender as alteracoes locais."
    $statusBefore | ForEach-Object { Write-Warning $_ }
}

$artifactRoot = Join-Path $repoRoot "validation-artifacts/$shortSha"
New-Item -ItemType Directory -Force -Path $artifactRoot | Out-Null

$javaSummary = try { (& java -version 2>&1 | Select-Object -First 1) } catch { "java indisponivel" }
$wrapperSummary = if (Test-Path "./gradlew.bat") { "gradlew.bat" } elseif (Test-Path "./gradlew") { "./gradlew" } else { "missing" }

@(
    "timestamp=$(Get-Date -Format o)",
    "branch=$branch",
    "sha=$sha",
    "java=$javaSummary",
    "gradle_wrapper=$wrapperSummary"
) | Set-Content -Encoding UTF8 (Join-Path $artifactRoot "run-metadata.txt")

$gradle = if (Test-Path "./gradlew.bat") { ".\gradlew.bat" } elseif (Test-Path "./gradlew") { "./gradlew" } else { throw "Gradle wrapper nao encontrado." }

$results = [ordered]@{}
$results["testDebugUnitTest"] = Invoke-Step -Name "Unit tests" -Command { & $gradle testDebugUnitTest --no-daemon --stacktrace } -LogPath (Join-Path $artifactRoot "testDebugUnitTest.log")
$results["assembleDebug"] = Invoke-Step -Name "assembleDebug" -Command { & $gradle assembleDebug --no-daemon --stacktrace } -LogPath (Join-Path $artifactRoot "assembleDebug.log")

if (-not $SkipLint) {
    $results["lint"] = Invoke-Step -Name "lint" -Command { & $gradle lint --no-daemon --stacktrace } -LogPath (Join-Path $artifactRoot "lint.log")
} else {
    $results["lint"] = $null
}

$auditScript = Join-Path $repoRoot "docs/audit-v5/run-all-audits.ps1"
if (-not $SkipAudit -and (Test-Path $auditScript)) {
    $results["audit"] = Invoke-Step -Name "audit runner" -Command { & $auditScript } -LogPath (Join-Path $artifactRoot "audit.log")
} elseif ($SkipAudit) {
    $results["audit"] = $null
} else {
    Write-Warning "Runner de auditoria nao encontrado em $auditScript"
    $results["audit"] = $false
}

$testResultsDir = Join-Path $repoRoot "app/build/test-results/testDebugUnitTest"
if (Test-Path $testResultsDir) {
    Copy-Item -Recurse -Force $testResultsDir (Join-Path $artifactRoot "test-results")
}

$reportsDir = Join-Path $repoRoot "app/build/reports"
if (Test-Path $reportsDir) {
    $lintResults = Get-ChildItem -Path $reportsDir -Recurse -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -match '^lint-results.*\.(html|xml|txt)$' }
    foreach ($item in $lintResults) {
        Copy-Item -Force $item.FullName $artifactRoot
    }
}

$debugApk = Join-Path $repoRoot "app/build/outputs/apk/debug/app-debug.apk"
if (Test-Path $debugApk) {
    $hash = Get-FileHash -Algorithm SHA256 $debugApk
    @(
        "DEBUG APK - NAO E RELEASE CERTIFICADA",
        "path=$($hash.Path)",
        "sha256=$($hash.Hash)"
    ) | Set-Content -Encoding UTF8 (Join-Path $artifactRoot "debug-apk-sha256.txt")
}

@(git status --porcelain) | Set-Content -Encoding UTF8 (Join-Path $artifactRoot "git-status-after.txt")

$summary = @("branch=$branch", "sha=$sha")
foreach ($key in $results.Keys) {
    $value = $results[$key]
    $text = if ($null -eq $value) { "SKIPPED" } elseif ($value) { "PASS" } else { "FAIL" }
    $summary += "$key=$text"
}
$summary | Set-Content -Encoding UTF8 (Join-Path $artifactRoot "summary.txt")

Write-Host "`n=== RESUMO ===" -ForegroundColor Cyan
$summary | ForEach-Object { Write-Host $_ }
Write-Host "Artefatos: $artifactRoot"

$failed = @($results.Values | Where-Object { $_ -eq $false })
if ($failed.Count -gt 0) {
Write-Host "Validacao local encontrou falhas. NAO feche F5.G/F6.G/F7.G." -ForegroundColor Red
    exit 1
}

Write-Host "Validacao automatizada local passou. Isso ainda NAO substitui S25 Ultra/Watch, E2E, acessibilidade, upgrade ou release assinada." -ForegroundColor Green
exit 0
