# ============================================================
#  NPU Check — Neural Processing Unit Detection Utility
#  Scans your Windows system for NPU hardware and generates
#  a beautiful HTML report.
# ============================================================

$ErrorActionPreference = "SilentlyContinue"

# ── 1. Gather System Info ────────────────────────────────────

$os = Get-CimInstance Win32_OperatingSystem
$cpu = Get-CimInstance Win32_Processor | Select-Object -First 1

$osName = $os.Caption
$osBuild = $os.BuildNumber
$osVersion = $os.Version
$cpuName = $cpu.Name.Trim()
$cpuCores = $cpu.NumberOfCores
$cpuThreads = $cpu.NumberOfLogicalProcessors

# Determine Windows 11 update version
$displayVersion = (Get-ItemProperty -Path "HKLM:\SOFTWARE\Microsoft\Windows NT\CurrentVersion" -Name DisplayVersion -ErrorAction SilentlyContinue).DisplayVersion
if (-not $displayVersion) { $displayVersion = "Unknown" }

# ── 2. Detect NPU Devices ───────────────────────────────────

# Search PnP devices for known NPU identifiers
$npuKeywords = @(
    "Neural",
    "NPU",
    "AI Accelerator",
    "AI Boost",
    "Machine Learning",
    "Hexagon",
    "XDNA",
    "IPU",
    "Intel(R) AI",
    "Qualcomm(R) AI",
    "AMD AI",
    "Microsoft(R) NPU"
)

$allDevices = Get-PnpDevice -ErrorAction SilentlyContinue | Where-Object { $_.Status -eq "OK" -or $_.Status -eq "Error" -or $_.Status -eq "Degraded" -or $_.Status -eq "Unknown" }

$npuDevices = @()
foreach ($device in $allDevices) {
    foreach ($keyword in $npuKeywords) {
        if ($device.FriendlyName -like "*$keyword*" -or $device.Class -like "*$keyword*") {
            $npuDevices += $device
            break
        }
    }
}

# Also check specifically for Intel NPU driver via service
$intelNpuDriver = Get-PnpDevice -ErrorAction SilentlyContinue | Where-Object {
    $_.FriendlyName -like "*Intel*NPU*" -or
    $_.FriendlyName -like "*Intel*AI Boost*" -or
    $_.FriendlyName -like "*Intel*AI*Accelerator*"
}

# Check for Qualcomm NPU
$qualcommNpu = Get-PnpDevice -ErrorAction SilentlyContinue | Where-Object {
    $_.FriendlyName -like "*Qualcomm*NPU*" -or
    $_.FriendlyName -like "*Hexagon*" -or
    $_.FriendlyName -like "*Qualcomm*AI*"
}

# Check for AMD XDNA
$amdNpu = Get-PnpDevice -ErrorAction SilentlyContinue | Where-Object {
    $_.FriendlyName -like "*AMD*NPU*" -or
    $_.FriendlyName -like "*AMD*XDNA*" -or
    $_.FriendlyName -like "*AMD*AI*" -or
    $_.FriendlyName -like "*AMD IPU*"
}

# Merge all found NPU devices (deduplicate)
$allNpuDevices = @()
$seenIds = @{}
foreach ($d in ($npuDevices + $intelNpuDriver + $qualcommNpu + $amdNpu)) {
    if ($d -and -not $seenIds.ContainsKey($d.InstanceId)) {
        $seenIds[$d.InstanceId] = $true
        $allNpuDevices += $d
    }
}

$npuFound = $allNpuDevices.Count -gt 0

# ── 3. CPU-based NPU Eligibility ────────────────────────────

$npuCapableCpu = $false
$npuCpuReason = ""

