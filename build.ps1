param(
    [switch]$Install
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$toolsRoot = if ($env:STS_COACH_TOOLS) {
    $env:STS_COACH_TOOLS
} else {
    Join-Path $env:USERPROFILE 'Tools'
}

# Prefer environment variables when they exist, but keep this checkout runnable
# with the user-scoped tools installed by the setup process.
$javaHome = if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME 'bin\javac.exe'))) {
    $env:JAVA_HOME
} else {
    Join-Path $toolsRoot 'temurin8\jdk8u504-b01'
}

$mavenHome = if ($env:MAVEN_HOME -and (Test-Path (Join-Path $env:MAVEN_HOME 'bin\mvn.cmd'))) {
    $env:MAVEN_HOME
} else {
    Join-Path $toolsRoot 'apache-maven-3.9.16'
}

$jarPaths = @{
    STS_JAR = if ($env:STS_JAR -and (Test-Path $env:STS_JAR)) {
        $env:STS_JAR
    } else {
        'C:\Program Files (x86)\Steam\steamapps\common\SlayTheSpire\desktop-1.0.jar'
    }
    BASEMOD_JAR = if ($env:BASEMOD_JAR -and (Test-Path $env:BASEMOD_JAR)) {
        $env:BASEMOD_JAR
    } else {
        Join-Path $toolsRoot 'sts-mod-deps\BaseMod.jar'
    }
    MODTHESPIRE_JAR = if ($env:MODTHESPIRE_JAR -and (Test-Path $env:MODTHESPIRE_JAR)) {
        $env:MODTHESPIRE_JAR
    } else {
        Join-Path $toolsRoot 'sts-mod-deps\ModTheSpire.jar'
    }
}

if (-not (Test-Path (Join-Path $javaHome 'bin\javac.exe'))) {
    throw "JDK 8 not found at $javaHome"
}
if (-not (Test-Path (Join-Path $mavenHome 'bin\mvn.cmd'))) {
    throw "Maven not found at $mavenHome"
}
foreach ($name in $jarPaths.Keys) {
    if (-not (Test-Path $jarPaths[$name])) {
        throw "$name not found at $($jarPaths[$name])"
    }
}

$env:JAVA_HOME = $javaHome
$env:MAVEN_HOME = $mavenHome
$env:STS_JAR = $jarPaths.STS_JAR
$env:BASEMOD_JAR = $jarPaths.BASEMOD_JAR
$env:MODTHESPIRE_JAR = $jarPaths.MODTHESPIRE_JAR
$env:Path = "$javaHome\bin;$mavenHome\bin;$env:Path"

Push-Location $projectRoot
try {
    & (Join-Path $mavenHome 'bin\mvn.cmd') '-Dgame-build=true' 'package'
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }

    if ($Install) {
        $gameRoot = Split-Path -Parent $jarPaths.STS_JAR
        $modsDir = Join-Path $gameRoot 'mods'
        New-Item -ItemType Directory -Path $modsDir -Force | Out-Null
        Copy-Item -LiteralPath (Join-Path $projectRoot 'target\SpireCoach.jar') `
            -Destination (Join-Path $modsDir 'SpireCoach.jar') -Force
        Write-Output "Installed to $modsDir\SpireCoach.jar"
    }
}
finally {
    Pop-Location
}
