# 助农电商平台 - Apifox 测试指南

> 版本：v1.0  
> 更新日期：2026-03-07  
> 适用对象：后端接口测试、毕业设计答辩演示

---

## 一、环境准备

### 1.1 软件要求
- **Apifox 客户端**：下载地址 https://www.apifox.cn/ （Windows/Mac 均可）
- **后端服务**：确保本地服务已启动 `http://localhost:8080`
- **数据库**：MySQL 已运行，且已执行 `schema.sql` 初始化脚本

### 1.2 服务启动检查清单
```bash
# 1. 检查 MySQL 是否运行
mysql -u root -p -e "SELECT 1;"

# 2. 检查 Redis 是否运行（Windows）
redis-cli ping
# 应返回 PONG

# 3. 启动后端服务（在 IntelliJ IDEA 中）
# 运行 AssistPlatformApplication.java

# 4. 验证服务启动
curl http://localhost:8080/api/user/home
# 应返回 JSON 数据
```

---

## 二、导入 OpenAPI 文档到 Apifox

### 2.1 导入步骤

#### 步骤 1：打开 Apifox
1. 启动 Apifox 客户端
2. 登录账号（没有可注册免费账号）
3. 进入主界面

#### 步骤 2：创建项目
1. 点击左侧「+ 新建项目」
2. 项目名称：`助农电商平台`
3. 项目描述：`毕业设计 - 助农电商平台 API 测试`
4. 点击「创建」

#### 步骤 3：导入 OpenAPI 文件
1. 进入项目后，点击左侧「导入」按钮
2. 选择「OpenAPI/Swagger」
3. 选择导入方式：「文件导入」
4. 点击「选择文件」，选择 `api-tests/apifox-openapi-import.json`
5. 点击「确认导入」

#### 步骤 4：配置环境
1. 点击右上角「环境管理」
2. 点击「+ 新建环境」
3. 环境名称：`本地开发环境`
4. 前置 URL：`http://localhost:8080/api`
5. 点击「保存」

#### 步骤 5：设置全局参数（可选）
1. 在环境配置中，点击「全局参数」
2. 添加 Header 参数：
   - Content-Type: application/json
3. 点击「保存」

---

## 三、接口测试流程

### 3.1 测试顺序建议

按照以下顺序测试，确保依赖关系正确：

```
阶段一：基础功能测试
├── 1. 用户注册/登录
├── 2. 首页数据获取
├── 3. 商品浏览
└── 4. 新闻浏览

阶段二：用户端核心流程
├── 1. 添加收货地址
├── 2. 添加购物车
├── 3. 创建订单
├── 4. 支付订单
├── 5. 确认收货
└── 6. 提交评价

阶段三：商家端流程
├── 1. 商家注册/登录
├── 2. 完善店铺信息
├── 3. 添加商品
├── 4. 处理订单（发货）
└── 5. 处理售后

阶段四：管理员端流程
├── 1. 管理员登录
├── 2. 审核商家
├── 3. 审核商品
├── 4. 查看订单/资金
└── 5. 新闻管理
```

### 3.2 详细测试步骤

#### 【测试 1】用户注册

**接口**：`POST /user/sms/send`

**请求参数**：
```json
{
  "phone": "13800138001"
}
```

**预期响应**：
```json
{
  "code": 200,
  "msg": "发送成功",
  "data": null
}
```

> 注意：测试环境使用固定验证码 `123456`

---

#### 【测试 2】用户登录

**接口**：`POST /user/login/password`

**请求参数**：
```json
{
  "phone": "13800138001",
  "password": "123456"
}
```

**预期响应**：
```json
{
  "code": 200,
  "msg": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "userInfo": {
      "id": 1,
      "phone": "13800138001",
      "nickname": "测试用户"
    }
  }
}
```

