[CmdletBinding()]
param(
    [switch]$SkipInstall
)

$ErrorActionPreference = "Stop"
$frontendRoot = Resolve-Path (Join-Path $PSScriptRoot "..\frontend")

if (-not (Get-Command "node" -ErrorAction SilentlyContinue)) {
    throw "Node.js is not installed or is not available in PATH."
}

Push-Location $frontendRoot
try {
    if (-not $SkipInstall -and -not (Test-Path "node_modules")) {
        npm install
        if ($LASTEXITCODE -ne 0) {
            throw "npm install failed with exit code $LASTEXITCODE."
        }
    }
    Write-Host "Starting frontend at http://127.0.0.1:5173" -ForegroundColor Cyan
    npm run dev -- --port 5173 --strictPort
} finally {
    Pop-Location
}
