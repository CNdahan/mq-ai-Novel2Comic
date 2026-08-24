$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$frontendRoot = Join-Path $root 'novel2comic-frontend'
$backendJar = Join-Path $root 'target\novel2comic-0.0.1-SNAPSHOT.jar'
$mysqlExe = if ($env:MYSQL_EXE) {
    $env:MYSQL_EXE
} else {
    'C:\Program Files\MySQL\MySQL Server 8.4\bin\mysqld.exe'
}

function Test-Port([int]$Port) {
    try {
        return (Test-NetConnection localhost -Port $Port `
            -WarningAction SilentlyContinue).TcpTestSucceeded
    } catch {
        return $false
    }
}

function Get-ListeningProcess([int]$Port) {
    $connection = Get-NetTCPConnection -LocalPort $Port -State Listen `
        -ErrorAction SilentlyContinue | Select-Object -First 1
    if (-not $connection) {
        return $null
    }
    return Get-Process -Id $connection.OwningProcess -ErrorAction SilentlyContinue
}

function Resolve-MySqlConfig {
    if ($env:MYSQL_CONFIG -and (Test-Path -LiteralPath $env:MYSQL_CONFIG)) {
        return $env:MYSQL_CONFIG
    }

    $candidates = @(
        (Join-Path $root 'my.ini'),
        'D:\mysql-novel2comic-fresh3\my.ini',
        'D:\mysql-novel2comic\my.ini'
    )

    return $candidates |
        Where-Object { Test-Path -LiteralPath $_ } |
        Select-Object -First 1
}

function Resolve-Java {
    $candidates = @()
    if ($env:JAVA_HOME) {
        $candidates += (Join-Path $env:JAVA_HOME 'bin\java.exe')
    }
    $candidates += @(
        'C:\Program Files\Eclipse Adoptium\jdk-21.0.12.8-hotspot\bin\java.exe',
        'C:\Program Files\Java\jdk-21\bin\java.exe'
    )

    $resolved = $candidates |
        Where-Object { Test-Path -LiteralPath $_ } |
        Select-Object -First 1
    if ($resolved) {
        return $resolved
    }

    $javaCommand = Get-Command java.exe -ErrorAction SilentlyContinue
    if ($javaCommand) {
        return $javaCommand.Source
    }

    throw 'Java not found. Install JDK 21 or set JAVA_HOME.'
}

$mysqlListener = Get-ListeningProcess 3306
if ($mysqlListener) {
    if ($mysqlListener.ProcessName -notmatch '^mysqld$') {
        throw "Port 3306 is already used by $($mysqlListener.ProcessName) (PID $($mysqlListener.Id))."
    }
    Write-Host "MySQL already running (PID $($mysqlListener.Id))."
} else {
    $mysqlConfig = Resolve-MySqlConfig
    if (-not $mysqlConfig) {
        throw 'MySQL config not found. Set MYSQL_CONFIG or add my.ini to the project root.'
    }
    if (-not (Test-Path -LiteralPath $mysqlExe)) {
        throw "MySQL executable not found: $mysqlExe"
    }

    Start-Process -FilePath $mysqlExe `
        -ArgumentList @("--defaults-file=$mysqlConfig", '--console') `
        -WorkingDirectory (Split-Path -Parent $mysqlExe) `
        -WindowStyle Hidden `
        -RedirectStandardOutput (Join-Path $root 'mysql.out.log') `
        -RedirectStandardError (Join-Path $root 'mysql.err.log')

    $deadline = (Get-Date).AddSeconds(30)
    while ((Get-Date) -lt $deadline -and -not (Test-Port 3306)) {
        Start-Sleep -Seconds 1
    }
    if (-not (Test-Port 3306)) {
        throw 'MySQL failed to start. Check mysql.err.log.'
    }
    $mysqlListener = Get-ListeningProcess 3306
    if (-not $mysqlListener -or $mysqlListener.ProcessName -notmatch '^mysqld$') {
        throw 'Port 3306 became available, but the listener is not mysqld. Check mysql.err.log.'
    }
    Write-Host "MySQL started (PID $($mysqlListener.Id)) using $mysqlConfig."
}

$backendListener = Get-ListeningProcess 8123
if (-not $backendListener) {
    if (-not (Test-Path -LiteralPath $backendJar)) {
        throw "Backend jar not found: $backendJar. Run .\mvnw.cmd -DskipTests package first."
    }

    $java = Resolve-Java
    Start-Process -FilePath $java `
        -ArgumentList @('-jar', ('"' + $backendJar + '"')) `
        -WorkingDirectory $root `
        -WindowStyle Hidden `
        -RedirectStandardOutput (Join-Path $root 'backend.out.log') `
        -RedirectStandardError (Join-Path $root 'backend.err.log')
    Write-Host 'Backend start requested on port 8123.'
} else {
    Write-Host "Backend already running (PID $($backendListener.Id))."
}

if (-not (Test-Path -LiteralPath (Join-Path $frontendRoot 'node_modules'))) {
    Push-Location $frontendRoot
    try {
        npm install
    } finally {
        Pop-Location
    }
}

$frontendListener = Get-ListeningProcess 5173
if (-not $frontendListener) {
    Start-Process -FilePath 'cmd.exe' `
        -ArgumentList @('/c', 'npm run dev -- --host 0.0.0.0 --port 5173') `
        -WorkingDirectory $frontendRoot `
        -WindowStyle Hidden `
        -RedirectStandardOutput (Join-Path $root 'frontend.out.log') `
        -RedirectStandardError (Join-Path $root 'frontend.err.log')
    Write-Host 'Frontend start requested on port 5173.'
} else {
    Write-Host "Frontend already running (PID $($frontendListener.Id))."
}

Start-Sleep -Seconds 3
Start-Process 'http://localhost:5173/'
Write-Host 'MySQL, backend, and frontend started: http://localhost:5173/'