**重要**：保存返回的 `token`，后续请求需要在 Header 中携带：
```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

---

#### 【测试 3】设置 Token 自动继承

1. 在 Apifox 中，进入「登录接口」的「后置操作」
2. 点击「+ 添加后置操作」
3. 选择「提取变量」
4. 变量名：`userToken`
5. JSON Path：`$.data.token`
6. 保存

然后在其他需要登录的接口中：
1. 进入「Header」标签
2. 添加：`Authorization: Bearer {{userToken}}`

---

#### 【测试 4】获取首页数据

**接口**：`GET /user/home`

**Header**：
```
Authorization: Bearer {{userToken}}
```

**预期响应**：
```json
{
  "code": 200,
  "data": {
    "recommendProducts": [...],
    "hotProducts": [...],
    "news": [...]
  }
}
```

---

#### 【测试 5】商品搜索

**接口**：`GET /user/products/search`

**Query 参数**：
```
keyword: 苹果
sortBy: sales
categoryId: 1
pageNum: 1
pageSize: 10
```

---

#### 【测试 6】添加收货地址

**接口**：`POST /user/addresses`

**Header**：
```
Authorization: Bearer {{userToken}}
```

**请求参数**：
```json
{
  "receiver": "张三",
  "phone": "13800138001",
  "province": "广东省",
  "city": "深圳市",
  "county": "南山区",
  "town": "粤海街道",
  "detailAddress": "科技园南路88号",
  "isDefault": 1
}
```

---

#### 【测试 7】添加购物车

**接口**：`POST /user/cart/add`

**请求参数**：
```json
{
  "productId": 1,
  "productNum": 2
}
```

---

#### 【测试 8】创建订单

**接口**：`POST /user/orders`

**请求参数**（购物车下单）：
```json
{
  "addressId": 1,
  "cartIds": [1, 2]
}
```

**或**（直接购买）：
```json
{
  "addressId": 1,
  "productItems": [
    {
      "productId": 1,
      "productNum": 2
    }
  ]
}
```

**预期响应**：
```json
{
  "code": 200,
  "data": {
    "orderNo": "ORDER202403070001",
    "totalAmount": 59.80,
    "orderStatus": 1
  }
}
```

---

#### 【测试 9】支付订单

**接口**：`POST /user/orders/{id}/pay`

> 注意：这是模拟支付，实际项目中会对接支付宝/微信支付

---

#### 【测试 10】商家注册

**接口**：`POST /merchant/register`

**请求参数**：
```json
{
  "phone": "13900139001",
  "code": "123456",
  "merchantName": "绿野农产品店",
  "contactPerson": "王老板",
  "contactPhone": "13900139001",
  "password": "merchant123"
}
```

---

#### 【测试 11】商家登录

**接口**：`POST /merchant/login`

**请求参数**：
```json
{
  "phone": "13900139001",
  "password": "merchant123"
}
```

**保存 token**：`merchantToken`

---

#### 【测试 12】商家添加商品

**接口**：`POST /merchant/products`

**Header**：
```
Authorization: Bearer {{merchantToken}}
```

**请求参数**：
```json
{
  "productName": "山东红富士苹果",
  "categoryId": 1,
  "price": 29.9,
  "stock": 100,
  "stockWarning": 10,
  "productImg": "https://example.com/apple.jpg",
  "productDesc": "新鲜采摘，口感脆甜",
  "originPlace": "山东烟台",
  "plantingCycle": "春季种植，秋季收获",
  "originPlaceDetail": "山东烟台栖霞市",
  "fertilizerType": "有机肥料",
  "storageMethod": "冷藏保存",
  "transportMethod": "冷链运输"
}
```

---

#### 【测试 13】管理员登录

**接口**：`POST /admin/login`

**请求参数**：
```json
{
  "username": "admin",
  "password": "admin123",
  "phone": "13800000000",
  "code": "123456"
}
```

**保存 token**：`adminToken`

---

#### 【测试 14】管理员审核商家

**接口**：`PUT /admin/merchants/{id}/audit`

**Header**：
```
Authorization: Bearer {{adminToken}}
```

**请求参数**：
```json
{
  "pass": true
}
```

或驳回：
```json
{
  "pass": false,
  "rejectReason": "资质图片模糊，请重新上传"
}
```

---

## 四、测试用例导入（可选）

除了 OpenAPI 文档，还可以导入测试用例：

### 4.1 导入测试用例
1. 在 Apifox 中，点击「测试」标签
2. 点击「+ 新建测试步骤」
3. 选择「导入测试用例」
4. 选择文件 `api-tests/apifox-test-cases.json`
5. 点击「确认导入」

### 4.2 运行测试套件
1. 导入后会生成测试套件
2. 点击「运行全部」
3. 查看测试结果报告

---

## 五、自动化测试配置

### 5.1 设置测试环境变量

在 Apifox 环境配置中，添加以下变量：

| 变量名 | 初始值 | 说明 |
|--------|--------|------|
| baseUrl | http://localhost:8080/api | 基础URL |
| userPhone | 13800138001 | 测试用户手机号 |
| merchantPhone | 13900139001 | 测试商家手机号 |
| adminUsername | admin | 管理员账号 |
| userToken | | 用户Token（自动填充） |
| merchantToken | | 商家Token（自动填充） |
| adminToken | | 管理员Token（自动填充） |

### 5.2 配置自动化测试步骤

#### 步骤 1：用户登录并提取 Token
```
接口：POST /user/login/password
后置操作：
  - 提取变量：userToken
  - JSON Path：$.data.token
