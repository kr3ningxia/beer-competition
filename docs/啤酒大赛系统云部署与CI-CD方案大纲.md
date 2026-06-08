# 啤酒大赛系统云部署与 CI/CD 方案大纲

## 1. 部署目标

本方案按当前采购计划设计：先使用阿里云 ECS `2核2G` 低成本跑通系统，后续根据压测和比赛规模升配到 `2核4G`。

最终部署形态：

```text
阿里云 ECS Ubuntu 22.04/24.04
├─ Nginx                       # 宿主机安装，负责静态资源和反向代理
├─ MySQL 8.x                   # 宿主机安装，优先保证数据稳定
├─ Docker / Docker Compose
│  ├─ beer-api                 # Spring Boot 后端 API
│  └─ redis                    # 验证码、缓存
├─ /opt/beer-competition/app/.env
├─ /opt/beer-competition/www   # 前端 dist 静态文件
├─ /opt/beer-competition/uploads
├─ /opt/beer-competition/backups
└─ /opt/beer-competition/scripts
```

核心目标：

- 先用 99 元 `2核2G` ECS 跑通部署、域名、HTTPS 和主流程。
- MySQL 和 Nginx 放在宿主机，减少容器层复杂度和内存开销。
- Redis 和 Spring Boot API 使用 Docker Compose 管理，方便环境隔离、回滚和后续 CI/CD。
- 前端由 CI 或本地构建成 dist，上传到宿主机 Nginx 静态目录。
- 正式比赛前根据内存、接口响应、并发测试结果决定是否升到 `2核4G`。

## 2. 服务器规格与资源预算

当前推荐先购买：

| 配置项 | 推荐值 |
|---|---|
| 地域 | 华东1（杭州） |
| 实例 | `ecs.e-c1m1.large` |
| CPU / 内存 | `2 vCPU / 2 GiB` |
| 系统盘 | `ESSD Entry 40 GiB` |
| 公网带宽 | 固定带宽 `2-3 Mbps` |
| 操作系统 | Ubuntu 22.04 LTS 或 Ubuntu 24.04 LTS |
| 安全组 | 只开放 `22`、`80`、`443` |

2G 内存预算：

| 组件 | 预估占用 | 控制策略 |
|---|---:|---|
| Ubuntu 系统 | 350-600 MB | 关闭无关服务 |
| MySQL | 300-600 MB | 调低 buffer 和连接数 |
| Spring Boot API | 600-900 MB | JVM 限制 `-Xmx768m` |
| Redis | 50-150 MB | 设置 maxmemory |
| Nginx | 20-80 MB | 宿主机运行 |
| Docker daemon | 80-200 MB | 只跑必要容器 |

结论：

- 2核2G 可以用于内部测试、小规模比赛和低成本上线。
- 正式比赛如果评委超过 40 人，或后台要高频查询排名、导出、上传文件，建议提前升配。

## 3. 当前系统模块

```text
beer-competition
├─ beer-competition-api          # Spring Boot 3 + Java 17 + Maven
├─ beer-competition-console      # Vue 3 + Vite，后台端 + 厂商端
├─ beer-competition-judge-h5     # Vue 3 + Vite，评委移动端 H5
└─ docs
```

运行依赖：

| 组件 | 部署方式 |
|---|---|
| Ubuntu | 阿里云 ECS |
| MySQL | 宿主机安装 |
| Nginx | 宿主机安装 |
| Docker | 宿主机安装 |
| Docker Compose | 宿主机 Compose plugin |
| Spring Boot API | Docker 容器 |
| Redis | Docker 容器 |
| 前端 dist | 宿主机 Nginx 静态目录 |

## 4. 为什么不全用 Docker

原计划是除 MySQL 外全部 Docker。结合 2核2G 机器，调整为 Nginx 宿主机部署，API 和 Redis 使用 Docker。

| 项 | 全 Docker | 当前推荐方案 |
|---|---|---|
| Nginx | 容器运行 | 宿主机运行 |
| MySQL | 宿主机运行 | 宿主机运行 |
| Redis | 容器运行 | 容器运行 |
| Spring Boot API | 容器运行 | 容器运行 |
| 前端 | Nginx 镜像或挂载 | 宿主机静态目录 |

