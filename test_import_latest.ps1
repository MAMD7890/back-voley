Write-Host "Initiating Excel import test..."
$excelFile = Get-ChildItem -Filter "*.xlsx" | Select-Object -First 1
Write-Host "Excel file: $($excelFile.Name)"

if (-not $excelFile) {
    Write-Host "Excel file not found"
    exit 1
}

$url = "http://localhost:8080/api/estudiantes/importar-excel?sedeId=1"
$filePath = $excelFile.FullName

try {
    $fileBytes = [System.IO.File]::ReadAllBytes($filePath)
    
    # Create multipart form data
    $boundary = "----WebKitFormBoundary7$(Get-Random)"
    $header = "--$boundary`r`nContent-Disposition: form-data; name=`"file`"; filename=`"$($excelFile.Name)`"`r`nContent-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`r`n`r`n"
    $footer = "`r`n--$boundary--`r`n"
    
    $headerBytes = [System.Text.Encoding]::UTF8.GetBytes($header)
    $footerBytes = [System.Text.Encoding]::UTF8.GetBytes($footer)
    $bodyBytes = $headerBytes + $fileBytes + $footerBytes
    
    Write-Host "Sending request to: $url"
    Write-Host "Payload size: $($bodyBytes.Length) bytes"
    
    $response = Invoke-WebRequest -Uri $url -Method Post -Body $bodyBytes `
        -ContentType "multipart/form-data; boundary=$boundary" `
        -UseBasicParsing -TimeoutSec 120
    
    Write-Host "Response Status: $($response.StatusCode)"
    Write-Host "Response saved to import_result_latest.json"
    $response.Content | Out-File "import_result_latest.json"
    
} catch {
    Write-Host "Error: $($_.Exception.Message)"
    Write-Host "Error saved to import_error_latest.txt"
    $_.Exception | Out-File "import_error_latest.txt"
}
