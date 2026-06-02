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





在构建前端页面的时候文案要站在用户而非系统开发者的角度考虑，
添加文案和组件必须审视一遍能不能起到有效的说明或显示作用，禁止添加对用户来说无意义或明显不必要的文案或组件
