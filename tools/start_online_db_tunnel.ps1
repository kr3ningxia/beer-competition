param(
    [int]$LocalPort = 13306,
    [string]$SshKeyPath = 'C:\Users\a1508\.ssh\codex-ecs-121.40.25.17'
)

$ErrorActionPreference = 'Stop'
$sshTarget = 'root@121.40.25.17'

if (-not (Test-Path -LiteralPath $SshKeyPath -PathType Leaf)) {
    throw "SSH identity file not found: $SshKeyPath"
}

Write-Host "Opening MySQL tunnel: 127.0.0.1:$LocalPort -> $sshTarget`:127.0.0.1:3306"
Write-Host 'Keep this window open while the local API is running. Press Ctrl+C to stop.'

& ssh -N -T `
    -o ExitOnForwardFailure=yes `
    -o ServerAliveInterval=60 `
    -o ServerAliveCountMax=3 `
    -i $SshKeyPath `
    -p 22 `
    -L "${LocalPort}:127.0.0.1:3306" `
    $sshTarget

if ($LASTEXITCODE -ne 0) {
    throw "SSH tunnel exited with code $LASTEXITCODE"
}
