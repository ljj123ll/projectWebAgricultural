你需要在1个月内实现一个三分端（用户端/商家端/管理员端）的助农电商平台，采用Vue 3+SpringBoot前后端分离技术栈，核心要求是**优先保障核心链路可用，简化非核心功能，适配农村用户易用性**。由于时间紧张，不能追求面面俱到，必须按「优先级拆分+阶段落地」的思路推进，下面给你一套可落地、按时间拆分的从0到1实现方案。

## 一、先明确核心原则（一个月落地的关键）
1.  **核心链路优先**：先保障「用户浏览-下单-支付（模拟）-商家处理订单-管理员审核」这条主流程跑通，售后、数据看板、助农新闻等非核心功能后置简化。
2.  **拒绝完美主义**：非核心功能只做「可用级」，比如短信验证码先用模拟、支付用假数据、二维码本地生成、物流只存单号不做实时跟踪。
3.  **前后端协同先行**：先约定接口规范，再并行开发（若你独自开发，先做后端核心接口，再做前端对接）。
4.  **轻量化实现**：避免复杂配置、第三方服务深度集成，优先用技术栈原生工具和轻量依赖。

## 二、时间拆分（4周，每周有明确交付物）
### 第1周：前期准备 + 基础架构搭建 + 数据库设计（核心：搭好开发环境，定好基础骨架）
#### 任务1：环境搭建（1-2天）
先把所有开发环境和工具配置到位，避免后续踩环境坑，步骤如下：
1.  **后端环境**
    - 安装JDK 1.8（SpringBoot兼容性最好，新手不易出问题）。
    - 安装Maven 3.6+，配置阿里云镜像（加速依赖下载）。
    - 安装MySQL 8.0，用Navicat创建数据库（命名：`agricultural_assist`），配置字符集`utf8mb4`。
    - 安装Redis 6.0+，启动Redis服务（无需复杂配置，默认端口即可，用于缓存登录态、库存、验证码）。
    - 验证：在IDEA中创建一个空的SpringBoot项目，引入`spring-boot-starter-web`，启动后访问`http://localhost:8080`能看到404页面即成功。
2.  **前端环境**
    - 安装Node.js 16+（LTS版本，兼容Vite）。
    - 验证：终端执行`npm -v`、`node -v`能显示版本号，再执行`npm install -g pnpm`（推荐用pnpm，比npm更快）。
    - 安装VS Code，安装插件：Volar（Vue 3必备）、TypeScript Vue Plugin、Element Plus Snippets。
3.  **辅助工具**
    - 配置Git，创建本地仓库（用于版本控制，避免代码丢失）。
    - 安装Apifox，用于接口约定、调试、生成接口文档。

#### 任务2：项目初始化（2天）
1.  **后端项目初始化（SpringBoot）**
    - 用IDEA的Spring Initializr创建项目，勾选以下核心依赖（其他依赖后续按需添加）：
      - Web（Spring Web）
      - MyBatis-Plus（简化CRUD操作）
      - MySQL Driver（数据库驱动）
      - Redis（Spring Data Redis）
      - Lombok（简化实体类代码）
      - Validation（参数校验）
    - 后续手动添加额外核心依赖（在`pom.xml`中）：
      - JWT（用于身份认证）：`jjwt`
      - Swagger/Knife4j（接口文档，方便调试）
      - ZXing（生成溯源二维码）
      - Hutool（工具类库，简化验证码、日期等操作，减少重复造轮子）
    - 核心配置（`application.yml`）：配置数据库连接、Redis连接、端口（默认8080）、MyBatis-Plus别名、Swagger配置。
    - 搭建后端基础骨架（包结构）：
      ```
      com.agricultural
      ├── common/          // 通用工具类（返回结果、异常处理、常量、Redis工具等）
      ├── config/          // 配置类（MyBatis-Plus、Redis、JWT、CORS跨域等）
      ├── module/          // 业务模块（按端拆分，后续填充）
      │   ├── user/        // 用户端相关（用户、商品、订单等）
      │   ├── merchant/    // 商家端相关（店铺、商品管理、订单处理等）
      │   ├── admin/       // 管理员端相关（审核、管控等）
      ├── Application.java // 启动类
      ```
2.  **前端项目初始化（Vue 3 + Vite + Element Plus）**
    - 由于有3个端，**新手推荐创建3个独立前端项目**（避免路由混乱，开发效率更高），分别命名：`user-web`、`merchant-web`、`admin-web`。
    - 创建命令（终端）：
      ```bash
      # 创建用户端
      pnpm create vite user-web --template vue-ts
      cd user-web
      pnpm install
      # 安装核心依赖
      pnpm install element-plus pinia vue-router axios vant @vueuse/core
      ```
    - 核心依赖说明：
      - Element Plus：UI组件库（管理员端/商家端）
      - Vant：移动端适配UI组件库（用户端，更轻量化，适配手机端）
      - Pinia：状态管理（缓存用户信息、登录态等）
      - Vue Router：路由跳转
      - Axios：请求后端接口
    - 搭建每个前端项目的基础骨架：配置路由、Pinia、Axios拦截器（统一处理请求头、响应结果、登录态过期）、Element Plus/Vant全局引入（新手简化操作）。

#### 任务3：数据库设计（2-3天）
数据库是项目的基石，必须先设计到位，优先覆盖核心功能，字段预留即可，无需过度设计。采用`MySQL`，核心表如下（用Navicat创建，或用MyBatis-Plus代码生成器生成）：
> 提示：所有表添加`create_time`、`update_time`（自动填充）、`del_flag`（逻辑删除，避免物理删除数据）字段。

1.  **用户端核心表**
    - `sys_user`（用户表）：id、phone（手机号，主键/唯一）、nickname、password、avatar、status（状态）、last_login_time。
    - `user_address`（用户地址表）：id、user_id、province、city、county、town（省市区乡镇）、detail_address、receiver、phone、is_default（是否默认）。
    - `product`（商品表）：id、merchant_id、product_name、category（品类）、price、stock、warning_stock（库存预警）、cover_img、pics（多图，用逗号分隔）、intro（介绍）、trace_info（溯源信息，JSON格式）、trace_qr_url（溯源二维码地址）、status（上下架状态）、score（评分）、sales（销量）、is_stockout（是否滞销）。
    - `shopping_cart`（购物车表）：id、user_id、product_id、quantity、create_time。
    - `order_main`（订单主表）：id（订单编号）、user_id、merchant_id、total_amount、address_id、pay_type（支付方式）、order_status（订单状态）、pay_time、delivery_time、receive_time、cancel_reason（取消原因）、expire_time（订单过期时间）。
    - `order_item`（订单项表）：id、order_id、product_id、product_name、price、quantity、product_img。
    - `user_evaluation`（用户评价表）：id、order_id、product_id、user_id、score（评分）、content、imgs、create_time。
    - `agricultural_news`（助农新闻表）：id、title、category（新闻分类）、content（MD格式内容）、cover_img、create_time。
