[CmdletBinding()]
param(
    [string]$DbHost = "127.0.0.1",
    [int]$DbPort = 3306,
    [string]$DbName = "onboarding_sys",
    [string]$DbUsername = "root",
    [AllowEmptyString()]
    [string]$DbPassword = $env:DB_PASSWORD,
    [int]$ServerPort = 8080,
    [string]$LogFile = "logs/onboarding-system.log"
)

$ErrorActionPreference = "Stop"
$projectRoot = Resolve-Path (Join-Path $PSScriptRoot "..")

$env:DB_URL = "jdbc:mysql://${DbHost}:${DbPort}/${DbName}?serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true"
$env:DB_USERNAME = $DbUsername
$env:DB_PASSWORD = $DbPassword
$env:SERVER_PORT = $ServerPort
$env:LOG_FILE = $LogFile

Push-Location $projectRoot
try {
    Write-Host "Starting backend at http://127.0.0.1:$ServerPort" -ForegroundColor Cyan
    mvn spring-boot:run
} finally {
    Pop-Location
}
