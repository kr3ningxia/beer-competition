$ErrorActionPreference = 'Stop'

$repo = 'D:\project\beer-competition'
$rg = (Get-Command rg -ErrorAction Stop).Source
$searchTerms = @(
    '付款账户名|转账时间|转账备注|付款凭证|提交转账信息'
    'paymentProof|payment.*proof|transferTime|payerName|paymentRemark|transferRemark'
)

foreach ($term in $searchTerms) {
    Write-Output "`n---SEARCH: $term---"
    $nativeArgs = @('-n', '--hidden', '--glob', '!**/node_modules/**', '--glob', '!**/target/**', '--glob', '!**/dist/**', $term, $repo)
    & $rg @nativeArgs
    $exitCode = $LASTEXITCODE
    if ($exitCode -gt 1) {
        throw "rg failed with exit code $exitCode"
    }
}

Write-Output "`n---ROOT FILES---"
Get-ChildItem -LiteralPath $repo -Force | Select-Object Name, Mode