2.  **商家端核心表**
    - `merchant`（商家表）：id、merchant_name、contact、phone、qualification_img（资质图片）、account（收款账户）、account_status（账户状态）、audit_status（审核状态）、audit_reason（审核驳回原因）、create_time。
    - `merchant_account_detail`（商家对账明细表）：id、merchant_id、order_id、amount、type（收入/补贴/退款）、pay_time、pay_status（打款状态）。
3.  **管理员端核心表**
    - `sys_admin`（管理员表）：id、username、password、phone、role（角色：超级管理员/内容管理员）、status。
    - `sys_operation_log`（操作日志表）：id、admin_id、operation、content、create_time。
    - `after_sale`（售后表）：id、order_id、user_id、merchant_id、type（售后类型）、reason、evidence_img、status（售后状态）、admin_intervene（是否管理员介入）、create_time。

> 便捷工具：使用MyBatis-Plus的「代码生成器」，可以根据数据库表直接生成实体类、Mapper、Service、Controller基础代码，节省大量时间。

### 第2周：后端核心接口开发（核心：完成三分端的核心业务接口，保证接口可调用）
本周聚焦后端接口开发，优先实现**核心链路接口**，非核心接口简化或后置，每个模块开发完成后，用Apifox调试通过。

#### 任务1：通用功能开发（1天）
先完成后端通用功能，后续业务模块直接复用：
1.  全局返回结果类（`Result.java`）：统一接口返回格式（code、message、data），避免每个接口单独封装。
2.  全局异常处理器（`GlobalExceptionHandler.java`）：捕获业务异常、系统异常，返回友好提示。
3.  JWT工具类：实现JWT的生成、解析、验证，用于登录态校验。
4.  Redis工具类：实现缓存的增、删、查、过期时间设置（用于缓存登录态、验证码、库存、订单过期时间）。
5.  验证码工具类：生成6位数字验证码，存入Redis（过期时间5分钟）。
6.  二维码生成工具类：基于ZXing，根据商品溯源信息生成二维码，保存到本地项目目录，返回二维码访问地址。
7.  定时任务配置：开启SpringBoot定时任务（`@EnableScheduling`），用于后续「订单超时取消」「自动打款」功能。

#### 任务2：用户端后端接口（2天）
优先实现核心链路：「登录注册」→「商品浏览/搜索/筛选」→「购物车」→「订单生成/支付/超时取消」→「个人中心/订单管理」。
1.  登录注册模块：
    - 发送验证码接口（模拟，无需对接短信平台，生成6位数字存Redis）。
    - 手机号+验证码注册接口。
    - 验证码免密登录/账号密码登录接口（登录成功生成JWT，存入Redis，有效期7天）。
2.  商品模块：
    - 首页商品列表（推荐、热销榜、滞销专区）接口。
    - 商品搜索/筛选（按名称、产地、品类）接口。
    - 商品详情/溯源信息查询接口。
    - 购物车增、删、改、查接口（修改数量时校验库存）。
3.  订单模块（核心）：
    - 生成订单接口（扣减库存，生成待付款订单，设置15分钟过期时间，存入Redis）。
    - 模拟支付接口（修改订单状态为「已支付」，取消过期时间）。
    - 订单超时取消定时任务（每分钟执行一次，查询过期未支付订单，修改状态为「已取消」，回滚库存）。
    - 订单列表查询（按状态分类）接口。
    - 物流单号填写查询接口（仅保存和返回物流号）。
4.  个人中心模块：
    - 用户信息修改接口。
    - 地址增、删、改、查接口。
    - 商品评价提交接口。

#### 任务3：商家端后端接口（2天）
优先实现：「商家登录/店铺审核查询」→「商品管理/溯源二维码生成」→「订单处理」→「对账明细」。
1.  商家登录/店铺模块：
    - 商家注册接口（提交信息后进入审核状态）。
    - 商家登录接口。
    - 店铺审核进度查询接口。
    - 店铺信息修改接口。
2.  商品管理模块：
    - 商品增、删、改、上下架接口。
    - 溯源信息填写+二维码生成接口（保存二维码本地地址）。
    - 库存修改/库存预警接口（库存低于预警值时，存入Redis用于前端弹窗提醒）。
3.  订单处理模块：
    - 订单列表查询（按状态分类，新订单红点提醒）。
    - 订单取消接口。
    - 物流信息填写接口（校验物流单号格式，修改订单状态为「已发货」）。
4.  对账模块：
    - 对账明细查询接口。
    - 补贴明细查询接口。

#### 任务4：管理员端后端接口（1天）
优先实现：「管理员登录」→「商家/商品审核」→「订单管控」→「新闻管理」。
1.  管理员登录接口（双重验证：账号密码+验证码）。
2.  商家审核模块：商家列表查询、批量审核/驳回接口、商家禁用接口。
3.  商品审核模块：商品列表查询、审核/驳回接口。
4.  订单管控模块：全平台订单查询、异常订单手动取消接口。
5.  新闻管理模块：新闻增、删、改、查接口。

### 第3周：前端核心页面开发与接口对接（核心：完成三分端核心页面，实现前后端联动，保证页面可操作）
本周聚焦前端开发，对接上周后端接口，优先实现核心页面，适配移动端（用户端），简化样式，保证功能可用即可。

#### 任务1：用户端前端（2天，基于Vue 3 + Vant）
用户端核心是「移动端适配」，按钮、字体放大，流程简单直观，核心页面如下：
1.  登录注册页面：手机号输入框、验证码输入框、发送验证码按钮（倒计时功能）、注册/登录按钮。
2.  首页：四大板块（推荐商品、助农热销榜、滞销专区、助农新闻入口），卡片式布局，适配移动端。
3.  商品列表/搜索/筛选页面：搜索框、筛选条件（产地、品类）、排序选项、商品卡片（显示名称、价格、库存、封面图）。
4.  商品详情页：多图轮播、商品基础信息、溯源二维码（点击查看溯源H5）、加入购物车按钮（选择数量，校验库存）。
5.  购物车页面：商品列表、修改数量、删除、清空、结算按钮。
6.  订单生成/支付页面：选择收货地址、订单明细、支付金额、微信/支付宝模拟支付按钮。
7.  个人中心页面：用户信息、订单分类入口、地址管理入口、评价管理入口。
8.  订单详情页：订单信息、物流单号（复制功能）、确认收货/申请售后按钮。

#### 任务2：商家端前端（2天，基于Vue 3 + Element Plus）
商家端核心是「简化操作」，减少冗余功能，核心页面如下：
1.  登录/店铺审核页面：登录表单、店铺审核进度查询（显示审核状态/驳回原因）。
2.  首页（工作台）：待办事项（未处理订单/售后）、7天核心数据（销量、营收，简易柱状图）。
3.  商品管理页面：商品列表（上下架状态、库存）、新增/编辑商品表单（简化字段，批量上传图片，溯源信息填写，二维码预览/下载）。
4.  订单管理页面：订单列表（按状态分类，红点提醒）、订单详情、取消订单、填写物流信息表单。
5.  对账明细页面：对账列表、补贴明细列表。

