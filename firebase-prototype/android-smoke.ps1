param(
    [Parameter(Mandatory)][ValidatePattern('^emulator-\d+$')][string]$OwnerDevice,
    [Parameter(Mandatory)][ValidatePattern('^emulator-\d+$')][string]$ManagerDevice,
    [ValidateRange(0, 99969)][int]$StartDay = 23000
)

$ErrorActionPreference = 'Stop'
if ($OwnerDevice -eq $ManagerDevice) { throw 'Use two distinct Android emulators.' }

$sdk = $env:ANDROID_HOME
if (-not $sdk) { $sdk = Join-Path $env:LOCALAPPDATA 'Android/Sdk' }
$adb = Join-Path $sdk 'platform-tools/adb.exe'
$root = Split-Path -Parent $PSScriptRoot
$appApk = Join-Path $root 'app/build/outputs/apk/debug/app-debug.apk'
$testApk = Join-Path $root 'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk'
$runId = [guid]::NewGuid().ToString('N')

foreach ($device in @($OwnerDevice, $ManagerDevice)) {
    $deadline = (Get-Date).AddMinutes(5)
    do {
        $booted = & $adb -s $device shell getprop sys.boot_completed
        if ($booted -eq '1') { break }
        if ((Get-Date) -ge $deadline) { throw "$device did not finish booting." }
        Start-Sleep -Seconds 2
    } while ($true)

    foreach ($apk in @($appApk, $testApk)) {
        & $adb -s $device install -r $apk
        if ($LASTEXITCODE -ne 0) { throw "Installation failed on $device." }
    }
}

function Invoke-BookingStage($device, $role, $method) {
    $arguments = @(
        '-s', $device, 'shell', 'am', 'instrument', '-w',
        '-e', 'bookingSmoke', 'true', '-e', 'runId', $runId,
        '-e', 'startDay', $StartDay, '-e', 'role', $role,
        '-e', 'class', "com.gzone.guesthousebooking.BookingApiSmokeTest#$method",
        'com.gzone.guesthousebooking.test/androidx.test.runner.AndroidJUnitRunner'
    )
    $result = & $adb @arguments
    $result | Write-Output
    if ($LASTEXITCODE -ne 0 -or ($result -join ' ') -notmatch 'OK \(1 test\)') {
        throw "$method failed on $device."
    }
}

Invoke-BookingStage $OwnerDevice 'owner' 'ownerCreatesGroup'
Invoke-BookingStage $ManagerDevice 'manager' 'managerSeesConflictThenReleasesSelectedRoom'
Invoke-BookingStage $OwnerDevice 'owner' 'ownerCanBookReleasedRoomAndConnectionFailureIsUnconfirmed'
