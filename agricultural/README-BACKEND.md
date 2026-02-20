# 助农产品销售系统 - 后端说明

基于 **项目需求分析.md**、**项目接口设计.md**、**数据库设计.md** 实现的 Spring Boot 后端，提供用户端、商家端、管理员端 RESTful API。

## 技术栈

- Java 17、Spring Boot 3.5
- MyBatis-Plus、MySQL、Redis
- JWT 认证、Swagger（SpringDoc OpenAPI 3）
- Lombok、Hutool、ZXing（溯源二维码）

## 运行前准备

1. **MySQL**：创建数据库 `nongnong_ecommerce`，执行 `sql/schema.sql` 初始化表结构与初始数据。
2. **Redis**：本地 6379 端口（或修改 `application.yml`）。
3. **配置**：在 `src/main/resources/application.yml` 中修改数据库连接与 JWT 密钥（可选）。

## 启动方式

```bash
mvn spring-boot:run
```

服务启动后：

- **Base URL**：`http://localhost:8080/api`
- **Swagger 文档**：`http://localhost:8080/api/swagger-ui.html`

## 接口分组（与接口设计文档一致）

| 端     | 模块         | 路径前缀   | 说明                     |
|--------|--------------|------------|--------------------------|
| 用户端 | 登录注册     | `/user`    | 短信验证码、注册、登录、重置密码、退出 |
| 用户端 | 首页/商品    | `/user`    | 首页数据、商品搜索、详情、热销榜     |
| 用户端 | 购物车       | `/user/cart` | 查看、添加、修改数量、删除         |
| 用户端 | 订单         | `/user/orders` | 创建、列表、详情、取消、支付、确认收货 |
| 用户端 | 个人中心     | `/user/info`、`/user/addresses` | 信息、地址、默认地址   |
| 用户端 | 售后/评价    | `/user/after-sale`、`/user/comments` | 发起售后、售后列表、提交评价 |
| 商家端 | 登录审核     | `/merchant` | 注册、登录、审核状态             |
| 商家端 | 商品管理     | `/merchant/products` | 列表、新增、编辑、溯源码、上下架   |
| 商家端 | 订单/售后    | `/merchant/orders`、`/merchant/after-sale` | 订单列表、发货、售后列表与处理 |
| 商家端 | 数据对账     | `/merchant/stats`、`/merchant/reconciliation`、`/merchant/subsidy` | 核心数据、对账、补贴 |
| 管理员 | 登录         | `/admin/login` | 账号+密码+短信验证码            |
| 管理员 | 审核         | `/admin/merchants/audit`、`/admin/products/audit` | 商家/商品审核列表与操作 |
| 管理员 | 资金/看板    | `/admin/transfers`、`/admin/orders/logistics-abnormal`、`/admin/dashboard`、`/admin/merchants/rank` | 打款、物流异常、看板、排行 |

## 认证说明

- 除登录/注册/发送验证码等接口外，需在请求头携带：`Authorization: Bearer <Token>`。
- 用户/商家登录接口返回的 `token` 即 JWT，有效期 7 天（可在 `application.yml` 的 `jwt.expire-days` 修改）。
- 管理员登录：需先对管理员手机号发送验证码（调用用户端 `POST /user/sms/send`，传入管理员手机号），再调用 `POST /admin/login`，body 示例：`{"username":"admin","password":"admin123","phone":"13800000000","code":"123456"}`。开发环境固定验证码为 `123456`。

## 测试账号（初始化后）

- **管理员**：用户名 `admin`，密码 `admin123`，手机号在 `sys_user` 表中（如 `13800000000`），验证码使用 `123456`（开发环境）。
- **用户/商家**：通过注册接口自行注册；短信验证码开发环境统一为 `123456`。

## 项目结构概览

```
src/main/java/com/agricultural/assistplatform/
├── common/          # 统一响应、分页、登录上下文
├── config/          # 全局异常、CORS、JWT 过滤器、OpenAPI
├── controller/      # user、merchant、admin 各端 Controller
├── dto/             # 请求 DTO
├── entity/          # 数据库实体（与数据库设计一致）
├── exception/       # 业务异常
├── mapper/          # MyBatis-Plus Mapper
├── service/         # 业务逻辑（含 SmsService、user/merchant/admin 子包）
├── util/            # JwtUtil、QrCodeUtil
└── vo/              # 响应 VO
```

更多业务细节请参考 **项目需求分析.md** 与 **项目接口设计.md**。

你可以用 **Apifox 直接导入 OpenAPI 文档**，然后在 Apifox 里点接口“发送”来测。

## 1）先确认 OpenAPI 文档地址
你的后端是 `context-path: /api`，所以一般是：

- **OpenAPI JSON**：`http://localhost:8080/api/v3/api-docs`
- **Swagger UI**：`http://localhost:8080/api/swagger-ui/index.html`

在浏览器打开 `http://localhost:8080/api/v3/api-docs` 能看到一大段 JSON，说明文档接口正常。

## 2）把 Swagger(OpenAPI) 导入 Apifox
- 打开 Apifox → **项目**
- 左侧 **接口** → 右上角 **导入**
- 选择 **OpenAPI / Swagger**
- 导入方式选 **URL 导入**
- 填入：`http://localhost:8080/api/v3/api-docs`
- 点击 **导入**（可勾选“覆盖更新/追加”，按你项目情况选）

导入成功后，Apifox 会生成一整套接口分组（用户端/商家端/管理员端等）。

## 3）在 Apifox 配环境（Base URL）
- 进入 Apifox 右上角 **环境**（或项目设置里的环境）
- 新建 `dev` 环境
- **Base URL** 填：`http://localhost:8080/api`

这样 Apifox 里接口路径（如 `/user/home`）会自动拼成完整地址。

## 4）带 Token 的接口怎么测（最关键）
你项目里认证是请求头：

- `Authorization: Bearer <token>`

### 方法 A：环境变量 + 自动带上 Header（推荐）
- 在环境里新增变量：`token`
- 先调用登录接口拿 token：
  - 用户登录：`POST /user/login/sms` 或 `POST /user/login/password`
  - 商家登录：`POST /merchant/login`
  - 管理员登录：`POST /admin/login`
- 登录接口返回里有 `data.token`，复制到环境变量 `token`

然后在需要鉴权的接口里加 Header：
- Key：`Authorization`
- Value：`Bearer {{token}}`

### 方法 B：Apifox 的“公共请求头”
在 Apifox 项目/环境的 **公共请求头** 里统一加：
- `Authorization: Bearer {{token}}`

后面所有接口都会自动携带（更省事）。

## 5）快速验证一条完整链路（示例：用户端）
1. `POST /user/sms/send`  
   Body：`{"phone":"13800138000"}`  
   （开发环境验证码固定 `123456`）
2. `POST /user/register` 或 `POST /user/login/sms`  
   Body：`{"phone":"13800138000","code":"123456"}`  
   拿到 `data.token`
3. 设置环境变量 `token`
4. 测一个需要登录的接口，比如：
   - `GET /user/info`
   - `GET /user/cart`
   - `POST /user/orders`

如果返回 `code:200`，说明链路正常。

如果你愿意，把你 Apifox 导入后“某个接口发送失败的响应/截图（含请求URL、Header、Body、返回）”发我，我可以帮你定位是参数、鉴权还是后端逻辑问题。