#### 任务3：管理员端前端（1天，基于Vue 3 + Element Plus）
管理员端核心是「权限管控/审核」，核心页面如下：
1.  登录页面：账号密码+短信验证码（模拟）双重验证。
2.  首页：平台数据看板（简易数字展示：总订单、总营收）。
3.  商家审核页面：商家列表、批量审核、查看资质图片、填写驳回原因。
4.  商品审核页面：商品列表、审核/驳回接口对接。
5.  订单管控页面：全平台订单查询、异常订单手动取消。
6.  新闻管理页面：新闻增、删、改、查，MD格式内容编辑（可使用第三方轻量MD编辑器）。

### 第4周：功能测试、bug修复、简化优化与部署（核心：保证项目可用，能够演示核心链路）
本周不再开发新功能，聚焦「修bug、补短板、做演示」，确保核心链路能够顺畅运行。

#### 任务1：功能测试与bug修复（2天）
1.  核心链路全流程测试：模拟「用户注册→浏览商品→加入购物车→下单→模拟支付→商家处理订单→填写物流→用户确认收货→评价商品→管理员审核商家/商品」，记录每一步的bug。
2.  重点测试高频问题：
    - 库存扣减/回滚是否正确（避免超卖）。
    - 订单超时是否能自动取消。
    - 登录态是否稳定（Redis缓存是否有效）。
    - 移动端适配是否正常（用户端在手机上打开是否清晰、操作是否便捷）。
    - 接口是否有冗余参数、返回结果是否正确。
3.  优先修复核心bug，非核心bug（如样式不美观、新闻格式错乱）可暂时搁置。

#### 任务2：简化优化与补充次要功能（1天）
1.  优化操作体验：农村用户易用性优化（按钮放大、减少输入项、提示语更直白）。
2.  补充次要功能：简化版售后功能（仅实现提交售后申请、商家查看）、助农新闻详情页（MD内容适配移动端）。
3.  清理冗余代码：删除开发过程中的测试代码、注释无用代码、优化项目结构。

#### 任务3：简易部署（1天）
由于是演示项目，无需部署到云服务器，做本地/内网部署即可，步骤如下：
1.  后端项目打包：用Maven打包成`jar`包（IDEA中执行`mvn clean package -Dmaven.test.skip=true`）。
2.  前端项目打包：每个前端项目执行`pnpm run build`，生成`dist`文件夹（静态资源）。
3.  本地运行：
    - 启动Redis、MySQL服务。
    - 终端执行`java -jar 项目包名.jar`，启动后端服务。
    - 用Nginx部署前端静态资源（或直接用VS Code的`Live Server`插件打开`dist`文件夹中的`index.html`）。
4.  验证：访问前端页面，能够正常登录、操作核心功能，接口调用正常。

## 三、关键技术点落地提示（新手避坑）
1.  **登录态管理**：JWT+Redis结合，JWT存前端本地存储（localStorage），Redis存JWT对应的用户信息（有效期7天），避免JWT无法作废的问题。
2.  **库存管控**：下单时先查询Redis库存，再扣减MySQL库存（加事务），避免超卖；订单取消时回滚库存，同样加事务。
3.  **订单超时**：用SpringBoot定时任务（`@Scheduled(cron = "0 */1 * * * ?")`），每分钟查询一次待付款订单，对比过期时间，超时则修改状态并回滚库存。
4.  **二维码生成**：生成的二维码图片保存到项目的`static`目录，前端直接通过`http://localhost:8080/xxx.png`访问。
5.  **移动端适配**：用户端使用Vant组件库，开启自适应布局（`viewport`设置），避免固定宽度，采用百分比/弹性布局。

### 总结
1.  一个月落地的核心是**优先级拆分**，先保障「下单-处理订单」核心链路，再补充次要功能，拒绝完美主义。
2.  技术落地优先**轻量化工具**（Hutool、MyBatis-Plus、Vant），减少重复造轮子，节省开发时间。
3.  开发流程遵循「环境搭建→数据库设计→后端接口→前端对接→测试部署」，每一步有明确交付物，避免混乱。

按照这套方案执行，你能够在一个月内完成一个可用的助农电商平台演示版本，覆盖三分端的核心功能，满足项目需求的核心诉求。



# 数据库设计
### 一、需求理解
你需要为这个面向农村用户的助农电商平台（包含用户端、商家端、管理员端）设计一套完整、规范且具备扩展性的数据库结构，要求覆盖所有核心业务链路（商品交易、溯源、订单、售后、审核、对账、补贴等），同时适配后续分阶段优化（如Redis扩展、分库分表、区块链溯源等）的需求。

### 二、数据库设计方案
#### 设计原则
1. 遵循**第三范式**，减少数据冗余，核心表预留扩展字段；
2. 字段类型精准（如金额用`decimal(10,2)`、手机号用`varchar(11)`），状态字段用`tinyint`（便于索引和枚举管理）；
3. 所有业务表添加`delete_flag`软删除字段、`create_time/update_time`时间字段，便于数据追溯；
4. 关键查询字段（如订单状态、用户ID、商家ID）添加索引，适配高频查询场景；
5. 预留分布式扩展能力（主键用`bigint`，支持雪花算法；表结构适配ShardingSphere分库分表）。

#### 核心表结构设计（MySQL）
##### 1. 系统基础模块（管理员端核心）
```sql
-- 管理员用户表
CREATE TABLE sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '管理员ID',
    username VARCHAR(50) NOT NULL COMMENT '登录账号',
    password VARCHAR(100) NOT NULL COMMENT '加密密码（BCrypt）',
    phone VARCHAR(11) NOT NULL COMMENT '手机号',
    real_name VARCHAR(20) COMMENT '真实姓名',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    status TINYINT DEFAULT 1 COMMENT '状态 0-禁用 1-正常',
    last_login_time DATETIME COMMENT '最后登录时间',
    delete_flag TINYINT DEFAULT 0 COMMENT '软删除 0-未删 1-已删',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_role_id (role_id),
    INDEX idx_phone (phone)
) COMMENT = '管理员用户表';

-- 角色表
CREATE TABLE sys_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '角色ID',
    role_name VARCHAR(30) NOT NULL COMMENT '角色名称（超级管理员/内容管理员）',
    role_code VARCHAR(30) NOT NULL COMMENT '角色编码（SUPER_ADMIN/CONTENT_ADMIN）',
    description VARCHAR(200) COMMENT '角色描述',
    delete_flag TINYINT DEFAULT 0 COMMENT '软删除 0-未删 1-已删',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_role_code (role_code)
) COMMENT = '角色表';

-- 权限表
CREATE TABLE sys_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '权限ID',
    perm_name VARCHAR(50) NOT NULL COMMENT '权限名称',
    perm_code VARCHAR(50) NOT NULL COMMENT '权限编码（如merchant:audit、order:manage）',
    parent_id BIGINT DEFAULT 0 COMMENT '父权限ID',
    delete_flag TINYINT DEFAULT 0 COMMENT '软删除 0-未删 1-已删',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_perm_code (perm_code)
) COMMENT = '权限表';

-- 角色权限关联表
CREATE TABLE sys_role_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '关联ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    perm_id BIGINT NOT NULL COMMENT '权限ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_role_perm (role_id, perm_id),
    INDEX idx_role_id (role_id),
    INDEX idx_perm_id (perm_id)
) COMMENT = '角色权限关联表';

-- 系统操作日志表
CREATE TABLE sys_operation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
    operator_id BIGINT NOT NULL COMMENT '操作人ID（管理员ID）',
    operator_name VARCHAR(50) NOT NULL COMMENT '操作人名称',
    operation_type VARCHAR(20) NOT NULL COMMENT '操作类型（新增/编辑/删除/审核）',
    business_type VARCHAR(50) NOT NULL COMMENT '业务类型（商家审核/商品审核/订单管控）',
    business_id BIGINT NOT NULL COMMENT '业务ID（如商家ID/商品ID）',
    operation_content TEXT COMMENT '操作内容（JSON格式）',
    ip VARCHAR(32) COMMENT '操作IP',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    INDEX idx_operator_id (operator_id),
    INDEX idx_business_type_id (business_type, business_id),
    INDEX idx_create_time (create_time)
) COMMENT = '系统操作日志表';
```