if ($cpuName -match "Core.*Ultra" -or $cpuName -match "Core\(TM\) Ultra") {
    $npuCapableCpu = $true
    $npuCpuReason = "Intel Core Ultra series - includes Intel AI Boost NPU"
}
elseif ($cpuName -match "Snapdragon.*X" -or $cpuName -match "Qualcomm.*Snapdragon") {
    $npuCapableCpu = $true
    $npuCpuReason = "Qualcomm Snapdragon X series - includes Hexagon NPU"
}
elseif ($cpuName -match "Ryzen.*AI" -or $cpuName -match "AMD.*XDNA") {
    $npuCapableCpu = $true
    $npuCpuReason = "AMD Ryzen AI series - includes AMD XDNA NPU"
}
elseif ($cpuName -match "Meteor Lake" -or $cpuName -match "Lunar Lake" -or $cpuName -match "Arrow Lake") {
    $npuCapableCpu = $true
    $npuCpuReason = "Intel codename detected - likely includes NPU"
}
else {
    $npuCpuReason = "CPU model not recognized as NPU-capable (no Intel Core Ultra, Snapdragon X, or Ryzen AI detected)"
}

# ── 4. Check for AI / ML Runtimes ───────────────────────────

# DirectML
$directmlAvailable = $false
$directmlPath = Get-ChildItem -Path "$env:SystemRoot\System32" -Filter "DirectML.dll" -ErrorAction SilentlyContinue
if ($directmlPath) { $directmlAvailable = $true }

# ONNX Runtime
$onnxAvailable = $false
$onnxPaths = @(
    "$env:ProgramFiles\ONNX Runtime",
    "$env:LOCALAPPDATA\Programs\ONNX Runtime",
    "$env:ProgramFiles\Microsoft\ONNX Runtime"
)
foreach ($p in $onnxPaths) {
    if (Test-Path $p) { $onnxAvailable = $true; break }
}
# Also check pip
$pipOnnx = pip show onnxruntime 2>$null
if ($pipOnnx) { $onnxAvailable = $true }
$pipOnnxDml = pip show onnxruntime-directml 2>$null
if ($pipOnnxDml) { $onnxAvailable = $true }

# OpenVINO
$openvinoAvailable = $false
if ($env:INTEL_OPENVINO_DIR -or (Test-Path "$env:ProgramFiles\Intel\openvino*")) {
    $openvinoAvailable = $true
}
$pipOpenvino = pip show openvino 2>$null
if ($pipOpenvino) { $openvinoAvailable = $true }

# Windows ML / WinAI
$windowsMLAvailable = $false
$winmlDll = Get-ChildItem -Path "$env:SystemRoot\System32" -Filter "windows.ai.machinelearning.dll" -ErrorAction SilentlyContinue
if ($winmlDll) { $windowsMLAvailable = $true }

# ── 5. NPU Driver Info ──────────────────────────────────────

$npuDriverInfo = @()
foreach ($dev in $allNpuDevices) {
    $driverInfo = Get-CimInstance Win32_PnPSignedDriver -ErrorAction SilentlyContinue |
    Where-Object { $_.DeviceID -eq $dev.InstanceId } | Select-Object -First 1

    $npuDriverInfo += @{
        Name          = $dev.FriendlyName
        Status        = $dev.Status
        Class         = $dev.Class
        DriverVersion = if ($driverInfo) { $driverInfo.DriverVersion } else { "N/A" }
        DriverDate    = if ($driverInfo -and $driverInfo.DriverDate) { $driverInfo.DriverDate.ToString("yyyy-MM-dd") } else { "N/A" }
        Manufacturer  = if ($driverInfo) { $driverInfo.Manufacturer } else { "N/A" }
    }
}

# ── 6. OS Compatibility Check ───────────────────────────────

$osCompatible = $false
# Windows 11 22H2+ (build 22621+) has NPU support; 24H2 (26100+) has full Copilot+ support
if ([int]$osBuild -ge 22621) {
    $osCompatible = $true
}

$copilotPlusReady = $false
if ([int]$osBuild -ge 26100) {
    $copilotPlusReady = $true
}

# ── 7. Build NPU Device List HTML ───────────────────────────

$checkMark = [char]0x2713
$crossMark = [char]0x2717
$warnMark = [char]0x26A0

