-- 助农电商平台 - 数据库初始化脚本（与 数据库设计.md 一致）
-- 使用前请创建数据库: CREATE DATABASE nongnong_ecommerce DEFAULT CHARSET utf8mb4;

USE nongnong_ecommerce;

-- ========== 1. 系统基础模块 ==========
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '管理员ID',
    username VARCHAR(50) NOT NULL COMMENT '登录账号',
    password VARCHAR(100) NOT NULL COMMENT '加密密码（BCrypt）',
    phone VARCHAR(11) NOT NULL COMMENT '手机号',
    real_name VARCHAR(20) COMMENT '真实姓名',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    status TINYINT DEFAULT 1 COMMENT '状态 0-禁用 1-正常',
    last_login_time DATETIME COMMENT '最后登录时间',
    delete_flag TINYINT DEFAULT 0 COMMENT '软删除 0-未删 1-已删',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_role_id (role_id),
    INDEX idx_phone (phone)
) COMMENT = '管理员用户表';

CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '角色ID',
    role_name VARCHAR(30) NOT NULL COMMENT '角色名称',
    role_code VARCHAR(30) NOT NULL COMMENT '角色编码',
    description VARCHAR(200) COMMENT '角色描述',
    delete_flag TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_role_code (role_code)
) COMMENT = '角色表';

CREATE TABLE IF NOT EXISTS sys_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '权限ID',
    perm_name VARCHAR(50) NOT NULL,
    perm_code VARCHAR(50) NOT NULL,
    parent_id BIGINT DEFAULT 0,
    delete_flag TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_perm_code (perm_code)
) COMMENT = '权限表';

CREATE TABLE IF NOT EXISTS sys_role_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id BIGINT NOT NULL,
    perm_id BIGINT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_role_perm (role_id, perm_id),
    INDEX idx_role_id (role_id),
    INDEX idx_perm_id (perm_id)
) COMMENT = '角色权限关联表';

CREATE TABLE IF NOT EXISTS sys_operation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    operator_id BIGINT NOT NULL,
    operator_name VARCHAR(50) NOT NULL,
    operation_type VARCHAR(20) NOT NULL,
    business_type VARCHAR(50) NOT NULL,
    business_id BIGINT NOT NULL,
    operation_content TEXT,
    ip VARCHAR(32),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_operator_id (operator_id),
    INDEX idx_business_type_id (business_type, business_id),
    INDEX idx_create_time (create_time)
) COMMENT = '系统操作日志表';

-- ========== 2. 用户端核心模块 ==========
CREATE TABLE IF NOT EXISTS user_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    phone VARCHAR(11) NOT NULL,
    nickname VARCHAR(50),
    password VARCHAR(100),
    avatar_url VARCHAR(255),
    last_login_time DATETIME,
    login_token VARCHAR(100),
    token_expire_time DATETIME,
    status TINYINT DEFAULT 1,
    delete_flag TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_phone (phone),
    INDEX idx_login_token (login_token)
) COMMENT = '消费者用户表';

CREATE TABLE IF NOT EXISTS user_address (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    receiver VARCHAR(20) NOT NULL,
    phone VARCHAR(11) NOT NULL,
    province VARCHAR(20) NOT NULL,
    city VARCHAR(20) NOT NULL,
    county VARCHAR(20) NOT NULL,
    town VARCHAR(20) NOT NULL,
    detail_address VARCHAR(200) NOT NULL,
    is_default TINYINT DEFAULT 0,
    delete_flag TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_user_default (user_id, is_default)
) COMMENT = '用户收货地址表';

CREATE TABLE IF NOT EXISTS cart (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_num INT NOT NULL,
    select_status TINYINT DEFAULT 1,
    delete_flag TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_product (user_id, product_id),
    INDEX idx_user_id (user_id)
) COMMENT = '购物车表';

CREATE TABLE IF NOT EXISTS order_main (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no VARCHAR(32) NOT NULL,
    user_id BIGINT NOT NULL,
    address_id BIGINT NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    order_status TINYINT NOT NULL COMMENT '1-待付款 2-待发货 3-待收货 4-已完成 5-已取消 6-支付异常',
    cancel_reason VARCHAR(100),
    pay_deadline DATETIME,
    receiver VARCHAR(20) NOT NULL,
    receiver_phone VARCHAR(11) NOT NULL,
    receiver_address VARCHAR(255) NOT NULL,
    merchant_id BIGINT NOT NULL,
    delete_flag TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_order_no (order_no),
    INDEX idx_user_id (user_id),
    INDEX idx_merchant_id (merchant_id),
    INDEX idx_order_status (order_status),
    INDEX idx_create_time (create_time)
) COMMENT = '订单主表';