选择当前方案的原因：

- 2G 内存下，宿主机 Nginx 更轻，证书、静态目录、日志配置也更直观。
- API 容器化最有价值，能隔离 Java 环境，方便 CI/CD 和回滚。
- Redis 容器化成本低，配置简单，不对公网开放。
- MySQL 宿主机部署，备份、恢复、权限、磁盘路径更清楚。
- 后续迁移到 2核4G 或新服务器时，API/Redis 的容器配置可以直接复用。

性能差异：

- Docker 在 Linux 上 CPU 损耗很小。
- 主要差异来自额外内存占用、日志、镜像和容器管理开销。
- 在 2核2G 机器上，Nginx 宿主机部署更省资源；API 用 Docker 带来的维护收益大于额外开销。

## 5. 访问入口设计

无域名阶段：

```text
http://服务器IP/console    后台端 + 厂商端
http://服务器IP/judge      评委 H5
http://服务器IP/api        后端 API
```

有域名后建议：

```text
https://console.example.com      后台端 + 厂商端
https://judge.example.com        评委 H5
https://api.example.com          后端 API
```

也可以继续使用路径：

```text
https://example.com/console
https://example.com/judge
https://example.com/api
```

注意：

- 评委扫码、摄像头能力通常需要 HTTPS。
- 内部跑通可以先用 IP + HTTP。
- 正式验收评委端前应配置域名和 HTTPS。

## 6. 推荐目录规划

```text
/opt/beer-competition
├─ app
│  ├─ docker-compose.yml
│  └─ .env
├─ www
│  ├─ console
│  └─ judge
├─ nginx
│  ├─ beer-competition.conf
│  └─ ssl
├─ uploads
├─ backups
│  ├─ mysql
│  ├─ uploads
│  └─ releases
└─ scripts
   ├─ deploy-api.sh
   ├─ deploy-web.sh
   ├─ backup-mysql.sh
   ├─ rollback-api.sh
   ├─ rollback-web.sh
   └─ health-check.sh
```

建议属主：

```text
/opt/beer-competition/app       root 或 deploy
/opt/beer-competition/www       www-data 或 deploy
/opt/beer-competition/uploads   deploy
/opt/beer-competition/backups   root 或 deploy
```

## 7. MySQL 宿主机规划

### 7.1 安装方式

MySQL 安装在 Ubuntu 宿主机：

```text
Ubuntu
└─ MySQL 8.x
```

原因：

- 数据库文件路径清晰。
- 备份和恢复更直观。
- 低配机器上少一层容器管理成本。
- 后续真实使用可以迁移到阿里云 RDS。

### 7.2 数据库账号

不建议应用使用 root。

建议创建：

```text
数据库：beer_competition
用户：beer_app
权限：仅 beer_competition 库
```

示例：

```sql
CREATE DATABASE beer_competition CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER 'beer_app'@'172.17.%' IDENTIFIED BY '强密码';
GRANT ALL PRIVILEGES ON beer_competition.* TO 'beer_app'@'172.17.%';
FLUSH PRIVILEGES;
```

如果 Docker 网段不是 `172.17.%`，以实际网段为准。初期可以临时使用 `%`，部署稳定后收窄来源。

### 7.3 2G 机器 MySQL 参数

建议在 `/etc/mysql/mysql.conf.d/mysqld.cnf` 中控制内存：

```ini
[mysqld]
bind-address = 0.0.0.0
max_connections = 80
innodb_buffer_pool_size = 256M
innodb_log_file_size = 64M
table_open_cache = 400
tmp_table_size = 32M
max_heap_table_size = 32M
```

说明：

- `bind-address = 0.0.0.0` 只代表 MySQL 监听网卡，不等于开放公网。
- ECS 安全组必须禁止公网访问 `3306`。
- Ubuntu 本机防火墙也应限制 `3306` 只允许本机和 Docker 网段访问。

### 7.4 中文数据导入

导入中文 SQL 时：

- SQL 文件使用 UTF-8 编码。
- 命令指定 `--default-character-set=utf8mb4`。
- 导入后抽查中文字段 `HEX()`。
- 如果结果出现大量 `3F`，说明中文已经被写成问号，需要立刻修正。