$npuDeviceCardsHtml = ""
if ($allNpuDevices.Count -gt 0) {
    foreach ($info in $npuDriverInfo) {
        $statusIcon = if ($info.Status -eq "OK") { $checkMark } else { $warnMark }
        $statusClass = if ($info.Status -eq "OK") { "status-ok" } else { "status-warn" }
        $npuDeviceCardsHtml += @"
            <div class="device-card">
                <div class="device-header">
                    <span class="$statusClass">$statusIcon</span>
                    <strong>$($info.Name)</strong>
                </div>
                <div class="device-details">
                    <div class="detail-row"><span class="label">Status</span><span class="value $statusClass">$($info.Status)</span></div>
                    <div class="detail-row"><span class="label">Class</span><span class="value">$($info.Class)</span></div>
                    <div class="detail-row"><span class="label">Driver Version</span><span class="value">$($info.DriverVersion)</span></div>
                    <div class="detail-row"><span class="label">Driver Date</span><span class="value">$($info.DriverDate)</span></div>
                    <div class="detail-row"><span class="label">Manufacturer</span><span class="value">$($info.Manufacturer)</span></div>
                </div>
            </div>
"@
    }
}
else {
    $npuDeviceCardsHtml = @"
        <div class="device-card no-device">
            <div class="device-header">
                <span class="status-error">$crossMark</span>
                <strong>No NPU Device Detected</strong>
            </div>
            <p class="no-device-msg">No Neural Processing Unit was found in your system's device list.</p>
        </div>
"@
}

# ── 8. Build Checklist HTML ──────────────────────────────────

function CheckItem($label, $pass, $detail) {
    $icon = if ($pass) { $checkMark } else { $crossMark }
    $cls = if ($pass) { "check-pass" } else { "check-fail" }
    return "<div class='check-item $cls'><span class='check-icon'>$icon</span><div><strong>$label</strong><span class='check-detail'>$detail</span></div></div>"
}

$checklistHtml = CheckItem "NPU Hardware Detected" $npuFound $(if ($npuFound) { "$($allNpuDevices.Count) NPU device(s) found" } else { "No NPU hardware found in Device Manager" })
$checklistHtml += CheckItem "NPU-Capable CPU" $npuCapableCpu $npuCpuReason
$checklistHtml += CheckItem "Windows Version Compatible" $osCompatible "Windows Build $osBuild ($displayVersion) - requires 22621+"
$checklistHtml += CheckItem "Copilot+ PC Ready" $copilotPlusReady $(if ($copilotPlusReady) { "Build $osBuild meets Copilot+ requirements (26100+)" } else { "Requires Windows 11 24H2 (Build 26100+)" })
$checklistHtml += CheckItem "DirectML Available" $directmlAvailable $(if ($directmlAvailable) { "DirectML.dll found in System32" } else { "DirectML.dll not found" })
$checklistHtml += CheckItem "Windows ML Runtime" $windowsMLAvailable $(if ($windowsMLAvailable) { "Windows.AI.MachineLearning.dll present" } else { "Not found" })
$checklistHtml += CheckItem "ONNX Runtime" $onnxAvailable $(if ($onnxAvailable) { "ONNX Runtime detected" } else { "Not installed (pip install onnxruntime-directml)" })
$checklistHtml += CheckItem "OpenVINO Toolkit" $openvinoAvailable $(if ($openvinoAvailable) { "OpenVINO detected" } else { "Not installed (optional, for Intel NPUs)" })

# ── 9. Overall Verdict ──────────────────────────────────────

$overallScore = 0
if ($npuFound) { $overallScore += 3 }
if ($npuCapableCpu) { $overallScore += 2 }
if ($osCompatible) { $overallScore += 1 }
if ($copilotPlusReady) { $overallScore += 1 }
if ($directmlAvailable) { $overallScore += 1 }

$rocketEmoji = [char]::ConvertFromUtf32(0x1F680)
$boltEmoji = [char]0x26A1
$warnEmoji = [char]0x26A0
$crossEmoji = [char]0x274C

