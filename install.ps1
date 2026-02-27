# Sit CLI Installer
# Run with: irm https://raw.githubusercontent.com/abhishek0112cs221008/SIT/main/install.ps1 | iex

$ErrorActionPreference = "Stop"

$repo    = "abhishek0112cs221008/SIT"
$installDir = "$env:USERPROFILE\sit"
$apiUrl  = "https://api.github.com/repos/$repo/releases/latest"

Write-Host ""
Write-Host "  Sit CLI Installer" -ForegroundColor Cyan
Write-Host "  -----------------" -ForegroundColor DarkGray
Write-Host ""

# 1. Get latest release info
Write-Host "  Fetching latest release..." -ForegroundColor Gray
$release = Invoke-RestMethod -Uri $apiUrl -Headers @{ "User-Agent" = "sit-installer" }
$version = $release.tag_name
Write-Host "  Found: $version" -ForegroundColor Green

# 2. Get download URLs
$jarAsset = $release.assets | Where-Object { $_.name -eq "sit.jar" }
$batAsset = $release.assets | Where-Object { $_.name -eq "sit.bat" }

if (-not $jarAsset -or -not $batAsset) {
    Write-Host "  ERROR: Could not find sit.jar or sit.bat in release assets." -ForegroundColor Red
    exit 1
}

# 3. Create install directory
if (-not (Test-Path $installDir)) {
    New-Item -ItemType Directory -Path $installDir | Out-Null
}
Write-Host "  Installing to: $installDir" -ForegroundColor Gray

# 4. Download files
Write-Host "  Downloading sit.jar..." -ForegroundColor Gray
Invoke-WebRequest -Uri $jarAsset.browser_download_url -OutFile "$installDir\sit.jar"

Write-Host "  Downloading sit.bat..." -ForegroundColor Gray
Invoke-WebRequest -Uri $batAsset.browser_download_url -OutFile "$installDir\sit.bat"

# 5. Add to user PATH if not already there
$currentPath = [Environment]::GetEnvironmentVariable("Path", "User")
if ($currentPath -notlike "*$installDir*") {
    [Environment]::SetEnvironmentVariable("Path", "$currentPath;$installDir", "User")
    Write-Host "  Added to PATH." -ForegroundColor Gray
} else {
    Write-Host "  Already in PATH." -ForegroundColor Gray
}

Write-Host ""
Write-Host "  Sit CLI $version installed successfully!" -ForegroundColor Green
Write-Host "  Open a NEW terminal and run: sit version" -ForegroundColor Cyan
Write-Host ""