CREATE TABLE IF NOT EXISTS order_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no VARCHAR(32) NOT NULL,
    product_id BIGINT NOT NULL,
    product_name VARCHAR(100) NOT NULL,
    product_img VARCHAR(255),
    product_price DECIMAL(10,2) NOT NULL,
    product_num INT NOT NULL,
    product_amount DECIMAL(10,2) NOT NULL,
    delete_flag TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_order_no (order_no),
    INDEX idx_product_id (product_id)
) COMMENT = '订单详情表';

CREATE TABLE IF NOT EXISTS payment_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no VARCHAR(32) NOT NULL,
    pay_type TINYINT,
    pay_amount DECIMAL(10,2) NOT NULL,
    pay_status TINYINT NOT NULL,
    pay_time DATETIME,
    transaction_id VARCHAR(64),
    refund_status TINYINT DEFAULT 0,
    refund_amount DECIMAL(10,2) DEFAULT 0,
    refund_time DATETIME,
    delete_flag TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_order_no (order_no),
    INDEX idx_pay_status (pay_status)
) COMMENT = '支付记录表';

CREATE TABLE IF NOT EXISTS logistics_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no VARCHAR(32) NOT NULL,
    logistics_company VARCHAR(50),
    logistics_no VARCHAR(50),
    logistics_status TINYINT DEFAULT 0,
    abnormal_reason VARCHAR(100),
    delete_flag TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_order_no (order_no),
    INDEX idx_logistics_no (logistics_no)
) COMMENT = '物流信息表';

CREATE TABLE IF NOT EXISTS comment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no VARCHAR(32) NOT NULL,
    product_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    score TINYINT NOT NULL,
    content TEXT,
    img_urls VARCHAR(500),
    audit_status TINYINT DEFAULT 1,
    delete_flag TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_product_id (product_id),
    INDEX idx_user_id (user_id),
    INDEX idx_order_no (order_no),
    INDEX idx_audit_status (audit_status)
) COMMENT = '商品评价表';

CREATE TABLE IF NOT EXISTS after_sale (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    after_sale_no VARCHAR(32) NOT NULL,
    order_no VARCHAR(32) NOT NULL,
    user_id BIGINT NOT NULL,
    merchant_id BIGINT NOT NULL,
    after_sale_type TINYINT NOT NULL,
    apply_reason VARCHAR(50) NOT NULL,
    proof_img_urls VARCHAR(500),
    after_sale_status TINYINT NOT NULL,
    handle_result VARCHAR(200),
    admin_id BIGINT,
    delete_flag TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_after_sale_no (after_sale_no),
    INDEX idx_order_no (order_no),
    INDEX idx_merchant_id (merchant_id),
    INDEX idx_after_sale_status (after_sale_status)
) COMMENT = '售后表';

CREATE TABLE IF NOT EXISTS after_sale_communication (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    after_sale_no VARCHAR(32) NOT NULL,
    sender_type TINYINT NOT NULL,
    sender_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    is_read TINYINT DEFAULT 0,
    delete_flag TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_after_sale_no (after_sale_no),
    INDEX idx_sender_id (sender_id)
) COMMENT = '售后沟通表';

CREATE TABLE IF NOT EXISTS news_category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    category_name VARCHAR(30) NOT NULL,
    category_code VARCHAR(30) NOT NULL,
    delete_flag TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_category_code (category_code)
) COMMENT = '助农新闻分类表';

CREATE TABLE IF NOT EXISTS news (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(100) NOT NULL,
    category_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    cover_img VARCHAR(255),
    view_count INT DEFAULT 0,
    audit_status TINYINT DEFAULT 1,
    create_by BIGINT,
    delete_flag TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_category_id (category_id),
    INDEX idx_audit_status (audit_status)
) COMMENT = '助农新闻表';

-- ========== 3. 商家端核心模块 ==========
CREATE TABLE IF NOT EXISTS merchant_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    phone VARCHAR(11) NOT NULL,
    merchant_name VARCHAR(50) NOT NULL,
    contact_person VARCHAR(20) NOT NULL,
    contact_phone VARCHAR(11) NOT NULL,
    password VARCHAR(100),
    audit_status TINYINT DEFAULT 0,
    reject_reason VARCHAR(200),
    status TINYINT DEFAULT 0,
    login_token VARCHAR(100),
    token_expire_time DATETIME,
    delete_flag TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_phone (phone),
    INDEX idx_audit_status (audit_status),
    INDEX idx_status (status)
) COMMENT = '商家信息表';

CREATE TABLE IF NOT EXISTS shop_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    merchant_id BIGINT NOT NULL,
    shop_name VARCHAR(50) NOT NULL,
    shop_intro VARCHAR(500),
    qualification_img VARCHAR(500),
    delete_flag TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_merchant_id (merchant_id)
) COMMENT = '店铺信息表';

CREATE TABLE IF NOT EXISTS merchant_account (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    merchant_id BIGINT NOT NULL,
    account_type TINYINT NOT NULL,
    account_no VARCHAR(50) NOT NULL,
    account_name VARCHAR(50) NOT NULL,
    verify_status TINYINT DEFAULT 0,
    audit_status TINYINT DEFAULT 0,
    delete_flag TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_merchant_account (merchant_id, account_type),
    INDEX idx_verify_status (verify_status)
) COMMENT = '商家收款账户表';

