param()

Write-Host "Starting import test..."
$excelFile = Get-ChildItem -Filter "*.xlsx" | Select-Object -First 1

if (-not $excelFile) {
    Write-Host "Excel not found"
    exit
}

$url = "http://localhost:8080/api/estudiantes/importar-excel?sedeId=1"
Write-Host "URL: $url"
Write-Host "File: $($excelFile.Name)"

$boundary = "----FormBoundary"
$fileBytes = [System.IO.File]::ReadAllBytes($excelFile.FullName)

$header = "--$boundary`r`nContent-Disposition: form-data; name=`"file`"; filename=`"$($excelFile.Name)`"`r`nContent-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`r`n`r`n"
$footer = "`r`n--$boundary--`r`n"

$headerBytes = [System.Text.Encoding]::UTF8.GetBytes($header)
$footerBytes = [System.Text.Encoding]::UTF8.GetBytes($footer)
$bodyBytes = $headerBytes + $fileBytes + $footerBytes

try {
    $response = Invoke-WebRequest -Uri $url -Method Post -Body $bodyBytes -ContentType "multipart/form-data; boundary=$boundary" -UseBasicParsing -TimeoutSec 120
    Write-Host "Status: $($response.StatusCode)"
    Write-Host "Response:"
    Write-Host $response.Content
} catch {
    Write-Host "Error: $_"
}
