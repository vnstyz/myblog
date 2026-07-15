#!/usr/bin/env bash
# 在 VPS（Linux）上执行的一键部署脚本
# 前置：已安装 Docker Engine 与 docker compose 插件、已存在本项目文件
set -euo pipefail
cd "$(dirname "$0")"

echo "==> 检查 Docker"
if ! command -v docker >/dev/null 2>&1; then
  echo "错误：未检测到 docker，请先安装 Docker Engine 与 docker compose 插件"
  echo "参考：https://docs.docker.com/engine/install/"
  exit 1
fi
if ! docker compose version >/dev/null 2>&1; then
  echo "错误：docker compose 插件不可用，请安装 compose v2"
  exit 1
fi

echo "==> 准备 .env"
if [ ! -f .env ]; then
  if [ ! -f .env.example ]; then
    echo "错误：缺少 .env.example，无法生成 .env"
    exit 1
  fi
  cp .env.example .env
  echo "已生成 .env（默认弱密码）。请先编辑 .env 设置 MYSQL_ROOT_PASSWORD / ADMIN_PASSWORD，"
  echo "修改完成后再重新执行： bash deploy.sh"
  exit 0
fi

open_firewall() {
  local sudoprefix=""
  command -v sudo >/dev/null 2>&1 && sudoprefix="sudo"
  if command -v ufw >/dev/null 2>&1; then
    echo "==> 使用 ufw 放行 8080/tcp"
    $sudoprefix ufw allow 8080/tcp
  elif command -v firewall-cmd >/dev/null 2>&1; then
    echo "==> 使用 firewalld 放行 8080/tcp"
    $sudoprefix firewall-cmd --permanent --add-port=8080/tcp
    $sudoprefix firewall-cmd --reload
  else
    echo "警告：未检测到 ufw / firewalld，请手动放行 8080 端口（如云厂商安全组也需放行）"
  fi
}

echo "==> 配置防火墙"
open_firewall

echo "==> 构建并启动（docker compose up -d --build）"
docker compose up -d --build

echo ""
echo "部署完成！访问地址： http://<本机公网IP>:8080"
echo "查看日志： docker compose logs -f app"
echo "停止：     docker compose down"
echo "（如未设置 ADMIN_PASSWORD，首次启动日志中会打印自动生成的管理员密码）"