##### 2. 用户端核心模块
```sql
-- 消费者用户表
CREATE TABLE user_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    phone VARCHAR(11) NOT NULL COMMENT '手机号（唯一）',
    nickname VARCHAR(50) COMMENT '昵称（默认脱敏手机号）',
    password VARCHAR(100) COMMENT '密码（可选，免密登录可不填）',
    avatar_url VARCHAR(255) COMMENT '头像URL',
    last_login_time DATETIME COMMENT '最后登录时间',
    login_token VARCHAR(100) COMMENT 'Redis登录态token',
    token_expire_time DATETIME COMMENT 'token过期时间（7天）',
    status TINYINT DEFAULT 1 COMMENT '状态 0-禁用 1-正常',
    delete_flag TINYINT DEFAULT 0 COMMENT '软删除 0-未删 1-已删',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_phone (phone),
    INDEX idx_login_token (login_token)
) COMMENT = '消费者用户表';

-- 用户收货地址表
CREATE TABLE user_address (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '地址ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    receiver VARCHAR(20) NOT NULL COMMENT '收件人',
    phone VARCHAR(11) NOT NULL COMMENT '收件手机号',
    province VARCHAR(20) NOT NULL COMMENT '省',
    city VARCHAR(20) NOT NULL COMMENT '市',
    county VARCHAR(20) NOT NULL COMMENT '县/区',
    town VARCHAR(20) NOT NULL COMMENT '乡镇/街道',
    detail_address VARCHAR(200) NOT NULL COMMENT '详细地址',
    is_default TINYINT DEFAULT 0 COMMENT '是否默认 0-否 1-是',
    delete_flag TINYINT DEFAULT 0 COMMENT '软删除 0-未删 1-已删',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_user_default (user_id, is_default)
) COMMENT = '用户收货地址表';

-- 购物车表
CREATE TABLE cart (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '购物车ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    product_num INT NOT NULL COMMENT '商品数量',
    select_status TINYINT DEFAULT 1 COMMENT '是否选中 0-否 1-是',
    delete_flag TINYINT DEFAULT 0 COMMENT '软删除 0-未删 1-已删',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_product (user_id, product_id),
    INDEX idx_user_id (user_id)
) COMMENT = '购物车表';

-- 订单主表
CREATE TABLE order_main (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '订单ID',
    order_no VARCHAR(32) NOT NULL COMMENT '订单编号（唯一，雪花算法生成）',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    address_id BIGINT NOT NULL COMMENT '收货地址ID',
    total_amount DECIMAL(10,2) NOT NULL COMMENT '订单总金额',
    order_status TINYINT NOT NULL COMMENT '订单状态 1-待付款 2-待发货 3-待收货 4-已完成 5-已取消 6-支付异常',
    cancel_reason VARCHAR(100) COMMENT '取消原因（超时未付款/用户手动取消/商家取消）',
    pay_deadline DATETIME COMMENT '支付截止时间（创建后15分钟）',
    receiver VARCHAR(20) NOT NULL COMMENT '收件人',
    receiver_phone VARCHAR(11) NOT NULL COMMENT '收件手机号',
    receiver_address VARCHAR(255) NOT NULL COMMENT '收件地址（省市区乡镇+详细）',
    merchant_id BIGINT NOT NULL COMMENT '商家ID',
    delete_flag TINYINT DEFAULT 0 COMMENT '软删除 0-未删 1-已删',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_order_no (order_no),
    INDEX idx_user_id (user_id),
    INDEX idx_merchant_id (merchant_id),
    INDEX idx_order_status (order_status),
    INDEX idx_create_time (create_time)
) COMMENT = '订单主表（适配分表：按create_time分表）';

-- 订单详情表
CREATE TABLE order_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '订单详情ID',
    order_no VARCHAR(32) NOT NULL COMMENT '订单编号',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    product_name VARCHAR(100) NOT NULL COMMENT '商品名称',
    product_img VARCHAR(255) COMMENT '商品图片',
    product_price DECIMAL(10,2) NOT NULL COMMENT '商品单价',
    product_num INT NOT NULL COMMENT '购买数量',
    product_amount DECIMAL(10,2) NOT NULL COMMENT '商品小计金额',
    delete_flag TINYINT DEFAULT 0 COMMENT '软删除 0-未删 1-已删',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_order_no (order_no),
    INDEX idx_product_id (product_id)
) COMMENT = '订单详情表';

-- 支付记录表
CREATE TABLE payment_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '支付记录ID',
    order_no VARCHAR(32) NOT NULL COMMENT '订单编号',
    pay_type TINYINT COMMENT '支付方式 1-支付宝 2-微信支付',
    pay_amount DECIMAL(10,2) NOT NULL COMMENT '支付金额',
    pay_status TINYINT NOT NULL COMMENT '支付状态 0-待支付 1-支付成功 2-支付失败 3-支付异常',
    pay_time DATETIME COMMENT '支付时间',
    transaction_id VARCHAR(64) COMMENT '第三方支付流水号',
    refund_status TINYINT DEFAULT 0 COMMENT '退款状态 0-未退款 1-退款中 2-退款成功 3-退款失败',
    refund_amount DECIMAL(10,2) DEFAULT 0 COMMENT '退款金额',
    refund_time DATETIME COMMENT '退款时间',
    delete_flag TINYINT DEFAULT 0 COMMENT '软删除 0-未删 1-已删',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_order_no (order_no),
    INDEX idx_pay_status (pay_status)
) COMMENT = '支付记录表（打款台账核心表）';

-- 物流信息表
CREATE TABLE logistics_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '物流ID',
    order_no VARCHAR(32) NOT NULL COMMENT '订单编号',
    logistics_company VARCHAR(50) COMMENT '快递公司',
    logistics_no VARCHAR(50) COMMENT '物流单号',
    logistics_status TINYINT DEFAULT 0 COMMENT '物流状态 0-未发货 1-已发货 2-运输中 3-已签收 4-异常',
    abnormal_reason VARCHAR(100) COMMENT '物流异常原因',
    delete_flag TINYINT DEFAULT 0 COMMENT '软删除 0-未删 1-已删',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_order_no (order_no),
    INDEX idx_logistics_no (logistics_no)
) COMMENT = '物流信息表';

-- 评价表
CREATE TABLE comment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '评价ID',
    order_no VARCHAR(32) NOT NULL COMMENT '订单编号',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    score TINYINT NOT NULL COMMENT '评分（1-5分）',
    content TEXT COMMENT '评价内容',
    img_urls VARCHAR(500) COMMENT '评价图片/视频URL（逗号分隔）',
    audit_status TINYINT DEFAULT 1 COMMENT '审核状态 0-待审核 1-已通过 2-已驳回',
    delete_flag TINYINT DEFAULT 0 COMMENT '软删除 0-未删 1-已删',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_product_id (product_id),
    INDEX idx_user_id (user_id),
    INDEX idx_order_no (order_no),
    INDEX idx_audit_status (audit_status)
) COMMENT = '商品评价表';

-- 售后表
CREATE TABLE after_sale (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '售后ID',
    after_sale_no VARCHAR(32) NOT NULL COMMENT '售后单号（唯一）',
    order_no VARCHAR(32) NOT NULL COMMENT '订单编号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    merchant_id BIGINT NOT NULL COMMENT '商家ID',
    after_sale_type TINYINT NOT NULL COMMENT '售后类型 1-退款 2-退货 3-换货',
    apply_reason VARCHAR(50) NOT NULL COMMENT '申请原因（变质/少发/规格不符等）',
    proof_img_urls VARCHAR(500) COMMENT '凭证图片URL（逗号分隔）',
    after_sale_status TINYINT NOT NULL COMMENT '售后状态 1-待商家处理 2-协商中 3-已解决 4-管理员介入 5-已驳回',
    handle_result VARCHAR(200) COMMENT '处理结果',
    admin_id BIGINT COMMENT '介入管理员ID',
    delete_flag TINYINT DEFAULT 0 COMMENT '软删除 0-未删 1-已删',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_after_sale_no (after_sale_no),
    INDEX idx_order_no (order_no),
    INDEX idx_merchant_id (merchant_id),
    INDEX idx_after_sale_status (after_sale_status)
) COMMENT = '售后表';

-- 售后沟通表
CREATE TABLE after_sale_communication (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '沟通ID',
    after_sale_no VARCHAR(32) NOT NULL COMMENT '售后单号',
    sender_type TINYINT NOT NULL COMMENT '发送方类型 1-用户 2-商家 3-管理员',
    sender_id BIGINT NOT NULL COMMENT '发送方ID',
    content TEXT NOT NULL COMMENT '沟通内容',
    is_read TINYINT DEFAULT 0 COMMENT '是否已读 0-未读 1-已读',
    delete_flag TINYINT DEFAULT 0 COMMENT '软删除 0-未删 1-已删',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_after_sale_no (after_sale_no),
    INDEX idx_sender_id (sender_id)
) COMMENT = '售后沟通表';

-- 新闻分类表
CREATE TABLE news_category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '分类ID',
    category_name VARCHAR(30) NOT NULL COMMENT '分类名称（农业政策/扶贫案例/产地动态）',
    category_code VARCHAR(30) NOT NULL COMMENT '分类编码',
    delete_flag TINYINT DEFAULT 0 COMMENT '软删除 0-未删 1-已删',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_category_code (category_code)
) COMMENT = '助农新闻分类表';

-- 助农新闻表
CREATE TABLE news (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '新闻ID',
    title VARCHAR(100) NOT NULL COMMENT '新闻标题',
    category_id BIGINT NOT NULL COMMENT '分类ID',
    content TEXT NOT NULL COMMENT '新闻内容（MD格式）',
    cover_img VARCHAR(255) COMMENT '封面图片',
    view_count INT DEFAULT 0 COMMENT '浏览量',
    audit_status TINYINT DEFAULT 1 COMMENT '审核状态 0-待审核 1-已通过 2-已驳回',
    create_by BIGINT COMMENT '创建人（管理员ID）',
    delete_flag TINYINT DEFAULT 0 COMMENT '软删除 0-未删 1-已删',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_category_id (category_id),
    INDEX idx_audit_status (audit_status)
) COMMENT = '助农新闻表';
```