```

#### 步骤 2：使用 Token 调用其他接口
```
接口：GET /user/info
Header：Authorization: Bearer {{userToken}}
```

### 5.3 批量运行测试

1. 创建「测试步骤」集合
2. 按顺序添加接口：
   - 用户登录
   - 获取首页
   - 添加购物车
   - 创建订单
   - 支付订单
3. 点击「运行测试步骤」
4. 查看测试报告

---

## 六、常见问题解决

### Q1：导入后接口显示不正确？
**解决**：
- 检查 JSON 文件格式是否正确
- 尝试重新导入
- 手动调整接口路径和参数

### Q2：请求返回 401 未授权？
**解决**：
- 检查是否正确携带 `Authorization` Header
- 确认 Token 格式为 `Bearer {token}`
- 确认 Token 未过期

### Q3：请求返回 404？
**解决**：
- 检查服务是否已启动
- 检查 URL 是否正确
- 检查路径参数是否正确

### Q4：数据库连接失败？
**解决**：
- 检查 MySQL 是否运行
- 检查 `application.yml` 中的数据库配置
- 确认数据库 `nongnong_ecommerce` 已创建

### Q5：Redis 连接失败？
**解决**：
- 检查 Redis 是否运行
- Windows 可使用 `redis-server.exe` 启动
- 检查 `application.yml` 中的 Redis 配置

---

## 七、测试数据准备

### 7.1 初始化测试数据 SQL

```sql
-- 执行 schema.sql 后，插入测试数据

-- 商品分类
INSERT INTO product_category (category_name, parent_id, category_level) VALUES
('新鲜水果', 0, 1),
('时令蔬菜', 0, 1),
('粮油副食', 0, 1),
('苹果', 1, 2),
('橙子', 1, 2);

-- 新闻分类
INSERT INTO news_category (category_name, category_code) VALUES
('助农政策', 'policy'),
('农业技术', 'technology'),
('市场动态', 'market');

-- 新闻数据
INSERT INTO news (title, category_id, content, cover_img, audit_status) VALUES
('2024年助农新政策发布', 1, '近日，国家发布新的助农政策...', 'https://example.com/news1.jpg', 1),
('春季种植技术指南', 2, '春季是农作物种植的关键时期...', 'https://example.com/news2.jpg', 1);
```

### 7.2 测试账号

| 角色 | 手机号 | 密码 | 用途 |
|------|--------|------|------|
| 测试用户 | 13800138001 | 123456 | 用户端测试 |
| 测试商家 | 13900139001 | merchant123 | 商家端测试 |
| 管理员 | - | admin123 | 管理员端测试 |

---

## 八、导出测试报告

### 8.1 导出接口文档
1. 点击「分享」按钮
2. 选择「导出文档」
3. 选择格式：Markdown / HTML / PDF
4. 点击「导出」

### 8.2 导出测试结果
1. 运行测试套件
2. 点击「查看报告」
3. 点击「导出报告」
4. 选择格式：HTML / PDF

---

## 九、毕业设计答辩演示建议

### 9.1 演示流程（5-8分钟）

1. **项目介绍**（1分钟）
   - 助农电商平台背景
   - 技术栈介绍

2. **功能演示**（3-5分钟）
   - 用户注册/登录
   - 商品浏览/搜索
   - 下单支付流程
   - 商家后台管理
   - 管理员审核

3. **接口测试演示**（1-2分钟）
   - 打开 Apifox
   - 展示接口文档
   - 运行几个核心接口
   - 展示测试结果

### 9.2 答辩要点

- 强调接口设计的规范性（RESTful）
- 展示接口文档的完整性
- 说明测试覆盖率
- 演示自动化测试能力

---

## 十、联系与支持

如有问题，可查阅：
- 项目文档：`项目需求分析.md`
- 接口设计：`项目接口设计.md`
- 数据库设计：`数据库设计.md`

---

**祝测试顺利，答辩成功！**
