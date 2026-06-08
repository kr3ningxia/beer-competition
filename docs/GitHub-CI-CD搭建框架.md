# 啤酒大赛系统 GitHub CI/CD 搭建框架

## 1. 目标效果

目标是把当前手动部署流程，逐步变成可重复、可追踪、可回滚的发布流程。

最终效果：

- 代码推送后，GitHub 自动检查项目能否构建。
- 后端自动执行 Maven 构建。
- 前端自动执行 npm 构建。
- 需要部署时，手动触发 GitHub Actions。
- GitHub 自动打包：
  - `beer-api.jar`
  - `console dist`
  - `judge dist`
- GitHub 自动上传发布包到阿里云 ECS。
- ECS 自动执行部署脚本。
- Nginx 自动 reload。
- Spring Boot API 自动重启。
- 部署失败时可以查看日志排查。
- 后续可以按版本回滚。

## 2. 当前部署架构

服务器当前结构：

```text
阿里云 ECS 2核2G Ubuntu
├─ MySQL：宿主机安装
├─ Nginx：宿主机安装
├─ Docker / Docker Compose
│  ├─ beer-api：Spring Boot API
│  └─ redis：验证码、缓存
├─ /opt/beer-competition/app
│  ├─ beer-api.jar
│  ├─ Dockerfile
│  ├─ docker-compose.yml
│  └─ .env
├─ /opt/beer-competition/www
│  ├─ console
│  └─ judge
├─ /opt/beer-competition/backups
└─ /opt/beer-competition/scripts
```

访问入口：

```text
http://服务器IP/console/
http://服务器IP/judge/
http://服务器IP/api/
```

## 3. 为什么不用服务器构建

当前服务器是 `2核2G`，适合运行服务，不适合长期承担构建任务。

Maven 和 npm 构建会消耗较多 CPU、内存和磁盘 IO。推荐：

```text
GitHub Actions 负责构建
ECS 只负责运行
```

这样服务器更稳定，部署也更快。

## 4. 仓库同步方式

当前代码在 Gitee，可以同步到 GitHub。

推荐方式：

```text
Gitee：可以继续作为原始仓库
GitHub：用于 CI/CD
```

也可以后续把 GitHub 作为主仓库。

基本思路：

```text
本地代码
-> push 到 Gitee
-> push 到 GitHub
-> GitHub Actions 自动运行
```

## 5. CI 和 CD 的区别

### 5.1 CI：持续集成

作用是检查代码能否正常构建。

触发方式：

```text
push
pull request
```

CI 做的事情：

```text
后端：
- 安装 Java 17
- Maven package

console 前端：
- 安装 Node
- npm ci
- npm build

judge 前端：
- 安装 Node
- npm ci
- npm build
```

CI 不会部署到服务器。

### 5.2 CD：持续部署

作用是把构建产物发布到服务器。

当前阶段建议手动触发：

```text
workflow_dispatch
```

CD 做的事情：

```text
- 构建后端 jar
- 构建 console dist
- 构建 judge dist
- 打包 release.zip
- 上传 release.zip 到 ECS
- SSH 进入 ECS
- 执行 deploy.sh
```

当前不建议每次 push 自动部署。项目还在频繁调整，手动发布更稳。

## 6. 发布包结构

GitHub Actions 最终打包成：

```text
release.zip
├─ app
│  └─ beer-api.jar
└─ www
   ├─ console
   │  ├─ index.html
   │  ├─ favicon.ico
   │  └─ assets
   └─ judge
      ├─ index.html
      ├─ favicon.ico
      └─ assets
```

服务器解压后覆盖：

```text
app/beer-api.jar -> /opt/beer-competition/app/beer-api.jar
www/console/*    -> /opt/beer-competition/www/console/
www/judge/*      -> /opt/beer-competition/www/judge/
```

## 7. 服务器部署脚本职责

服务器上需要一个脚本：

```text
/opt/beer-competition/scripts/deploy.sh
```

它负责：

```text
1. 接收 release.zip
2. 解压到临时目录
3. 备份当前 jar
4. 备份当前前端 dist
5. 覆盖新的 jar
6. 覆盖新的 console
7. 覆盖新的 judge
8. 重新构建并启动 beer-api 容器
9. 检查 Nginx 配置
10. reload Nginx
11. 调用健康检查接口
12. 输出部署结果
```

这样 GitHub Actions 不需要知道服务器内部细节，只要上传文件并执行脚本。

## 8. GitHub Secrets

GitHub 需要保存服务器连接信息。

建议配置：

```text
ECS_HOST=服务器公网 IP
ECS_PORT=22
ECS_USER=root 或 deploy
ECS_SSH_KEY=SSH 私钥
```

这些不能写在代码里，要放在 GitHub 仓库的 Secrets 里。

后续如果用阿里云镜像仓库，还会增加：

```text
ACR_REGISTRY
ACR_USERNAME
ACR_PASSWORD
```

当前阶段暂时不需要。

## 9. 前端构建注意事项

console 构建：

```bash
npm run build -- --base=/console/
```

judge 构建：

```bash
npm run build -- --base=/judge/
```

不要再设置：

```bash
VITE_API_BASE_URL=/api
```

当前代码已经支持生产环境同源请求，接口会走：

```text
/api/...
```

避免出现：

```text
/api/api/...
```

## 10. Nginx 最终配置方向

后续发布新版前端后，可以删除临时兼容：

```nginx
location /api/api/ {
    ...
}
```

只保留：

```nginx
location /api/ {
    proxy_pass http://127.0.0.1:8080/api/;
}
```

前端静态资源：

```nginx
location /console/ {
    root /opt/beer-competition/www;
    try_files $uri $uri/ /console/index.html;
}

location /judge/ {
    root /opt/beer-competition/www;
    try_files $uri $uri/ /judge/index.html;
}
```

## 11. 推荐实施阶段

### 阶段 1：代码同步 GitHub

目标：

```text
GitHub 上有完整项目代码
```

完成后能做：

```text
git push github main
```

### 阶段 2：创建服务器 deploy.sh

目标：

```text
手动上传 release.zip
执行 deploy.sh
完成部署
```

这一步先不接 GitHub Actions。

### 阶段 3：搭建 CI

目标：

```text
每次 push 自动检查构建是否成功
```

不部署。

### 阶段 4：搭建手动 CD

目标：

```text
在 GitHub Actions 页面手动点击 Deploy
自动上传并部署到服务器
```

### 阶段 5：优化发布体验

可选增强：

```text
- 发布前自动数据库备份
- 保留最近 5 个 release
- 失败自动回滚
- 部署后自动健康检查
- 部署完成通知
```

## 12. 最终工作流

理想流程：

```text
开发代码
-> 本地测试
-> push 到 GitHub
-> GitHub CI 自动构建检查
-> 确认没问题
-> 手动触发 Deploy
-> GitHub Actions 构建 release.zip
-> 上传到 ECS
-> ECS 执行 deploy.sh
-> 自动重启 API
-> 自动 reload Nginx
-> 访问页面验证
```

## 13. 当前推荐下一步

先不要直接写 GitHub Actions。

推荐顺序：

```text
1. 同步代码到 GitHub
2. 在服务器写 deploy.sh
3. 手动用 deploy.sh 发布一次
4. 再搭建 CI
5. 最后搭建手动 CD
```

这样每一步都能单独验证，出问题也容易定位。
