# 后端模块约定

`beer-competition-api` 当前按业务域组织：

```text
controller
├─ admin
├─ judge
├─ portal
└─ publicapi
service
service/impl
mapper
pojo
├─ dto
├─ enums
├─ po
└─ vo
common
security
config
storage
```

## 约定

- `controller` 负责参数接收和统一返回。
- `service/impl` 负责业务编排和权限边界。
- `mapper` 只做数据库读写。
- `pojo/dto` 用于请求参数。
- `pojo/vo` 用于响应结构。
- `pojo/po` 对应数据库表。
- `security` 负责 JWT 和角色拦截。
- `storage` 提供文件存储抽象。
