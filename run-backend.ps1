Write-Host "Starting Backend Service..." -ForegroundColor Green
Set-Location "$PSScriptRoot\member-management"
.\mvnw spring-boot:run
