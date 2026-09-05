param(
    [string]$GradleHome = 'D:\GradleHome',
    [string]$TempHome = 'D:\GradleTmp'
)
$ErrorActionPreference = 'Stop'
$oldHome = $env:GRADLE_USER_HOME
$oldTemp = $env:TEMP
$oldTmp = $env:TMP
$oldJavaOptions = $env:JAVA_TOOL_OPTIONS
try {
    foreach ($path in @($GradleHome, $TempHome)) {
        if ($path -match '[^\x20-\x7E]' -or $path -match '\s') {
            throw 'Choose absolute ASCII paths without spaces for GradleHome and TempHome.'
        }
        if (-not [System.IO.Path]::IsPathRooted($path)) { throw 'Paths must be absolute.' }
        New-Item -ItemType Directory -Force -Path $path | Out-Null
    }
    $env:GRADLE_USER_HOME = $GradleHome
    $env:TEMP = $TempHome
    $env:TMP = $TempHome
    $env:JAVA_TOOL_OPTIONS = (($oldJavaOptions + ' -Djava.io.tmpdir=' + $TempHome).Trim())
    Push-Location (Join-Path $PSScriptRoot '..')
    try {
        & .\gradlew.bat --no-daemon --no-build-cache --rerun-tasks --stacktrace clean build
        if ($LASTEXITCODE -ne 0) { throw "Gradle build failed with exit code $LASTEXITCODE" }
        # Validate actual JUnit execution without requiring Python on Windows.
        $report = 'build/test-results/test/TEST-com.harvester.vehicle.VehicleStateTest.xml'
        if (-not (Test-Path $report)) { throw 'VehicleStateTest XML is missing.' }
        [xml]$xml = Get-Content -Raw $report
        $suite = $xml.testsuite
        if ([int]$suite.failures -ne 0 -or [int]$suite.errors -ne 0 -or [int]$suite.skipped -ne 0) {
            throw 'VehicleStateTest contains failed, errored or skipped tests.'
        }
        $names = @($suite.testcase | ForEach-Object { $_.name -replace '\(\)$', '' })
        foreach ($expected in @('fullCargoRoundtripRetainsComponentsAndEverySlot', 'emptySlotsAndBrokenConditionSurvive', 'rejectsUnknownSchemaAndType', 'everyFamilyHasMultipleStableIds')) {
            if ($names -notcontains $expected) { throw "Test was not executed: $expected" }
        }
        Write-Host 'PASS: build and all expected VehicleStateTest methods completed.'
    } finally { Pop-Location }
} finally {
    $env:GRADLE_USER_HOME = $oldHome
    $env:TEMP = $oldTemp
    $env:TMP = $oldTmp
    $env:JAVA_TOOL_OPTIONS = $oldJavaOptions
}