##### 3. 商家端核心模块
```sql
-- 商家信息表
CREATE TABLE merchant_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '商家ID',
    phone VARCHAR(11) NOT NULL COMMENT '手机号（登录账号）',
    merchant_name VARCHAR(50) NOT NULL COMMENT '农户/合作社名称',
    contact_person VARCHAR(20) NOT NULL COMMENT '联系人',
    contact_phone VARCHAR(11) NOT NULL COMMENT '联系电话',
    password VARCHAR(100) COMMENT '密码（可选）',
    audit_status TINYINT DEFAULT 0 COMMENT '审核状态 0-待审核 1-已通过 2-已驳回',
    reject_reason VARCHAR(200) COMMENT '驳回原因',
    status TINYINT DEFAULT 0 COMMENT '店铺状态 0-未开业 1-正常营业 2-禁用',
    login_token VARCHAR(100) COMMENT '登录态token',
    token_expire_time DATETIME COMMENT 'token过期时间',
    delete_flag TINYINT DEFAULT 0 COMMENT '软删除 0-未删 1-已删',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_phone (phone),
    INDEX idx_audit_status (audit_status),
    INDEX idx_status (status)
) COMMENT = '商家信息表';

-- 店铺信息表
CREATE TABLE shop_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '店铺ID',
    merchant_id BIGINT NOT NULL COMMENT '商家ID',
    shop_name VARCHAR(50) NOT NULL COMMENT '店铺名称',
    shop_intro VARCHAR(500) COMMENT '店铺简介',
    qualification_img VARCHAR(500) COMMENT '资质图片URL（逗号分隔）',
    delete_flag TINYINT DEFAULT 0 COMMENT '软删除 0-未删 1-已删',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_merchant_id (merchant_id)
) COMMENT = '店铺信息表';

-- 商家收款账户表
CREATE TABLE merchant_account (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '账户ID',
    merchant_id BIGINT NOT NULL COMMENT '商家ID',
    account_type TINYINT NOT NULL COMMENT '账户类型 1-支付宝 2-微信 3-银行卡',
    account_no VARCHAR(50) NOT NULL COMMENT '账户号（支付宝/微信账号/银行卡号）',
    account_name VARCHAR(50) NOT NULL COMMENT '账户名（真实姓名）',
    verify_status TINYINT DEFAULT 0 COMMENT '验证状态 0-未验证 1-已验证（1分钱验证）',
    audit_status TINYINT DEFAULT 0 COMMENT '修改审核状态 0-待审核 1-已通过 2-已驳回',
    delete_flag TINYINT DEFAULT 0 COMMENT '软删除 0-未删 1-已删',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_merchant_account (merchant_id, account_type),
    INDEX idx_verify_status (verify_status)
) COMMENT = '商家收款账户表';

-- 商品品类表
CREATE TABLE product_category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '品类ID',
    category_name VARCHAR(30) NOT NULL COMMENT '品类名称（生鲜/干货/粮油等）',
    parent_id BIGINT DEFAULT 0 COMMENT '父品类ID（一级品类为0）',
    category_level TINYINT NOT NULL COMMENT '品类层级（1/2/3）',
    delete_flag TINYINT DEFAULT 0 COMMENT '软删除 0-未删 1-已删',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_parent_id (parent_id)
) COMMENT = '商品品类表';

-- 商品信息表
CREATE TABLE product_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '商品ID',
    product_name VARCHAR(100) NOT NULL COMMENT '商品名称',
    merchant_id BIGINT NOT NULL COMMENT '商家ID',
    category_id BIGINT NOT NULL COMMENT '品类ID',
    price DECIMAL(10,2) NOT NULL COMMENT '商品价格',
    stock INT NOT NULL COMMENT '库存数量（Redis缓存核心字段）',
    stock_warning INT DEFAULT 10 COMMENT '库存预警值',
    product_img VARCHAR(500) COMMENT '商品图片URL（逗号分隔，多图轮播）',
    product_desc TEXT COMMENT '商品详细介绍',
    origin_place VARCHAR(50) COMMENT '产地（四川市县乡镇）',
    status TINYINT DEFAULT 0 COMMENT '状态 0-待审核 1-已上架 2-已下架 3-已驳回',
    reject_reason VARCHAR(200) COMMENT '驳回原因',
    is_unsalable TINYINT DEFAULT 0 COMMENT '是否滞销专区 0-否 1-是',
    sales_volume INT DEFAULT 0 COMMENT '销量（排序依据）',
    score DECIMAL(2,1) DEFAULT 5.0 COMMENT '商品评分（评价汇总）',
    delete_flag TINYINT DEFAULT 0 COMMENT '软删除 0-未删 1-已删',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_merchant_id (merchant_id),
    INDEX idx_category_id (category_id),
    INDEX idx_status (status),
    INDEX idx_is_unsalable (is_unsalable),
    INDEX idx_sales_volume (sales_volume),
    INDEX idx_score (score)
) COMMENT = '商品信息表（适配分表：按merchant_id分表）';

-- 商品溯源信息表
CREATE TABLE product_trace (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '溯源ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    planting_cycle VARCHAR(50) COMMENT '种植周期',
    origin_place_detail VARCHAR(100) COMMENT '详细产地',
    fertilizer_type VARCHAR(50) COMMENT '施肥类型',
    storage_method VARCHAR(50) COMMENT '保存方式',
    transport_method VARCHAR(50) COMMENT '运输方式',
    qr_code_url VARCHAR(255) COMMENT '溯源二维码URL',
    delete_flag TINYINT DEFAULT 0 COMMENT '软删除 0-未删 1-已删',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_product_id (product_id)
) COMMENT = '商品溯源信息表（可对接区块链存储）';

-- 对账明细表
CREATE TABLE reconciliation_detail (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '对账ID',
    merchant_id BIGINT NOT NULL COMMENT '商家ID',
    order_no VARCHAR(32) COMMENT '订单编号',
    order_amount DECIMAL(10,2) NOT NULL COMMENT '订单金额',
    actual_income DECIMAL(10,2) NOT NULL COMMENT '商家实际营收',
    service_fee DECIMAL(10,2) DEFAULT 0 COMMENT '平台服务费',
    payment_time DATETIME COMMENT '用户支付时间',
    transfer_status TINYINT DEFAULT 0 COMMENT '打款状态 0-待打款 1-打款成功 2-打款失败',
    transfer_time DATETIME COMMENT '打款时间',
    transfer_no VARCHAR(64) COMMENT '打款流水号',
    retry_count TINYINT DEFAULT 0 COMMENT '重试次数（最多3次）',
    delete_flag TINYINT DEFAULT 0 COMMENT '软删除 0-未删 1-已删',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_merchant_id (merchant_id),
    INDEX idx_order_no (order_no),
    INDEX idx_transfer_status (transfer_status)
) COMMENT = '商家对账明细表（打款台账核心）';

-- 补贴明细表
CREATE TABLE subsidy_detail (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '补贴ID',
    merchant_id BIGINT NOT NULL COMMENT '商家ID',
    order_no VARCHAR(32) COMMENT '订单编号',
    subsidy_type VARCHAR(30) NOT NULL COMMENT '补贴类型（物流补贴）',
    subsidy_amount DECIMAL(10,2) NOT NULL COMMENT '补贴金额',
    audit_status TINYINT DEFAULT 0 COMMENT '审核状态 0-待审核 1-已通过 2-已驳回',
    grant_status TINYINT DEFAULT 0 COMMENT '发放状态 0-未发放 1-已发放 2-发放失败',
    grant_time DATETIME COMMENT '发放时间',
    reject_reason VARCHAR(200) COMMENT '驳回原因',
    delete_flag TINYINT DEFAULT 0 COMMENT '软删除 0-未删 1-已删',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_merchant_id (merchant_id),
    INDEX idx_order_no (order_no),
    INDEX idx_audit_status (audit_status),
    INDEX idx_grant_status (grant_status)
) COMMENT = '补贴明细表';

-- 审核记录表（通用：商家/商品/滞销专区/补贴）
CREATE TABLE audit_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '审核记录ID',
    business_type VARCHAR(30) NOT NULL COMMENT '业务类型（merchant/ product/ unsalable/ subsidy）',
    business_id BIGINT NOT NULL COMMENT '业务ID（商家ID/商品ID等）',
    audit_user_id BIGINT COMMENT '审核人ID（管理员）',
    audit_status TINYINT NOT NULL COMMENT '审核状态 1-通过 2-驳回',
    audit_reason VARCHAR(200) COMMENT '审核意见/驳回原因',
    delete_flag TINYINT DEFAULT 0 COMMENT '软删除 0-未删 1-已删',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '审核时间',
    INDEX idx_business_type_id (business_type, business_id),
    INDEX idx_audit_user_id (audit_user_id)
) COMMENT = '通用审核记录表';
```

