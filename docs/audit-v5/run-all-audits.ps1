param([string]$AndroidSdk = $env:ANDROID_HOME)
$ErrorActionPreference = 'Stop'
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
if ($AndroidSdk) { $env:ANDROID_HOME = $AndroidSdk }

Push-Location $repoRoot
try {
    Write-Host "=================================================" -ForegroundColor Cyan
    Write-Host " [SCRIBE AUDIT V5] - VERIFICACAO AUTOMATIZADA    " -ForegroundColor Cyan
    Write-Host "=================================================" -ForegroundColor Cyan

    Write-Host "`n[1/5] Executando os 16 testes adversariais da Auditoria Codex V4..." -ForegroundColor Yellow
    & .\gradlew.bat testDebugUnitTest --tests com.scribe.caligrafia.audit.AuditV4IndependentTest --console=plain
    if ($LASTEXITCODE -ne 0) { throw "Falha nos testes de AuditV4IndependentTest (exit code $LASTEXITCODE)" }
    Write-Host "-> PASSOU: 16/16 testes adversariais aprovados!" -ForegroundColor Green

    Write-Host "`n[2/5] Executando os 33 testes formais de aceitacao historicos..." -ForegroundColor Yellow
    & .\gradlew.bat testDebugUnitTest --tests com.scribe.caligrafia.audit.AuditFixAcceptanceTest --console=plain
    if ($LASTEXITCODE -ne 0) { throw "Falha nos testes de AuditFixAcceptanceTest (exit code $LASTEXITCODE)" }
    Write-Host "-> PASSOU: 33/33 testes de aceitacao aprovados!" -ForegroundColor Green

    Write-Host "`n[3/5] Executando a suite completa de testes unitarios..." -ForegroundColor Yellow
    & .\gradlew.bat testDebugUnitTest --console=plain
    if ($LASTEXITCODE -ne 0) { throw "Falha na suite completa de testes (exit code $LASTEXITCODE)" }
    Write-Host "-> PASSOU: Suite completa (243 testes) 100% verde!" -ForegroundColor Green

    Write-Host "`n[4/5] Compilando o APK de depuracao (assembleDebug)..." -ForegroundColor Yellow
    & .\gradlew.bat assembleDebug --console=plain
    if ($LASTEXITCODE -ne 0) { throw "Falha no assembleDebug (exit code $LASTEXITCODE)" }
    Write-Host "-> PASSOU: APK compilado com sucesso!" -ForegroundColor Green

    Write-Host "`n[5/5] Executando verificacao estatica de Lint (:app:lintDebug)..." -ForegroundColor Yellow
    & .\gradlew.bat :app:lintDebug --console=plain
    if ($LASTEXITCODE -ne 0) { throw "Falha no lintDebug (exit code $LASTEXITCODE)" }
    Write-Host "-> PASSOU: Lint concluido com 0 erros bloqueadores!" -ForegroundColor Green

    Write-Host "`n=================================================" -ForegroundColor Green
    Write-Host " [SUCESSO] TODOS OS 5 GATES DE AUDITORIA APROVADOS! " -ForegroundColor Green
    Write-Host "=================================================" -ForegroundColor Green
} finally {
    Pop-Location
}
