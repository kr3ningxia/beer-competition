$ErrorActionPreference = "Stop"
$docPath = "D:\project\beer-competition\tmp\manual.docx"
$pdfPath = "D:\project\beer-competition\tmp\manual.pdf"

$word = New-Object -ComObject Word.Application
$word.Visible = $false
$word.DisplayAlerts = 0
$doc = $word.Documents.Open($docPath)
$doc.Fields.Update() | Out-Null
foreach ($toc in $doc.TablesOfContents) { $toc.Update() }
$doc.Save()
$doc.SaveAs([ref]$pdfPath, [ref]17)
$doc.Close([ref]0)
$word.Quit()
[System.Runtime.Interopservices.Marshal]::ReleaseComObject($doc) | Out-Null
[System.Runtime.Interopservices.Marshal]::ReleaseComObject($word) | Out-Null
Write-Output "Updated and exported: $pdfPath"