示例：

```bash
mysql --default-character-set=utf8mb4 -u beer_app -p beer_competition < init.sql
```

## 8. Docker Compose 设计

Compose 只管理 API 和 Redis：

```text
docker-compose.yml
├─ beer-api
│  ├─ Spring Boot jar
│  ├─ 读取 .env
│  ├─ 连接 redis:6379
│  └─ 连接宿主机 MySQL
└─ redis
   ├─ 仅 Compose 网络内部访问
   └─ 可选 volume 持久化
```

网络关系：

```text
公网用户
  -> ECS:80/443
  -> 宿主机 Nginx
      -> /console 静态文件
      -> /judge 静态文件
      -> /api 反向代理 127.0.0.1:8080

beer-api 容器
  -> redis 容器：redis:6379
  -> 宿主机 MySQL：host.docker.internal:3306
```

端口暴露原则：

```text
公网开放：22、80、443
本机开放：8080
不开放公网：3306、6379
```

Compose 示例：

```yaml
services:
  beer-api:
    image: beer-competition-api:latest
    container_name: beer-api
    restart: unless-stopped
    env_file:
      - .env
    environment:
      JAVA_TOOL_OPTIONS: "-Xms256m -Xmx768m -XX:+UseG1GC"
    ports:
      - "127.0.0.1:8080:8080"
    volumes:
      - /opt/beer-competition/uploads:/opt/beer-competition/uploads
      - /opt/beer-competition/logs/api:/logs
    extra_hosts:
      - "host.docker.internal:host-gateway"
    depends_on:
      - redis
    mem_limit: 900m

  redis:
    image: redis:7-alpine
    container_name: beer-redis
    restart: unless-stopped
    command: redis-server --maxmemory 128mb --maxmemory-policy allkeys-lru
    volumes:
      - redis-data:/data
    mem_limit: 180m

volumes:
  redis-data:
```

说明：

- `beer-api` 只绑定 `127.0.0.1:8080`，不直接暴露公网。
- Redis 不配置 `ports`，只允许 Compose 网络内访问。
- 2G 机器必须限制 JVM 和 Redis 内存。

## 9. API 容器设计

### 9.1 Dockerfile 思路

推荐多阶段构建：

```text
第一阶段：Maven + JDK 17
- 复制 pom.xml
- 下载依赖
- 复制 src
- mvn package

第二阶段：JRE 17
- 复制 jar
- 暴露 8080
- java -jar app.jar
```

后续接 CI/CD 后，推荐在 GitHub Actions 或阿里云流水线中构建镜像，服务器只负责拉取和启动。

### 9.2 运行要求

API 容器需要：

- 读取 `/opt/beer-competition/app/.env`。
- 挂载 `/opt/beer-competition/uploads`。
- 通过 `host.docker.internal` 连接宿主机 MySQL。
- 通过 `redis` 服务名连接 Redis 容器。
- JVM 限制内存，避免挤爆 2G 机器。

## 10. Redis 容器设计

Redis 用途：

- 短信验证码。
- 登录态或短期缓存。
- 后续可用于限流、排行榜缓存。

建议：

- 初期不暴露公网端口。
- 初期可以不设置密码；准生产建议设置密码。
- 数据不是核心持久化数据，优先控制内存。

推荐配置：

```yaml
redis:
  image: redis:7-alpine
  command: redis-server --maxmemory 128mb --maxmemory-policy allkeys-lru
  volumes:
    - redis-data:/data
```

## 11. Nginx 宿主机设计

### 11.1 Nginx 职责

宿主机 Nginx 负责：

- 监听公网 `80/443`。
- 托管后台端和厂商端 dist。
- 托管评委 H5 dist。
- 反向代理 `/api` 到本机 `127.0.0.1:8080`。
- 配置 HTTPS 证书。
- 控制上传大小。
- 支持 Vue Router history 模式。

### 11.2 静态目录

```text
/opt/beer-competition/www
├─ console
│  └─ index.html
└─ judge
   └─ index.html
```

前端 Vite 构建需要配合路径：

```env
VITE_API_BASE_URL=/api
```

如果使用路径部署：

```text
console base: /console/
judge base: /judge/
```

如果使用独立域名：