#### Redis缓存设计（适配性能优化）
结合你分阶段优化的需求，Redis主要缓存以下高频访问数据：
```
# 1. 用户登录态（7天有效期）
key: user:token:{token}
value: {user_id: 123, phone: "13800138000", expire_time: "2026-02-13 12:00:00"}
类型: Hash

# 2. 商品库存（实时同步，避免超卖）
key: product:stock:{product_id}
value: 100
类型: String

# 3. 订单支付倒计时（15分钟过期）
key: order:pay:timeout:{order_no}
value: {order_id: 456, user_id: 123, deadline: "2026-02-06 14:15:00"}
类型: Hash
过期时间: 15分钟

# 4. 热销商品排行（首页展示）
key: product:hot:rank
value: [product_id1, product_id2, ...]
类型: ZSet（score为销量）
过期时间: 1小时（定时更新）
```

### 三、总结
1. **核心表设计**：覆盖用户、商家、管理员三大端的全业务链路，核心表包括`user_info`（用户）、`merchant_info`（商家）、`product_info`（商品）、`order_main`（订单）、`after_sale`（售后）、`reconciliation_detail`（对账）等，满足“商品交易-溯源-售后-对账-补贴”全流程需求；
2. **扩展性适配**：
   - 主键用`bigint`支持分布式雪花算法，`order_main`/`product_info`预留分表字段（按时间/商家ID），适配中期ShardingSphere分库分表；
   - `product_trace`独立设计，可无缝对接区块链溯源（仅需扩展存储字段）；
   - 所有表添加`ext_json`预留字段（可根据实际需求补充），支持后期功能扩展；
