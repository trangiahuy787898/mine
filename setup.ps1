Write-Host "=== Setup Spawner Finder Mod ===" -ForegroundColor Yellow

# Check Java
try {
    $javaVersion = java -version 2>&1 | ForEach-Object { $_.ToString() }
    if ($javaVersion -match 'version "(\d+)') {
        $ver = [int]$Matches[1]
        if ($ver -ge 21) {
            Write-Host "Java $ver detected!" -ForegroundColor Green
        } else {
            Write-Host "Java version too old: $ver. Need 21+." -ForegroundColor Red
            Write-Host "Download from: https://adoptium.net/temurin/releases/?version=21"
            exit 1
        }
    }
} catch {
    Write-Host "Java not found! Install JDK 21 from: https://adoptium.net/temurin/releases/?version=21" -ForegroundColor Red
    exit 1
}

# Download gradle wrapper jar if not present
$wrapperJar = "gradle\wrapper\gradle-wrapper.jar"
if (-not (Test-Path $wrapperJar)) {
    Write-Host "Downloading Gradle Wrapper..." -ForegroundColor Yellow
    $url = "https://raw.githubusercontent.com/FabricMC/fabric-example-mod/1.21.1/gradle/wrapper/gradle-wrapper.jar"
    try {
        Invoke-WebRequest -Uri $url -OutFile $wrapperJar -UseBasicParsing
        Write-Host "Downloaded gradle-wrapper.jar" -ForegroundColor Green
    } catch {
        Write-Host "Failed to download gradle-wrapper.jar. Trying alternative..." -ForegroundColor Yellow
        try {
            # Try direct from gradle services
            $url2 = "https://services.gradle.org/distributions/gradle-8.10-bin.zip"
            $zipPath = "$env:TEMP\gradle.zip"
            Invoke-WebRequest -Uri $url2 -OutFile $zipPath -UseBasicParsing
            Expand-Archive -Path $zipPath -DestinationPath "$env:TEMP\gradle" -Force
            & "$env:TEMP\gradle\gradle-8.10\bin\gradle.bat" wrapper --gradle-version 8.10
            Write-Host "Gradle wrapper generated" -ForegroundColor Green
        } catch {
            Write-Host "Failed to set up Gradle. Please install Gradle manually." -ForegroundColor Red
            exit 1
        }
    }
}

Write-Host "Setup complete! Run .\gradlew.bat build to build the mod." -ForegroundColor Green
Write-Host "Output: build/libs/spawnerfinder-1.0.0.jar" -ForegroundColor Cyan