```text
console base: /
judge base: /
```

### 11.3 Nginx 配置示意

无域名阶段：

```nginx
server {
    listen 80;
    server_name _;

    client_max_body_size 200m;

    location /api/ {
        proxy_pass http://127.0.0.1:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /console/ {
        alias /opt/beer-competition/www/console/;
        try_files $uri $uri/ /console/index.html;
    }

    location /judge/ {
        alias /opt/beer-competition/www/judge/;
        try_files $uri $uri/ /judge/index.html;
    }

    location = / {
        return 302 /console/;
    }
}
```

启用配置：

```bash
sudo ln -s /opt/beer-competition/nginx/beer-competition.conf /etc/nginx/sites-enabled/beer-competition.conf
sudo nginx -t
sudo systemctl reload nginx
```

## 12. 环境变量设计

服务器 `.env` 放在：

```text
/opt/beer-competition/app/.env
```

示例：

```env
SPRING_PROFILES_ACTIVE=dev

APP_DB_HOST=host.docker.internal
APP_DB_PORT=3306
APP_DB_NAME=beer_competition
APP_DB_USERNAME=beer_app
APP_DB_PASSWORD=强密码

APP_REDIS_HOST=redis
APP_REDIS_PORT=6379
APP_REDIS_PASSWORD=
APP_REDIS_DATABASE=0

APP_JWT_SECRET=生产随机长密钥
APP_JWT_ADMIN_TTL=7200000
APP_JWT_PORTAL_TTL=7200000
APP_JWT_JUDGE_TTL=7200000

APP_SMS_MOCK_ENABLED=true

APP_PII_AES_KEY=生产随机长密钥
APP_PII_HASH_KEY=生产随机长密钥

APP_STORAGE_PROVIDER=local
APP_STORAGE_LOCAL_DIR=/opt/beer-competition/uploads
```

要求：

- `.env` 不提交 Git。
- JWT、PII、数据库密码必须替换为强随机值。
- GitHub Actions 使用 Secrets 保存 SSH 私钥、镜像仓库账号、服务器地址。
- 真实短信上线前再关闭 mock。

## 13. 初始部署实施顺序

### 13.1 阿里云购买配置

- 包年包月，1年。
- Ubuntu 22.04 LTS 或 Ubuntu 24.04 LTS。
- 2核2G，40G 系统盘。
- 固定带宽 2-3Mbps。
- 安全组开放 `22`、`80`、`443`。
- 不开放 `3306`、`6379`、`8080`。
- 登录使用密钥对。
- 扩展程序可以只选 Docker；Java、Node、Python 不需要在购买页预装。

### 13.2 系统初始化

- 创建普通部署用户。
- 配置 SSH 密钥登录。
- 更新系统软件包。
- 安装 Nginx。
- 安装 MySQL。
- 安装 Docker 和 Docker Compose plugin。
- 创建 `/opt/beer-competition` 目录。
- 创建 2G swap。

2G swap 建议：

```bash
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
```

### 13.3 MySQL 初始化

- 创建数据库。
- 创建应用账号。
- 导入初始化 SQL。
- 检查字符集。
- 抽查中文字段。
- 确认 API 容器可以连接宿主机 MySQL。

### 13.4 应用部署

- 构建 API 镜像。
- 编写 docker-compose.yml。
- 编写 `.env`。
- 启动 Redis 和 API。
- 构建 console 和 judge 前端。
- 上传 dist 到 `/opt/beer-competition/www`。
- 配置 Nginx。
- 验证前端、API、数据库、Redis。

首次启动命令：

```bash
cd /opt/beer-competition/app
docker compose up -d
docker compose ps
docker compose logs -f beer-api
sudo nginx -t
sudo systemctl reload nginx
```

验证入口：

```text
http://服务器IP/console
http://服务器IP/judge
http://服务器IP/api/...
```

## 14. 前端发布方式

前端构建结果：

```text
beer-competition-console/dist   -> /opt/beer-competition/www/console
beer-competition-judge-h5/dist  -> /opt/beer-competition/www/judge
```

初期可以本地构建后上传：

```bash
npm ci
npm run build
```

服务器端发布脚本大纲：

