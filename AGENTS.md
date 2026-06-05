## Writing Style

- 中文讲解和文档写作中，少用“不是，而是”这类固定对照句式；需要表达区别时，优先用直接说明、表格或分点对比，避免反复出现同一种表达。

## Local Database

- Host: `localhost`
- Port: `3306`
- Database: `beer_competition`
- Username: `root`
- Password: `0d000721`

```env
APP_DB_HOST=localhost
APP_DB_PORT=3306
APP_DB_NAME=beer_competition
APP_DB_USERNAME=root
APP_DB_PASSWORD=0d000721
```

- 本地数据库有备份，后续可以按需连接本地数据库进行操作。

### 中文测试数据写入注意事项

- 在 Windows PowerShell 中向 `mysql`、`node` 等命令通过管道传递包含中文的 SQL 或脚本时，容易出现中文被写成 `???` 的编码问题。
- 往数据库写入中文测试数据时，优先使用以下方式之一：
  - 生成 UTF-8 SQL 文件后，通过 `mysql --default-character-set=utf8mb4 ... < file.sql` 执行。
  - 如果必须通过命令管道生成 SQL，脚本源码保持 ASCII，中文用 Unicode 转义生成，再写成 `CONVERT(0x... USING utf8mb4)` 形式的 SQL 字面量。
- 插入或修正中文测试数据后，必须抽查 `HEX(中文字段)`。如果结果是 `3F3F3F` 或大量 `3F`，说明中文已经被替换成问号，需要立即修正。
- 涉及评委账号等隐私字段时，不能直接写入明文手机号或微信号。需按后端 `PiiService` 的规则生成 `phone_enc`、`phone_hash`、`wechat_enc`，并确认后台列表可以正常解密展示。

## Browser Acceptance

- 厂商端和后台端进行浏览器验收时，使用桌面端全屏尺寸验收。
- 验收后台端页面时，优先覆盖后台常用宽屏工作场景，确认表格、操作按钮、筛选区和侧边栏在全屏下布局稳定。
- 验收厂商端页面时，确认首页流程、表单、列表和关键操作在桌面全屏下没有遮挡、错位或无意义文案。
- 如果我没有明确让你验收，不用主动调用浏览器验收

## Frontend Copy

- 在构建前端页面的时候文案要站在用户而非系统开发者的角度考虑。
-在加一项说明文字的时候要站在业务与用户的角度考虑，如果没有起到应有的装饰或说明功能，就不要加这处说明文字！