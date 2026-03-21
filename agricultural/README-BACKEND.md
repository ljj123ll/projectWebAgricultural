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

  <br />