```bash
#!/usr/bin/env bash
set -e

RELEASE_DIR=/opt/beer-competition/backups/releases/$(date +%Y%m%d%H%M%S)
mkdir -p "$RELEASE_DIR"

cp -a /opt/beer-competition/www/console "$RELEASE_DIR/console"
cp -a /opt/beer-competition/www/judge "$RELEASE_DIR/judge"

rm -rf /opt/beer-competition/www/console/*
rm -rf /opt/beer-competition/www/judge/*

cp -a ./dist-console/* /opt/beer-competition/www/console/
cp -a ./dist-judge/* /opt/beer-competition/www/judge/

nginx -t
systemctl reload nginx
```

注意：

- 低配服务器不建议在运行服务时执行大量 npm build。
- 前端最好在本地或 CI 构建，再上传 dist。

## 15. 手动部署脚本

建议保留两个部署脚本：

```text
/opt/beer-competition/scripts/deploy-api.sh
/opt/beer-competition/scripts/deploy-web.sh
```

API 发布职责：

- 备份当前镜像 tag 或 compose 文件。
- 拉取新镜像或本地 build。
- 执行 `docker compose up -d beer-api`。
- 查看健康检查。
- 输出关键日志。

Web 发布职责：

- 备份当前 dist。
- 替换 console 和 judge 静态目录。
- 执行 `nginx -t`。
- reload Nginx。

初期服务器 build 版：

```bash
cd /opt/beer-competition/source
git pull
docker build -t beer-competition-api:latest -f beer-competition-api/Dockerfile .
cd /opt/beer-competition/app
docker compose up -d beer-api
```

后续镜像仓库版：

```bash
cd /opt/beer-competition/app
docker compose pull beer-api
docker compose up -d beer-api
docker image prune -f
```

## 16. CI/CD 设计

### 16.1 推荐策略

当前项目仍在频繁修改，建议：

```text
push 自动构建检查
手动触发部署测试环境
```

暂不建议每次 push 自动覆盖服务器，避免半成品影响演示和验收。

### 16.2 CI 工作流

每次 push 或 pull request：

```text
后端：
- setup Java 17
- mvn test
- mvn package

console：
- setup Node 20/22
- npm ci
- npm run build

judge H5：
- setup Node 20/22
- npm ci
- npm run build
```

### 16.3 CD 工作流

手动触发：

```text
GitHub Actions workflow_dispatch
-> 构建 API 镜像
-> 推送到 GHCR 或阿里云 ACR
-> 构建两个前端 dist
-> SSH 上传 dist 到 ECS
-> SSH 到 ECS 执行 docker compose pull/up
-> nginx -t && systemctl reload nginx
-> health check
```

镜像仓库选择：

| 镜像仓库 | 说明 |
|---|---|
| GitHub Container Registry | 配合 GitHub Actions 简单 |
| 阿里云 ACR | 国内 ECS 拉取速度更稳定 |
| 不用镜像仓库 | 初期服务器直接 build，最简单 |

推荐路线：

```text
第 1 阶段：本地/服务器手动部署
第 2 阶段：GitHub Actions 构建前端 dist，SSH 上传
第 3 阶段：GitHub Actions 构建 API 镜像，ECS 拉取
第 4 阶段：引入镜像版本、回滚和自动备份
```

### 16.4 GitHub Secrets

需要配置：

```text
ECS_HOST
ECS_PORT
ECS_USER
ECS_SSH_KEY
ACR_USERNAME
ACR_PASSWORD
ACR_REGISTRY
```

如果使用 GHCR，则使用 GitHub token 或 PAT。

## 17. 回滚策略

### 17.1 API 回滚

镜像 tag 建议：

```text
latest
staging
commit-sha
v1.0.0
```

回滚方式：

- 修改 compose 中 API 镜像 tag 为上一个 commit-sha。
- `docker compose pull beer-api`。
- `docker compose up -d beer-api`。
- 检查健康接口和日志。

### 17.2 前端回滚

每次发布前备份：

```text
/opt/beer-competition/backups/releases/yyyyMMddHHmmss
```

回滚方式：

- 恢复上一个 console 和 judge dist。
- `nginx -t`。
- `systemctl reload nginx`。

## 18. 备份策略

### 18.1 MySQL 备份