3. **性能优化**：关键查询字段（订单状态、商家ID、商品ID）添加索引，Redis缓存登录态、库存、订单倒计时等高频数据，适配农村用户移动端轻量化访问需求。

你可根据实际开发进度，先实现核心表（用户、商品、订单、支付），再逐步扩展售后、补贴、审核等表，分阶段落地优化需求。



# 任务1：通用功能开发（1天）
### 一、需求理解
你需要开发一套后端通用功能组件，作为助农电商平台的基础支撑，包括统一返回结果、全局异常处理、JWT认证、Redis缓存、验证码生成、二维码生成、定时任务配置，这些组件需具备通用性，后续业务模块可直接复用，且要求1天内完成开发。

### 二、通用功能实现
#### 1. 前置依赖（pom.xml）
首先添加核心依赖，确保工具类能正常运行：
```xml
<!-- SpringBoot核心 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<!-- JWT -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.11.5</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.11.5</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.11.5</version>
    <scope>runtime</scope>
</dependency>
<!-- 二维码生成 ZXing -->
<dependency>
    <groupId>com.google.zxing</groupId>
    <artifactId>core</artifactId>
    <version>3.5.2</version>
</dependency>
<dependency>
    <groupId>com.google.zxing</groupId>
    <artifactId>javase</artifactId>
    <version>3.5.2</version>
</dependency>
<!-- 简化开发 -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>
<!-- 工具类 -->
<dependency>
    <groupId>cn.hutool</groupId>
    <artifactId>hutool-all</artifactId>
    <version>5.8.22</version>
</dependency>
```

#### 2. 全局返回结果类（Result.java）
统一接口返回格式，避免重复封装：
```java
package com.zhnong.helper.common;

import lombok.Data;

/**
 * 全局统一返回结果
 * @param <T> 数据类型
 */
@Data
public class Result<T> {
    /**
     * 响应码：200成功，500失败，401未登录，403无权限
     */
    private Integer code;
    /**
     * 响应消息
     */
    private String message;
    /**
     * 响应数据
     */
    private T data;

    // 私有化构造器，禁止外部直接创建
    private Result() {}

    // 成功响应（无数据）
    public static <T> Result<T> success() {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("操作成功");
        return result;
    }

    // 成功响应（带数据）
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("操作成功");
        result.setData(data);
        return result;
    }

    // 失败响应（自定义消息）
    public static <T> Result<T> error(String message) {
        Result<T> result = new Result<>();
        result.setCode(500);
        result.setMessage(message);
        return result;
    }

    // 失败响应（自定义码+消息）
    public static <T> Result<T> error(Integer code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }
}
```

#### 3. 自定义业务异常类（BusinessException.java）
用于区分业务异常和系统异常：
```java
package com.zhnong.helper.common.exception;

import lombok.Getter;

/**
 * 自定义业务异常
 */
@Getter
public class BusinessException extends RuntimeException {
    /**
     * 异常码
     */
    private Integer code;

    // 构造器：仅消息
    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }

    // 构造器：码+消息
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}
```

#### 4. 全局异常处理器（GlobalExceptionHandler.java）
统一捕获异常，返回友好提示：
```java
package com.zhnong.helper.common.handler;

import com.zhnong.helper.common.Result;
import com.zhnong.helper.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice // 全局捕获@RestController的异常
public class GlobalExceptionHandler {

    /**
     * 捕获业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e) {
        log.error("业务异常：{}", e.getMessage(), e);
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 捕获系统异常（所有未处理的异常）
     */
    @ExceptionHandler(Exception.class)
    public Result<?> handleSystemException(Exception e) {
        log.error("系统异常：{}", e.getMessage(), e);
        return Result.error("系统繁忙，请稍后再试");
    }
}
```

#### 5. JWT工具类（JwtUtil.java）
实现JWT生成、解析、验证，用于登录态校验：
```java
package com.zhnong.helper.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;

/**
 * JWT工具类
 */
@Component
public class JwtUtil {
    /**
     * JWT密钥（建议配置在application.yml，长度至少32位）
     */
    @Value("${jwt.secret:zhnong助农电商平台2026专用密钥12345678}")
    private String secret;

    /**
     * 过期时间（秒），默认7天
     */
    @Value("${jwt.expire:604800}")
    private Long expire;

    /**
     * 生成JWT令牌
     * @param claims 自定义载荷（如userId、role）
     * @return token
     */
    public String generateToken(Map<String, Object> claims) {
        // 生成密钥
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());
        // 构建token
        return Jwts.builder()
                .setClaims(claims) // 自定义载荷
                .setIssuedAt(new Date()) // 签发时间
                .setExpiration(new Date(System.currentTimeMillis() + expire * 1000)) // 过期时间
                .signWith(key) // 签名
                .compact();
    }

    /**
     * 解析token，获取所有载荷
     * @param token 令牌
     * @return 载荷
     */
    public Claims parseToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 验证token是否有效（未过期）
     * @param token 令牌
     * @return true-有效，false-无效
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = parseToken(token);
            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 从token中获取指定字段
     * @param token 令牌
     * @param field 字段名
     * @return 字段值
     */
    public Object getClaim(String token, String field) {
        Claims claims = parseToken(token);
        return claims.get(field);
    }
}
```

#### 6. Redis工具类（RedisUtil.java）
封装Redis常用操作，支持泛型，适配登录态、验证码、库存等场景：
```java
package com.zhnong.helper.common.util;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * Redis工具类
 */
@Component
public class RedisUtil {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 设置缓存（无过期时间）
     * @param key 键
     * @param value 值
     */
    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 设置缓存（带过期时间）
     * @param key 键
     * @param value 值
     * @param expire 过期时间
     * @param timeUnit 时间单位
     */
    public void set(String key, Object value, Long expire, TimeUnit timeUnit) {
        redisTemplate.opsForValue().set(key, value, expire, timeUnit);
    }

    /**
     * 获取缓存
     * @param key 键
     * @param <T> 泛型
     * @return 值
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) redisTemplate.opsForValue().get(key);
    }

    /**
     * 删除缓存
     * @param key 键
     * @return true-删除成功，false-失败
     */
    public Boolean delete(String key) {
        return redisTemplate.delete(key);
    }

    /**
     * 设置过期时间
     * @param key 键
     * @param expire 过期时间
     * @param timeUnit 时间单位
     * @return true-成功，false-失败
     */
    public Boolean expire(String key, Long expire, TimeUnit timeUnit) {
        return redisTemplate.expire(key, expire, timeUnit);
    }

    /**
     * 判断key是否存在
     * @param key 键
     * @return true-存在，false-不存在
     */
    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }
}
```