CREATE TABLE IF NOT EXISTS product_category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    category_name VARCHAR(30) NOT NULL,
    parent_id BIGINT DEFAULT 0,
    category_level TINYINT NOT NULL,
    delete_flag TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_parent_id (parent_id)
) COMMENT = '商品品类表';

CREATE TABLE IF NOT EXISTS product_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_name VARCHAR(100) NOT NULL,
    merchant_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    stock INT NOT NULL,
    stock_warning INT DEFAULT 10,
    product_img VARCHAR(500),
    product_desc TEXT,
    origin_place VARCHAR(50),
    status TINYINT DEFAULT 0 COMMENT '0-待审核 1-已上架 2-已下架 3-已驳回',
    reject_reason VARCHAR(200),
    is_unsalable TINYINT DEFAULT 0,
    sales_volume INT DEFAULT 0,
    score DECIMAL(2,1) DEFAULT 5.0,
    delete_flag TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_merchant_id (merchant_id),
    INDEX idx_category_id (category_id),
    INDEX idx_status (status),
    INDEX idx_is_unsalable (is_unsalable),
    INDEX idx_sales_volume (sales_volume),
    INDEX idx_score (score)
) COMMENT = '商品信息表';

CREATE TABLE IF NOT EXISTS product_trace (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    planting_cycle VARCHAR(50),
    origin_place_detail VARCHAR(100),
    fertilizer_type VARCHAR(50),
    storage_method VARCHAR(50),
    transport_method VARCHAR(50),
    qr_code_url VARCHAR(255),
    delete_flag TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_product_id (product_id)
) COMMENT = '商品溯源信息表';

CREATE TABLE IF NOT EXISTS reconciliation_detail (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    merchant_id BIGINT NOT NULL,
    order_no VARCHAR(32),
    order_amount DECIMAL(10,2) NOT NULL,
    actual_income DECIMAL(10,2) NOT NULL,
    service_fee DECIMAL(10,2) DEFAULT 0,
    payment_time DATETIME,
    transfer_status TINYINT DEFAULT 0,
    transfer_time DATETIME,
    transfer_no VARCHAR(64),
    retry_count TINYINT DEFAULT 0,
    delete_flag TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_merchant_id (merchant_id),
    INDEX idx_order_no (order_no),
    INDEX idx_transfer_status (transfer_status)
) COMMENT = '商家对账明细表';

CREATE TABLE IF NOT EXISTS subsidy_detail (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    merchant_id BIGINT NOT NULL,
    order_no VARCHAR(32),
    subsidy_type VARCHAR(30) NOT NULL,
    subsidy_amount DECIMAL(10,2) NOT NULL,
    audit_status TINYINT DEFAULT 0,
    grant_status TINYINT DEFAULT 0,
    grant_time DATETIME,
    reject_reason VARCHAR(200),
    delete_flag TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_merchant_id (merchant_id),
    INDEX idx_order_no (order_no),
    INDEX idx_audit_status (audit_status),
    INDEX idx_grant_status (grant_status)
) COMMENT = '补贴明细表';

CREATE TABLE IF NOT EXISTS audit_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    business_type VARCHAR(30) NOT NULL,
    business_id BIGINT NOT NULL,
    audit_user_id BIGINT,
    audit_status TINYINT NOT NULL,
    audit_reason VARCHAR(200),
    delete_flag TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_business_type_id (business_type, business_id),
    INDEX idx_audit_user_id (audit_user_id)
) COMMENT = '通用审核记录表';

-- ========== 初始数据 ==========
INSERT INTO sys_role (role_name, role_code, description) VALUES
('超级管理员', 'SUPER_ADMIN', '全功能权限'),
('内容管理员', 'CONTENT_ADMIN', '仅商家/商品/评论审核、新闻管理')
ON DUPLICATE KEY UPDATE role_name=VALUES(role_name);

-- 默认管理员 admin / admin123（BCrypt），需先执行本脚本后可用管理员登录接口
-- 密码为 admin123 的 BCrypt 哈希
INSERT INTO sys_user (username, password, phone, role_id, status) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '13800000000', 1, 1)
ON DUPLICATE KEY UPDATE username=username;

INSERT INTO news_category (category_name, category_code) VALUES
('农业政策', 'policy'),
('扶贫案例', 'poverty'),
('产地动态', 'origin')
ON DUPLICATE KEY UPDATE category_name=VALUES(category_name);

INSERT INTO product_category (category_name, parent_id, category_level) VALUES
('生鲜', 0, 1),
('干货', 0, 1),
('粮油', 0, 1)
ON DUPLICATE KEY UPDATE category_name=VALUES(category_name);
