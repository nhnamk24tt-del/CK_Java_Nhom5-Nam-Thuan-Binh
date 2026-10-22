[CmdletBinding()]
param(
    [switch]$NoBrowser,
    [switch]$Foreground,
    [ValidateRange(1, 65535)]
    [int]$CustomerPort = 3000,
    [ValidateRange(1, 65535)]
    [int]$AdminPort = 3001
)

$ErrorActionPreference = 'Stop'
if ($CustomerPort -eq $AdminPort) {
    throw 'CustomerPort and AdminPort must be different.'
}
$root = $PSScriptRoot
$backend = Join-Path $root 'backend_xe'
$logDirectory = Join-Path $env:TEMP 'rentwheels-local'
New-Item -ItemType Directory -Path $logDirectory -Force | Out-Null

function Test-HttpEndpoint {
    param(
        [Parameter(Mandatory)]
        [string]$Url
    )

    try {
        return Invoke-WebRequest -Uri $Url -TimeoutSec 3 -UseBasicParsing
    }
    catch {
        return $null
    }
}

function Start-LocalServer {
    param(
        [Parameter(Mandatory)]
        [string]$Name,
        [Parameter(Mandatory)]
        [int]$Port,
        [Parameter(Mandatory)]
        [string]$HealthUrl,
        [Parameter(Mandatory)]
        [string]$InstanceRole,
        [Parameter(Mandatory)]
        [string]$FilePath,
        [Parameter(Mandatory)]
        [string[]]$ArgumentList,
        [Parameter(Mandatory)]
        [string]$WorkingDirectory,
        [switch]$Foreground
    )

    $response = Test-HttpEndpoint -Url $HealthUrl
    if ($response -and $response.StatusCode -eq 200) {
        $health = $response.Content | ConvertFrom-Json -ErrorAction SilentlyContinue
        if ($health.instanceRole -eq $InstanceRole) {
            Write-Host "$Name is already running: $HealthUrl"
            return
        }
    }

    $listener = Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue |
        Select-Object -First 1
    if ($listener) {
        throw "$Name cannot start because port $Port is occupied. Stop the other process or choose another port."
    }

    $stdout = Join-Path $logDirectory "$Name.stdout.log"
    $stderr = Join-Path $logDirectory "$Name.stderr.log"
    if ($Foreground) {
        $process = Start-Process -FilePath $FilePath `
            -ArgumentList $ArgumentList `
            -WorkingDirectory $WorkingDirectory `
            -NoNewWindow `
            -PassThru
    }
    else {
        $process = Start-Process -FilePath $FilePath `
            -ArgumentList $ArgumentList `
            -WorkingDirectory $WorkingDirectory `
            -WindowStyle Hidden `
            -RedirectStandardOutput $stdout `
            -RedirectStandardError $stderr `
            -PassThru
    }

    for ($attempt = 0; $attempt -lt 90; $attempt++) {
        Start-Sleep -Seconds 1
        $process.Refresh()

        if ($process.HasExited) {
            $errorLog = if (Test-Path $stderr) { Get-Content $stderr -Tail 20 } else { @() }
            $outputLog = if (Test-Path $stdout) { Get-Content $stdout -Tail 20 } else { @() }
            throw "$Name exited with code $($process.ExitCode).`n$($errorLog -join "`n")`n$($outputLog -join "`n")"
        }

        $response = Test-HttpEndpoint -Url $HealthUrl
        if ($response -and $response.StatusCode -eq 200) {
            $health = $response.Content | ConvertFrom-Json -ErrorAction SilentlyContinue
            if ($health.instanceRole -eq $InstanceRole) {
                Write-Host "$Name started: $HealthUrl"
                return $process
            }
        }
    }

    throw "$Name did not become ready. Check its logs in $logDirectory."
}

$envFile = Join-Path $backend '.env'
if (Test-Path $envFile) {
    foreach ($line in Get-Content $envFile) {
        if ($line -match '^\s*([^#\s][^=]*)\s*=\s*(.*)$') {
            $key = $matches[1].Trim()
            $value = $matches[2].Trim().Trim('"').Trim("'")
            Set-Item -Path "Env:$key" -Value $value
        }
    }
}