if ($overallScore -ge 5) {
    $verdictClass = "verdict-great"
    $verdictIcon = $rocketEmoji
    $verdictTitle = "NPU Ready!"
    $verdictMsg = "Your system has a Neural Processing Unit and is configured for AI acceleration."
}
elseif ($overallScore -ge 3) {
    $verdictClass = "verdict-ok"
    $verdictIcon = $boltEmoji
    $verdictTitle = "Partially Ready"
    $verdictMsg = "NPU hardware detected but some components may need updates. Check the details below."
}
elseif ($npuCapableCpu) {
    $verdictClass = "verdict-warn"
    $verdictIcon = $warnEmoji
    $verdictTitle = "NPU-Capable CPU, But Not Detected"
    $verdictMsg = "Your CPU supports an NPU, but it was not detected. Check drivers and BIOS settings."
}
else {
    $verdictClass = "verdict-none"
    $verdictIcon = $crossEmoji
    $verdictTitle = "No NPU Detected"
    $verdictMsg = "Your system does not appear to have a Neural Processing Unit."
}

# ── 10. Timestamp ────────────────────────────────────────────

$timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"

# ── 11. Generate HTML Report ────────────────────────────────

$html = @"
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>NPU Check Report</title>
<style>
  @import url('https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800&display=swap');
  
  *, *::before, *::after { margin: 0; padding: 0; box-sizing: border-box; }

  :root {
    --bg-primary: #0a0e1a;
    --bg-card: rgba(255,255,255,0.04);
    --bg-card-hover: rgba(255,255,255,0.07);
    --border: rgba(255,255,255,0.08);
    --text-primary: #e8eaf0;
    --text-secondary: #8b92a8;
    --text-muted: #555d74;
    --accent-blue: #4f8ff7;
    --accent-purple: #8b5cf6;
    --accent-green: #22c55e;
    --accent-red: #ef4444;
    --accent-amber: #f59e0b;
    --accent-cyan: #06b6d4;
    --glow-blue: rgba(79,143,247,0.15);
    --glow-green: rgba(34,197,94,0.15);
    --glow-red: rgba(239,68,68,0.12);
  }

  body {
    font-family: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
    background: var(--bg-primary);
    color: var(--text-primary);
    min-height: 100vh;
    overflow-x: hidden;
  }

  /* Animated background */
  body::before {
    content: '';
    position: fixed;
    top: -50%;
    left: -50%;
    width: 200%;
    height: 200%;
    background: radial-gradient(ellipse at 20% 50%, rgba(79,143,247,0.08) 0%, transparent 50%),
                radial-gradient(ellipse at 80% 20%, rgba(139,92,246,0.06) 0%, transparent 50%),
                radial-gradient(ellipse at 50% 80%, rgba(6,182,212,0.05) 0%, transparent 50%);
    animation: bgShift 20s ease-in-out infinite alternate;
    z-index: -1;
  }
  @keyframes bgShift {
    0% { transform: translate(0, 0) rotate(0deg); }
    100% { transform: translate(-5%, 3%) rotate(3deg); }
  }

  .container {
    max-width: 900px;
    margin: 0 auto;
    padding: 40px 24px 80px;
  }

  /* Header */
  .header {
    text-align: center;
    margin-bottom: 48px;
    animation: fadeInDown 0.6s ease-out;
  }
  .header-logo {
    font-size: 48px;
    margin-bottom: 8px;
  }
  .header h1 {
    font-size: 32px;
    font-weight: 800;
    letter-spacing: -0.5px;
    background: linear-gradient(135deg, var(--accent-blue), var(--accent-purple), var(--accent-cyan));
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
    margin-bottom: 8px;
  }
  .header p {
    color: var(--text-secondary);
    font-size: 14px;
    font-weight: 400;
  }
  .timestamp {
    color: var(--text-muted);
    font-size: 12px;
    margin-top: 4px;
    font-family: monospace;
  }

  /* Verdict Hero */
  .verdict {
    border-radius: 20px;
    padding: 40px 32px;
    text-align: center;
    margin-bottom: 36px;
    border: 1px solid var(--border);
    animation: fadeInUp 0.7s ease-out 0.1s both;
    position: relative;
    overflow: hidden;
  }
  .verdict::before {
    content: '';
    position: absolute;
    top: 0; left: 0; right: 0;
    height: 3px;
    border-radius: 20px 20px 0 0;
  }
  .verdict-great { background: linear-gradient(135deg, rgba(34,197,94,0.1), rgba(6,182,212,0.06)); }
  .verdict-great::before { background: linear-gradient(90deg, var(--accent-green), var(--accent-cyan)); }
  .verdict-ok { background: linear-gradient(135deg, rgba(79,143,247,0.1), rgba(139,92,246,0.06)); }
  .verdict-ok::before { background: linear-gradient(90deg, var(--accent-blue), var(--accent-purple)); }
  .verdict-warn { background: linear-gradient(135deg, rgba(245,158,11,0.1), rgba(239,68,68,0.05)); }
  .verdict-warn::before { background: linear-gradient(90deg, var(--accent-amber), var(--accent-red)); }
  .verdict-none { background: linear-gradient(135deg, rgba(239,68,68,0.08), rgba(85,93,116,0.06)); }
  .verdict-none::before { background: linear-gradient(90deg, var(--accent-red), var(--text-muted)); }

  .verdict-icon { font-size: 56px; margin-bottom: 12px; }
  .verdict-title { font-size: 28px; font-weight: 700; margin-bottom: 8px; }
  .verdict-msg { color: var(--text-secondary); font-size: 15px; line-height: 1.6; max-width: 600px; margin: 0 auto; }

  /* Section */
  .section {
    margin-bottom: 32px;
    animation: fadeInUp 0.7s ease-out both;
  }
  .section:nth-child(3) { animation-delay: 0.2s; }
  .section:nth-child(4) { animation-delay: 0.3s; }
  .section:nth-child(5) { animation-delay: 0.4s; }

  .section-title {
    font-size: 13px;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 1.5px;
    color: var(--text-muted);
    margin-bottom: 16px;
    padding-left: 4px;
  }

  /* Info Grid */
  .info-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
    gap: 12px;
  }
  .info-card {
    background: var(--bg-card);
    border: 1px solid var(--border);
    border-radius: 14px;
    padding: 20px;
    transition: all 0.25s ease;
  }
  .info-card:hover {
    background: var(--bg-card-hover);
    border-color: rgba(255,255,255,0.12);
    transform: translateY(-2px);
  }
  .info-card .label {
    font-size: 11px;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 1px;
    color: var(--text-muted);
    margin-bottom: 8px;
  }
  .info-card .value {
    font-size: 15px;
    font-weight: 500;
    color: var(--text-primary);
    word-break: break-word;
  }

  /* Device Card */
  .device-card {
    background: var(--bg-card);
    border: 1px solid var(--border);
    border-radius: 14px;
    padding: 24px;
    margin-bottom: 12px;
    transition: all 0.25s ease;
  }
  .device-card:hover {
    background: var(--bg-card-hover);
    transform: translateY(-2px);
  }
  .device-header {
    display: flex;
    align-items: center;
    gap: 12px;
    font-size: 17px;
    margin-bottom: 16px;
  }
  .device-details {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
    gap: 10px;
  }
  .detail-row {
    display: flex;
    flex-direction: column;
    gap: 2px;
  }
  .detail-row .label {
    font-size: 11px;
    text-transform: uppercase;
    letter-spacing: 0.8px;
    color: var(--text-muted);
  }
  .detail-row .value {
    font-size: 14px;
    color: var(--text-secondary);
  }
  .no-device { border-style: dashed; }
  .no-device-msg { color: var(--text-muted); font-size: 14px; margin-top: 4px; }

  .status-ok { color: var(--accent-green); }
  .status-warn { color: var(--accent-amber); }
  .status-error { color: var(--accent-red); font-size: 20px; }

  /* Checklist */
  .checklist {
    display: flex;
    flex-direction: column;
    gap: 8px;
  }
  .check-item {
    display: flex;
    align-items: flex-start;
    gap: 14px;
    background: var(--bg-card);
    border: 1px solid var(--border);
    border-radius: 12px;
    padding: 16px 20px;
    transition: all 0.25s ease;
  }
  .check-item:hover {
    background: var(--bg-card-hover);
    transform: translateX(4px);
  }
  .check-icon {
    font-size: 18px;
    flex-shrink: 0;
    margin-top: 1px;
    width: 24px;
    text-align: center;
  }
  .check-pass .check-icon { color: var(--accent-green); }
  .check-fail .check-icon { color: var(--accent-red); }
  .check-item strong {
    display: block;
    font-size: 14px;
    font-weight: 600;
    margin-bottom: 2px;
  }
  .check-detail {
    font-size: 12px;
    color: var(--text-muted);
    line-height: 1.5;
  }

  /* Footer */
  .footer {
    text-align: center;
    padding-top: 48px;
    color: var(--text-muted);
    font-size: 12px;
    animation: fadeInUp 0.7s ease-out 0.5s both;
  }
  .footer a {
    color: var(--accent-blue);
    text-decoration: none;
  }

  /* Animations */
  @keyframes fadeInDown {
    from { opacity: 0; transform: translateY(-20px); }
    to   { opacity: 1; transform: translateY(0); }
  }
  @keyframes fadeInUp {
    from { opacity: 0; transform: translateY(20px); }
    to   { opacity: 1; transform: translateY(0); }
  }

  /* Responsive */
  @media (max-width: 600px) {
    .container { padding: 24px 16px 60px; }
    .header h1 { font-size: 24px; }
    .verdict { padding: 28px 20px; }
    .verdict-icon { font-size: 40px; }
    .verdict-title { font-size: 22px; }
    .info-grid { grid-template-columns: 1fr 1fr; }
    .device-details { grid-template-columns: 1fr; }
  }
