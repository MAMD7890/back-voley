# Test script for Excel import
$excelFile = Get-ChildItem -Filter "*.xlsx" | Select-Object -First 1
if (-not $excelFile) {
    Write-Host "Excel file not found"
    exit 1
}

$url = "http://localhost:8080/api/estudiantes/importar-excel?sedeId=1"
$filePath = $excelFile.FullName

Write-Host "Testing Excel import..."
Write-Host "File: $($excelFile.Name)"
Write-Host "URL: $url"
Write-Host "Time: $(Get-Date)"

try {
    # Create multipart form data
    $boundary = "----WebKitFormBoundary$(Get-Random -Minimum 1000000000 -Maximum 9999999999)"
    
    # Read file
    $fileBytes = [System.IO.File]::ReadAllBytes($filePath)
    
    # Build body
    $bodyLines = @()
    $bodyLines += "--$boundary"
    $bodyLines += 'Content-Disposition: form-data; name="file"; filename="' + $excelFile.Name + '"'
    $bodyLines += 'Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    $bodyLines += ""
    
    # Combine header, file bytes, and footer
    $header = [System.Text.Encoding]::UTF8.GetBytes(($bodyLines -join "`r`n") + "`r`n")
    $footer = [System.Text.Encoding]::UTF8.GetBytes("`r`n--$boundary--")
    $bodyBytes = $header + $fileBytes + $footer
    
    # Make request
    $response = Invoke-WebRequest -Uri $url -Method Post -Body $bodyBytes `
        -ContentType "multipart/form-data; boundary=$boundary" `
        -UseBasicParsing -TimeoutSec 120
    
    Write-Host "Response Status: $($response.StatusCode)"
    Write-Host "Response Content:"
    $content = $response.Content | ConvertFrom-Json
    $content | ConvertTo-Json -Depth 10
    
    # Extract success count
    if ($content.excelImportResponseDTO -and $content.excelImportResponseDTO.totalExitosos) {
        Write-Host "`n✓ Successful imports: $($content.excelImportResponseDTO.totalExitosos)"
    }
    
} catch {
    Write-Host "Error: $($_.Exception.Message)"
}
