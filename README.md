# Beer Competition System

啤酒大赛管理系统采用单仓多项目结构。一期业务系统已完成，二期进入 SaaS 化设计阶段。

```text
beer-competition-system
├─ beer-competition-api
├─ beer-competition-console
├─ beer-competition-judge-h5
└─ docs
```

## 模块说明

- `beer-competition-api`
  Spring Boot 3 + MyBatis-Plus 后端，提供后台、厂商 Portal、评审 H5 三类接口。
- `beer-competition-console`
  Vue 3 + Element Plus 桌面端，承载厂商 Portal 与主办方后台。
- `beer-competition-judge-h5`
  Vue 3 移动端 H5，承载评审登录、扫码评分、同桌确认、桌长汇总和后续轮排序。

## 项目文档

文档入口：`docs/README.md`。

- 一期系统基线：`docs/01-一期-系统基线/`
- 二期 SaaS 化：`docs/02-二期-SaaS化/`
- 工程约定：`docs/03-工程约定/`
- 部署运维：`docs/04-运维/`

## 本地启动

1. 将根目录 `.env.example` 复制为 `.env` 并填写数据库、Redis 等配置。
2. 导入数据库脚本：

```text
beer-competition-api/src/main/resources/db/migrations/beer_competition_full.sql
```

3. 启动后端：

```bash
cd beer-competition-api
mvn spring-boot:run
```

4. 启动桌面端：

```bash
cd beer-competition-console
npm install
npm run dev
```

5. 启动评审 H5：

```bash
cd beer-competition-judge-h5
npm install
npm run dev
```

## 默认测试账号

- 后台管理员：`admin / 123456`
- 厂商手机号：`13800000001`
- 评审账号：通过评审 H5 注册新手机号，后台审核通过后加入比赛编排
- 本地验证码：`123456`