$java = Get-Command java.exe -ErrorAction Stop
$javaHome = $env:JAVA_HOME
if (-not $javaHome -or -not (Test-Path (Join-Path $javaHome 'bin\javac.exe'))) {
    $javaDiscoveryId = [guid]::NewGuid().ToString('N')
    $javaOutput = Join-Path $logDirectory "$javaDiscoveryId.stdout.log"
    $javaError = Join-Path $logDirectory "$javaDiscoveryId.stderr.log"
    try {
        $javaProcess = Start-Process -FilePath $java.Source `
            -ArgumentList @('-XshowSettings:properties', '-version') `
            -Wait -PassThru -WindowStyle Hidden `
            -RedirectStandardOutput $javaOutput `
            -RedirectStandardError $javaError
        if ($javaProcess.ExitCode -ne 0) {
            throw "Java failed while locating its JDK (exit code $($javaProcess.ExitCode))."
        }
        $javaProperties = @(Get-Content $javaOutput) + @(Get-Content $javaError)
    }
    finally {
        Remove-Item $javaOutput, $javaError -Force -ErrorAction SilentlyContinue
    }
    $javaHomeLine = $javaProperties | Select-String -Pattern '^\s*java.home\s*=\s*(.+)$' |
        Select-Object -First 1
    if (-not $javaHomeLine) {
        throw 'Could not locate a JDK. Install Java 17 or newer and set JAVA_HOME to its JDK directory.'
    }
    $javaHome = $javaHomeLine.Matches[0].Groups[1].Value.Trim()
}
$env:JAVA_HOME = $javaHome
$env:PATH = "$javaHome\bin;$env:PATH"

$wrapper = Join-Path $backend 'mvnw.cmd'
if (Test-Path $wrapper) {
    $buildExecutable = $wrapper
    $buildArguments = @('spring-boot:run')
}
else {
    $maven = Get-Command mvn.cmd -ErrorAction SilentlyContinue
    if (-not $maven) {
        $maven = Get-Command mvn -ErrorAction SilentlyContinue
    }
    if (-not $maven) {
        throw 'Maven Wrapper is missing. Run "mvn wrapper:wrapper" in backend_xe or install Maven.'
    }
    $buildExecutable = $maven.Source
    $buildArguments = @('spring-boot:run')
}

$env:ADMIN_URL = "http://localhost:$AdminPort"
$env:PORT = [string]$CustomerPort
$env:APP_INSTANCE_ROLE = 'user'
$env:SESSION_COOKIE_NAME = 'RENTWHEELS_USER_SESSION'
$startedProcesses = [System.Collections.ArrayList]::new()
$startupCompleted = $false
try {
    $customerProcess = Start-LocalServer -Name 'rentwheels-user' -Port $CustomerPort `
        -HealthUrl "http://localhost:$CustomerPort/api/health" -InstanceRole 'user' `
        -FilePath $buildExecutable -ArgumentList $buildArguments `
        -WorkingDirectory $backend -Foreground:$Foreground
    if ($customerProcess) {
        [void]$startedProcesses.Add($customerProcess)
    }

    $env:PORT = [string]$AdminPort
    $env:APP_INSTANCE_ROLE = 'admin'
    $env:SESSION_COOKIE_NAME = 'RENTWHEELS_ADMIN_SESSION'
    $adminProcess = Start-LocalServer -Name 'rentwheels-admin' -Port $AdminPort `
        -HealthUrl "http://localhost:$AdminPort/api/health" -InstanceRole 'admin' `
        -FilePath $buildExecutable -ArgumentList $buildArguments `
        -WorkingDirectory $backend -Foreground:$Foreground
    if ($adminProcess) {
        [void]$startedProcesses.Add($adminProcess)
    }

    Write-Host ''
    Write-Host 'RentWheels is ready:'
    Write-Host "  Customer: http://localhost:$CustomerPort/vehicles"
    Write-Host "  Admin:    http://localhost:$AdminPort/admin/dashboard"
    Write-Host "  User API: http://localhost:$CustomerPort/api"
    Write-Host "  Admin API: http://localhost:$AdminPort/api"
    if (-not $Foreground) {
        Write-Host "  Logs:     $logDirectory"
    }

    if (-not $NoBrowser) {
        Start-Process "http://localhost:$CustomerPort/vehicles"
    }

    $startupCompleted = $true
    if ($Foreground) {
        Write-Host ''
        Write-Host 'Running in this terminal. Press Ctrl+C to stop the instances started here.'
        while ($startedProcesses.Count -gt 0) {
            $runningProcesses = @($startedProcesses | Where-Object {
                $_.Refresh()
                -not $_.HasExited
            })
            if ($runningProcesses.Count -eq 0) {
                break
            }
            Start-Sleep -Seconds 1
        }
    }
}
finally {
    if ($Foreground -or -not $startupCompleted) {
        foreach ($process in $startedProcesses) {
            $process.Refresh()
            if (-not $process.HasExited) {
                Stop-Process -Id $process.Id -Force -ErrorAction SilentlyContinue
            }
        }
    }
}