</style>
</head>
<body>
<div class="container">

  <!-- Header -->
  <div class="header">
    <div class="header-logo">$([char]::ConvertFromUtf32(0x1F9E0))</div>
    <h1>NPU Check Report</h1>
    <p>Neural Processing Unit detection results for your system</p>
    <div class="timestamp">Scanned: $timestamp</div>
  </div>

  <!-- Verdict -->
  <div class="verdict $verdictClass">
    <div class="verdict-icon">$verdictIcon</div>
    <div class="verdict-title">$verdictTitle</div>
    <div class="verdict-msg">$verdictMsg</div>
  </div>

  <!-- System Info -->
  <div class="section">
    <div class="section-title">System Information</div>
    <div class="info-grid">
      <div class="info-card">
        <div class="label">Processor</div>
        <div class="value">$cpuName</div>
      </div>
      <div class="info-card">
        <div class="label">Cores / Threads</div>
        <div class="value">$cpuCores C / $cpuThreads T</div>
      </div>
      <div class="info-card">
        <div class="label">Operating System</div>
        <div class="value">$osName</div>
      </div>
      <div class="info-card">
        <div class="label">Build / Version</div>
        <div class="value">$osBuild ($displayVersion)</div>
      </div>
    </div>
  </div>

  <!-- NPU Devices -->
  <div class="section">
    <div class="section-title">NPU Devices</div>
    $npuDeviceCardsHtml
  </div>

  <!-- Compatibility Checklist -->
  <div class="section">
    <div class="section-title">Compatibility Checklist</div>
    <div class="checklist">
      $checklistHtml
    </div>
  </div>

  <!-- Footer -->
  <div class="footer">
    NPU Check - Neural Processing Unit Detection Utility<br>
    Generated on $timestamp
  </div>

</div>
</body>
</html>
"@

# ── 12. Write and Open Report ────────────────────────────────

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$reportPath = Join-Path $scriptDir "NPU_Check_Report.html"

$html | Out-File -FilePath $reportPath -Encoding utf8 -Force

Write-Host ""
Write-Host "  ====================================" -ForegroundColor Cyan
Write-Host "        NPU Check Complete!           " -ForegroundColor Cyan
Write-Host "  ====================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "  Verdict:  $verdictTitle" -ForegroundColor $(if ($npuFound) { "Green" } else { "Yellow" })
Write-Host "  CPU:      $cpuName" -ForegroundColor White
Write-Host "  NPU:      $(if ($npuFound) { "$($allNpuDevices.Count) device(s) found" } else { "Not detected" })" -ForegroundColor $(if ($npuFound) { "Green" } else { "Red" })
Write-Host "  OS:       $osName (Build $osBuild)" -ForegroundColor White
Write-Host ""
Write-Host "  Report saved to:" -ForegroundColor Gray
Write-Host "  $reportPath" -ForegroundColor Yellow
Write-Host ""

# Open in browser
Start-Process $reportPath
