# scripts/watchdog.ps1 — Auditoria Automatizada do Scribe
param(
    [switch]$SkipBuild = $false
)

$ErrorActionPreference = "Stop"
Write-Host "==================================================" -ForegroundColor Cyan
Write-Host "           SCRIBE WATCHDOG & AUDITOR              " -ForegroundColor Cyan
Write-Host "==================================================" -ForegroundColor Cyan

$RepoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $RepoRoot

$Failed = $false

# 1. Verificação estática de regras (AGENTS.md / WATCHDOG.md)
Write-Host "`n[1/4] Verificando regras estáticas de arquitetura..." -ForegroundColor Yellow

# Verificar se há uso de WebView no código Kotlin
$WebViewMatches = Get-ChildItem -Path "$RepoRoot\app\src\main" -Filter "*.kt" -Recurse | Select-String "android.webkit.WebView"
if ($WebViewMatches) {
    Write-Host " [FALHA] Proibido o uso de WebView:" -ForegroundColor Red
    $WebViewMatches | ForEach-Object { Write-Host "   -> $($_.Path):$($_.LineNumber)" -ForegroundColor Red }
    $Failed = $true
} else {
    Write-Host " [OK] Nenhuma WebView detectada." -ForegroundColor Green
}

# Verificar se há dependências proibidas no build.gradle.kts (Cloud, Firebase, etc.)
$ForbiddenDeps = @("com.google.firebase", "aws-sdk", "com.azure")
foreach ($dep in $ForbiddenDeps) {
    $DepMatch = Get-ChildItem -Path "$RepoRoot" -Filter "*build.gradle.kts" -Recurse | Select-String $dep
    if ($DepMatch) {
        Write-Host " [FALHA] Dependência de Cloud/Backend não autorizada encontrada ($dep):" -ForegroundColor Red
        $DepMatch | ForEach-Object { Write-Host "   -> $($_.Path):$($_.LineNumber)" -ForegroundColor Red }
        $Failed = $true
    }
}
if (-not $Failed) {
    Write-Host " [OK] Nenhuma dependência de nuvem/backend não autorizada detectada." -ForegroundColor Green
}

# 2. Execução dos testes unitários
Write-Host "`n[2/4] Executando testes unitários automatizados..." -ForegroundColor Yellow
try {
    if (Test-Path "$RepoRoot\app\build\test-results") {
        Remove-Item -Recurse -Force "$RepoRoot\app\build\test-results" -ErrorAction SilentlyContinue
    }
    & ".\gradlew.bat" testDebugUnitTest --quiet
    if ($LASTEXITCODE -ne 0) {
        Write-Host " [FALHA] Testes unitários falharam (exit code $LASTEXITCODE)." -ForegroundColor Red
        $Failed = $true
    } else {
        Write-Host " [OK] Todos os testes unitários passaram com sucesso." -ForegroundColor Green
    }
} catch {
    Write-Host " [FALHA] Erro ao executar testes: $_" -ForegroundColor Red
    $Failed = $true
}

# 3. Compilação do APK de Debug
if (-not $SkipBuild) {
    Write-Host "`n[3/4] Compilando APK de Depuração..." -ForegroundColor Yellow
    try {
        & ".\gradlew.bat" assembleDebug --quiet
        if ($LASTEXITCODE -ne 0) {
            Write-Host " [FALHA] Compilação do APK falhou (exit code $LASTEXITCODE)." -ForegroundColor Red
            $Failed = $true
        } else {
            Write-Host " [OK] APK compilado com sucesso." -ForegroundColor Green
        }
    } catch {
        Write-Host " [FALHA] Erro ao compilar APK: $_" -ForegroundColor Red
        $Failed = $true
    }
} else {
    Write-Host "`n[3/4] Compilação ignorada (--SkipBuild ativo)." -ForegroundColor DarkGray
}

# 4. Verificação de integridade dos artefatos
Write-Host "`n[4/4] Verificando integridade dos artefatos..." -ForegroundColor Yellow
$ApkPath = "$RepoRoot\app\build\outputs\apk\debug\app-debug.apk"
if (Test-Path $ApkPath) {
    $ApkSizeMB = [math]::Round(((Get-Item $ApkPath).Length / 1MB), 2)
    Write-Host " [OK] APK verificado: $ApkPath ($ApkSizeMB MB)" -ForegroundColor Green
} else {
    Write-Host " [FALHA] APK não foi encontrado em: $ApkPath" -ForegroundColor Red
    $Failed = $true
}

# Relatório Final
Write-Host "`n==================================================" -ForegroundColor Cyan
if ($Failed) {
    Write-Host " AUDITORIA: REPROVADA. Corrija as falhas antes de prosseguir." -ForegroundColor Red
    Write-Host "==================================================" -ForegroundColor Cyan
    exit 1
} else {
    Write-Host " AUDITORIA: APROVADA. Código em conformidade com as regras." -ForegroundColor Green
    Write-Host "==================================================" -ForegroundColor Cyan
    exit 0
}