备份目录：

```text
/opt/beer-competition/backups/mysql
```

建议：

- 每次部署前备份。
- 每天定时备份。
- 保留最近 7 到 14 天。
- 定期做恢复演练。

脚本方向：

```bash
mysqldump --default-character-set=utf8mb4 \
  -u beer_app -p beer_competition \
  > /opt/beer-competition/backups/mysql/beer_competition_$(date +%Y%m%d%H%M%S).sql
```

### 18.2 上传目录备份

MVP 阶段本地上传目录：

```text
/opt/beer-competition/uploads
```

建议：

- 定期打包备份。
- 正式使用前评估 OSS。
- 比赛前做一次完整快照。

### 18.3 ECS 快照

关键节点建议手动快照：

- 系统环境初始化完成后。
- 首次部署成功后。
- 正式比赛前。
- 重大数据库结构调整前。

## 19. 日志与排查

常用命令：

```bash
docker compose ps
docker compose logs -f beer-api
docker compose logs -f redis
docker compose restart beer-api
sudo tail -f /var/log/nginx/access.log
sudo tail -f /var/log/nginx/error.log
sudo systemctl status mysql
free -h
df -h
docker stats
```

常见问题：

| 问题 | 排查方向 |
|---|---|
| 前端空白 | Vite base、Nginx alias、静态资源路径 |
| 刷新 404 | Nginx try_files、Vue Router history |
| API 502 | beer-api 未启动、8080 是否监听本机 |
| API 连不上 MySQL | host.docker.internal、MySQL bind-address、用户授权 |
| API 连不上 Redis | APP_REDIS_HOST 是否为 redis |
| 登录失败 | Redis、mock 验证码、JWT secret |
| 上传失败 | 上传目录权限、Nginx client_max_body_size |
| 中文乱码 | SQL 文件编码、导入字符集、HEX 抽查 |
| 手机扫码失败 | 是否使用 HTTPS |
| 页面偶发慢 | free、docker stats、MySQL 慢查询、JVM GC |

## 20. 域名与 HTTPS

无域名阶段：

```text
http://服务器IP/console
http://服务器IP/judge
http://服务器IP/api
```

正式测试和上线前：

```text
https://console.example.com
https://judge.example.com
https://api.example.com
```

HTTPS 方案：

- 宿主机 Nginx + Certbot。
- 或使用阿里云免费证书，手动放到 Nginx。
- 评委扫码验收前必须解决 HTTPS。

Certbot 方向：

```bash
sudo apt install certbot python3-certbot-nginx
sudo certbot --nginx -d example.com
```

## 21. OSS 与真实短信演进

### 21.1 OSS

MVP：

```env
APP_STORAGE_PROVIDER=local
```

真实使用前：

```env
APP_STORAGE_PROVIDER=oss
APP_ALIOSS_BUCKET_NAME=...
APP_ALIOSS_ENDPOINT=...
APP_ALIOSS_ACCESS_KEY_ID=...
APP_ALIOSS_ACCESS_KEY_SECRET=...
```

### 21.2 短信

MVP：

```env
APP_SMS_MOCK_ENABLED=true
```

真实使用前：

```env
APP_SMS_MOCK_ENABLED=false
APP_ALIYUN_ACCESS_KEY_ID=...
APP_ALIYUN_ACCESS_KEY_SECRET=...
APP_SMS_SIGN_NAME=...
APP_SMS_TEMPLATE_CODE=...
```

需要提前完成阿里云短信签名和模板审核。

## 22. 数据库迁移管理

MVP 初期：

- 手动执行 SQL。
- 每次变更保留 SQL 文件。
- 执行前备份。

后续建议引入 Flyway：

```text
src/main/resources/db/migration
├─ V1__init.sql
├─ V2__awards.sql
├─ V3__score_record_unique.sql
└─ V4__award_certificate.sql
```

价值：

- 数据库结构版本可追踪。
- 多环境一致。
- 部署时自动应用迁移。
- 降低手动漏执行 SQL 的风险。

## 23. 安全配置

必须关注：

