param([string]$AndroidSdk = $env:ANDROID_HOME)
$ErrorActionPreference = 'Stop'
$auditRepoRoot = (Resolve-Path (Join-Path $PSScriptRoot '../../..')).Path
$auditSource = Join-Path $PSScriptRoot 'AuditV4IndependentTest.kt'
$auditTarget = Join-Path $auditRepoRoot 'app/src/test/java/com/scribe/caligrafia/audit/AuditV4IndependentTest.kt'
if (Test-Path -LiteralPath $auditTarget) { throw 'Temporary test path already exists; preserve it before running.' }
if ($AndroidSdk) { $env:ANDROID_HOME = $AndroidSdk }
$auditCode = 1
Push-Location $auditRepoRoot
try {
    Copy-Item -LiteralPath $auditSource -Destination $auditTarget
    & .\gradlew.bat testDebugUnitTest --tests com.scribe.caligrafia.audit.AuditV4IndependentTest --console=plain
    $auditCode = $LASTEXITCODE
} finally {
    if (Test-Path -LiteralPath $auditTarget) {
        if ((Get-FileHash -LiteralPath $auditTarget).Hash -eq (Get-FileHash -LiteralPath $auditSource).Hash) {
            Remove-Item -LiteralPath $auditTarget
        } else {
            Write-Warning 'Temporary test changed during execution; retained for review.'
        }
    }
    Pop-Location
}
exit $auditCode
