$ErrorActionPreference = "Stop"

$launcherPath = Join-Path (Split-Path $PSScriptRoot -Parent) "run.vbs"
$content = Get-Content -Raw -LiteralPath $launcherPath

function Assert-LauncherContains {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Pattern,

        [Parameter(Mandatory = $true)]
        [string]$FailureMessage
    )

    if ($content -notmatch $Pattern) {
        throw $FailureMessage
    }
}

Assert-LauncherContains 'WScript\.Arguments\.Named\.Exists\("check"\)' `
    "run.vbs must provide a side-effect-free /check syntax mode."
Assert-LauncherContains 'mysql-8\.4\.3-winx64\\bin\\mysqld\.exe' `
    "run.vbs must point to the verified Laragon MySQL executable."
Assert-LauncherContains 'IsPortListening\(mysqlPort\)' `
    "run.vbs must verify MySQL readiness through port 3306."
Assert-LauncherContains 'StartMySqlAndWait' `
    "run.vbs must start MySQL and wait before launching the JAR."
Assert-LauncherContains 'mysqlWaitSeconds\s*=\s*60' `
    "run.vbs must have a bounded 60-second MySQL startup timeout."

$mysqlStartIndex = $content.IndexOf("If Not IsPortListening(mysqlPort) Then")
$javaStartIndex = $content.IndexOf('command = "javaw -Dfile.encoding=UTF-8')
if ($mysqlStartIndex -lt 0 -or $javaStartIndex -lt 0 -or $mysqlStartIndex -gt $javaStartIndex) {
    throw "MySQL readiness must be checked before javaw starts pvt.jar."
}

& cscript.exe //nologo $launcherPath /check
if ($LASTEXITCODE -ne 0) {
    throw "VBScript syntax validation failed with exit code $LASTEXITCODE."
}

Write-Output "run.vbs launcher checks passed."