- `.env` 不提交 Git。
- MySQL 不开放公网。
- Redis 不开放公网。
- Spring Boot `8080` 只绑定本机。
- SSH 使用密钥登录。
- ECS 安全组只开放必要端口。
- 应用数据库用户不使用 root。
- JWT 和 PII 密钥使用强随机值。
- CI/CD 私钥只放 GitHub Secrets。
- 部署完成后关闭密码登录或限制 SSH 来源 IP。

安全组建议：

```text
22    SSH，限制个人 IP
80    HTTP
443   HTTPS
```

不要开放：

```text
3306  MySQL
6379  Redis
8080  Spring Boot
3389  Windows RDP
```

## 24. 性能验证与升配标准

2核2G 上线前至少观察：

```bash
free -h
docker stats
top
vmstat 1
df -h
```

建议内部压测场景：

- 20 个评委同时登录。
- 20-40 个评委同时打开评分页。
- 连续提交评分。
- 后台同时查看报名列表、评委列表、轮次进度、成绩排名。
- 上传小文件或图片。

建议升配到 2核4G 的信号：

- 内存长期超过 80%。
- swap 持续增长。
- API 提交评分经常超过 2 秒。
- 后台列表查询明显卡顿。
- Java 容器频繁重启或出现 OOM。
- 比赛当天评委规模超过 40 人。
- 同一台服务器还要部署啤酒地图小程序。

升配策略：

- 内部跑通阶段使用 2核2G。
- 比赛前至少提前 3-5 天做压力测试。
- 不稳定就升 2核4G，升配后重新验证。
- 不把升配留到比赛当天。

## 25. 推荐查资料关键词

```text
Ubuntu install Docker Engine
Docker Compose plugin Ubuntu
Dockerfile Spring Boot Maven multi stage
Docker Compose Spring Boot Redis
Docker host.docker.internal Linux host-gateway
Nginx Vue history mode
Nginx reverse proxy 127.0.0.1
Vite base path deploy
GitHub Actions Docker build push
GitHub Actions SSH deploy
GitHub Container Registry
Aliyun Container Registry ACR
MySQL bind-address Docker host
MySQL create user grant privileges
MySQL utf8mb4 import
Certbot Nginx HTTPS
Flyway Spring Boot MySQL
Aliyun OSS Spring Boot
Aliyun ECS upgrade instance type
```

## 26. 面试表达思路

可以这样组织：

```text
这个项目是一个啤酒赛事管理系统，包含后台端、厂商端、评委 H5 和 Spring Boot 后端。

部署上我没有把所有组件都容器化，而是结合低配 ECS 的资源限制做了分层：
MySQL 和 Nginx 部署在宿主机，API 和 Redis 使用 Docker Compose 管理。

MySQL 放宿主机是为了让数据备份、恢复、权限和磁盘路径更直观。
Nginx 放宿主机是为了降低 2G 机器的运行开销，并方便配置 HTTPS 和静态资源目录。
Spring Boot API 容器化是为了隔离 Java 运行环境，方便后续镜像发布和回滚。
Redis 容器化成本低，且只服务于 API 的验证码和缓存。

CI/CD 方面，早期采用手动触发发布。
GitHub Actions 负责后端构建镜像、前端构建 dist，再通过 SSH 部署到 ECS。
API 通过 docker compose 更新，前端通过替换 Nginx 静态目录发布。

由于项目早期修改频繁，没有直接做 push 自动发布，避免测试环境被半成品覆盖。
正式上线前会补齐 HTTPS、备份、数据库迁移、OSS、日志监控和压测。
```

## 27. 当前推荐结论

当前可以按下面路线执行：

```text
阿里云 ECS 2核2G Ubuntu
-> 宿主机安装 MySQL
-> 宿主机安装 Nginx
-> 安装 Docker 和 Docker Compose
-> Docker Compose 管理 API、Redis
-> Nginx 用 /console、/judge、/api 暴露服务
-> API 通过 host.docker.internal 连接宿主机 MySQL
-> 前端 dist 上传到 /opt/beer-competition/www
-> 先手动部署跑通
-> 再接 GitHub Actions 手动触发发布
-> 比赛前压测，不稳就升 2核4G
```

这条路线兼顾低成本、可维护和后续升级空间。2核2G 先用于内部跑通是合理的，正式比赛前是否升配由压测和监控数据决定。
