[CmdletBinding()]
param(
    [string]$DbHost = "127.0.0.1",
    [int]$Port = 3306,
    [string]$DbName = "onboarding_sys",
    [string]$User = "root",
    [AllowEmptyString()]
    [string]$Password = $env:DB_PASSWORD,
    [string]$MysqlExe = ""
)

$ErrorActionPreference = "Stop"

if (-not $MysqlExe) {
    $mysqlCommand = Get-Command "mysql" -ErrorAction SilentlyContinue
    if ($mysqlCommand) {
        $MysqlExe = $mysqlCommand.Source
    } elseif (Test-Path "C:\MySQL\MySQL Server 8.0\bin\mysql.exe") {
        $MysqlExe = "C:\MySQL\MySQL Server 8.0\bin\mysql.exe"
    } else {
        throw "mysql executable not found. Add MySQL bin to PATH or pass -MysqlExe."
    }
}

$sqlFile = Join-Path (Resolve-Path (Join-Path $PSScriptRoot "..")) "sql\onboarding_sys.sql"
if (-not (Test-Path $sqlFile)) {
    throw "Database script not found: $sqlFile"
}
if ($DbName -notmatch "^[A-Za-z0-9_]+$") {
    throw "DbName may only contain letters, numbers, and underscores."
}

$sqlText = Get-Content -Raw -Encoding UTF8 $sqlFile
$sqlText = $sqlText.Replace(
    "CREATE DATABASE IF NOT EXISTS onboarding_sys",
    "CREATE DATABASE IF NOT EXISTS $DbName")
$sqlText = $sqlText.Replace(
    "USE onboarding_sys;",
    "USE $DbName;")

$previousPassword = $env:MYSQL_PWD
try {
    if ($null -ne $Password) {
        $env:MYSQL_PWD = $Password
    }
    $sqlText | & $MysqlExe --protocol=TCP --host=$DbHost --port=$Port --user=$User --default-character-set=utf8mb4
    if ($LASTEXITCODE -ne 0) {
        throw "Database initialization failed with exit code $LASTEXITCODE."
    }
} finally {
    $env:MYSQL_PWD = $previousPassword
}

Write-Host "Database ${DbName} initialized at ${DbHost}:${Port}." -ForegroundColor Green
