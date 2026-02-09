$versions = @("1.20.1", "1.20.4", "1.21.1", "1.21.4")
$outputDir = "build\libs\all"

if (!(Test-Path $outputDir)) {
    New-Item -ItemType Directory -Path $outputDir -Force
}

foreach ($version in $versions) {
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "Building for Minecraft $version" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan
    
    Copy-Item "gradle-$version.properties" "gradle.properties" -Force
    
    .\gradlew.bat clean build --no-daemon
    
    if ($LASTEXITCODE -eq 0) {
        $jarFiles = Get-ChildItem "build\libs\*.jar" -Exclude "*-sources.jar", "*-dev.jar"
        foreach ($jar in $jarFiles) {
            Copy-Item $jar.FullName "$outputDir\" -Force
            Write-Host "Built: $($jar.Name)" -ForegroundColor Green
        }
    } else {
        Write-Host "Build failed for $version" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "All builds completed!" -ForegroundColor Green
Write-Host "JAR files are in: $outputDir" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