#### 7. 验证码工具类（CaptchaUtil.java）
生成6位数字验证码，存入Redis（5分钟过期）：
```java
package com.zhnong.helper.common.util;

import cn.hutool.core.util.RandomUtil;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * 验证码工具类
 */
@Component
public class CaptchaUtil {

    @Resource
    private RedisUtil redisUtil;

    /**
     * 验证码缓存前缀
     */
    private static final String CAPTCHA_PREFIX = "captcha:";

    /**
     * 验证码过期时间（分钟）
     */
    private static final Long CAPTCHA_EXPIRE = 5L;

    /**
     * 生成6位数字验证码并存入Redis
     * @param phone 手机号（作为key）
     * @return 6位验证码
     */
    public String generateCaptcha(String phone) {
        // 生成6位随机数字
        String captcha = RandomUtil.randomNumbers(6);
        // 存入Redis，5分钟过期
        redisUtil.set(CAPTCHA_PREFIX + phone, captcha, CAPTCHA_EXPIRE, TimeUnit.MINUTES);
        return captcha;
    }

    /**
     * 验证验证码是否正确
     * @param phone 手机号
     * @param inputCaptcha 用户输入的验证码
     * @return true-正确，false-错误/过期
     */
    public boolean validateCaptcha(String phone, String inputCaptcha) {
        String key = CAPTCHA_PREFIX + phone;
        // 校验key是否存在
        if (!redisUtil.hasKey(key)) {
            return false;
        }
        // 获取缓存的验证码
        String cacheCaptcha = redisUtil.get(key);
        // 验证通过后删除验证码（防止重复使用）
        if (inputCaptcha.equals(cacheCaptcha)) {
            redisUtil.delete(key);
            return true;
        }
        return false;
    }
}
```

#### 8. 二维码生成工具类（QrCodeUtil.java）
基于ZXing生成商品溯源二维码，保存到本地并返回访问地址：
```java
package com.zhnong.helper.common.util;

import com.google.zxing.*;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.client.j2se.MatrixToImageConfig;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * 二维码生成工具类（基于ZXing）
 */
@Slf4j
@Component
public class QrCodeUtil {

    /**
     * 二维码保存根路径（项目static目录）
     */
    @Value("${qrcode.save.path:./src/main/resources/static/qrcode/}")
    private String savePath;

    /**
     * 二维码访问前缀（前端访问路径）
     */
    @Value("${qrcode.access.prefix:/qrcode/}")
    private String accessPrefix;

    /**
     * 二维码宽度
     */
    private static final int WIDTH = 300;

    /**
     * 二维码高度
     */
    private static final int HEIGHT = 300;

    /**
     * 生成二维码
     * @param content 二维码内容（溯源信息JSON/链接）
     * @return 二维码访问地址
     */
    public String generateQrCode(String content) {
        try {
            // 1. 创建保存目录（不存在则创建）
            File dir = new File(savePath);
            if (!dir.exists()) {
                boolean mkdirs = dir.mkdirs();
                if (!mkdirs) {
                    log.error("创建二维码目录失败：{}", savePath);
                    throw new RuntimeException("生成二维码失败");
                }
            }

            // 2. 生成唯一文件名
            String fileName = UUID.randomUUID() + ".png";
            Path path = Paths.get(savePath, fileName);

            // 3. 生成二维码
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(
                    content,
                    BarcodeFormat.QR_CODE,
                    WIDTH,
                    HEIGHT
            );

            // 4. 写入文件（黑色码点，白色背景）
            MatrixToImageWriter.writeToPath(
                    bitMatrix,
                    "PNG",
                    path,
                    new MatrixToImageConfig(0xFF000000, 0xFFFFFFFF)
            );

            // 5. 返回访问地址
            return accessPrefix + fileName;
        } catch (Exception e) {
            log.error("生成二维码异常：{}", e.getMessage(), e);
            throw new RuntimeException("生成二维码失败");
        }
    }
}
```

#### 9. 定时任务配置（SchedulingConfig.java）
开启SpringBoot定时任务，支持订单超时取消、自动打款等功能：
```java
package com.zhnong.helper.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 定时任务配置
 */
@Configuration
@EnableScheduling // 开启定时任务
public class SchedulingConfig {
    // 无需额外配置，业务定时任务直接在方法上添加@Scheduled注解即可
}
```

#### 10. 配置文件补充（application.yml）
添加工具类所需的核心配置：
```yaml
# 服务器配置
server:
  port: 8080
  servlet:
    context-path: /api

# Redis配置
spring:
  redis:
    host: localhost
    port: 6379
    password: # 无密码则留空
    database: 0
  # 静态资源映射（二维码访问）
  web:
    resources:
      static-locations: classpath:/static/

# JWT配置
jwt:
  secret: zhnong助农电商平台2026专用密钥12345678 # 建议替换为自定义密钥（≥32位）
  expire: 604800 # 7天，单位秒

# 二维码配置
qrcode:
  save:
    path: ./src/main/resources/static/qrcode/ # 二维码保存路径
  access:
    prefix: /qrcode/ # 前端访问前缀
```

### 三、使用示例（快速验证）
#### 1. 验证码使用示例
```java
// 生成验证码
String captcha = captchaUtil.generateCaptcha("13800138000");
System.out.println("生成的验证码：" + captcha);

// 验证验证码
boolean validate = captchaUtil.validateCaptcha("13800138000", "123456");
System.out.println("验证码验证结果：" + validate);
```

#### 2. JWT使用示例
```java
// 生成token
Map<String, Object> claims = new HashMap<>();
claims.put("userId", 1001L);
claims.put("role", "user");
String token = jwtUtil.generateToken(claims);
System.out.println("生成的Token：" + token);

// 验证token
boolean valid = jwtUtil.validateToken(token);
System.out.println("Token是否有效：" + valid);

// 获取载荷中的userId
Long userId = (Long) jwtUtil.getClaim(token, "userId");
System.out.println("解析的userId：" + userId);
```

#### 3. 二维码使用示例
```java
// 生成溯源二维码（内容为溯源信息JSON）
String traceContent = "{\"goodsId\":1001,\"growCycle\":\"120天\",\"origin\":\"四川成都\",\"fertilizer\":\"有机肥\"}";
String qrCodeUrl = qrCodeUtil.generateQrCode(traceContent);
System.out.println("二维码访问地址：" + qrCodeUrl);
```

#### 4. 统一返回结果使用示例
```java
// 控制器中返回结果
@RestController
@RequestMapping("/test")
public class TestController {
    @GetMapping("/success")
    public Result<String> success() {
        return Result.success("测试成功");
    }

    @GetMapping("/error")
    public Result<?> error() {
        throw new BusinessException("测试业务异常");
    }
}
```

### 四、总结
1. **核心组件覆盖**：完成了全局返回结果、异常处理、JWT、Redis、验证码、二维码、定时任务7个通用组件，覆盖登录态、缓存、验证码、溯源、定时任务等核心通用场景；
2. **复用性强**：所有工具类均基于SpringBoot注解开发，配置可自定义，后续业务模块直接注入即可使用；
3. **适配业务场景**：验证码默认5分钟过期、JWT默认7天有效期、二维码保存到静态目录，完全匹配助农电商平台的业务需求。

### 五、部署验证要点
1. 确保本地Redis已启动，且application.yml中的Redis配置正确；
2. 生成的二维码会保存到`src/main/resources/static/qrcode`目录，前端可通过`http://localhost:8080/api/qrcode/xxx.png`访问；
3. 定时任务只需在业务方法上添加`@Scheduled`注解即可生效（如订单超时取消）。