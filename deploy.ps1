# 在 Windows 客户端执行，把项目打包传到 VPS 并触发部署
# 前置：系统已启用 OpenSSH 客户端（Win10+ 默认自带 ssh/scp/tar）
#
# 用法：
#   .\deploy.ps1 -Host 1.2.3.4 -User root
#   .\deploy.ps1 -Host 1.2.3.4 -User root -Identity C:\Users\you\.ssh\id_rsa -RemoteDir /opt/myblog
param(
  [Parameter(Mandatory = $true)]  [string]$Host,
  [Parameter(Mandatory = $true)]  [string]$User,
  [string]$Identity = "",
  [string]$RemoteDir = "/opt/myblog"
)

$ErrorActionPreference = "Stop"
$local = Resolve-Path $PSScriptRoot

$sshBase = @()
if ($Identity) { $sshBase += "-i"; $sshBase += $Identity }
$scpBase = @()
if ($Identity) { $scpBase += "-i"; $scpBase += $Identity }

Write-Host "==> 创建远端目录 $RemoteDir"
ssh @sshBase "${User}@${Host}" "mkdir -p $RemoteDir"

Write-Host "==> 打包并上传项目（排除 target/.git/.idea）"
# 使用 tar 流式传输，避免 scp 通配符兼容问题，也更省流量
tar -czf - -C "$local" --exclude target --exclude .git --exclude .idea --exclude node_modules . |
  ssh @sshBase "${User}@${Host}" "tar -xzf - -C $RemoteDir"

Write-Host "==> 在 VPS 上执行部署"
ssh @sshBase "${User}@${Host}" "cd $RemoteDir && bash deploy.sh"
