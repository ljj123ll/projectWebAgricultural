# 助农电商平台 - 前端项目

基于 Vue 3 + TypeScript + Vite + Element Plus 开发。

## 项目结构
- `src/api`: 接口请求封装
- `src/layout`: 页面布局（用户端、商家端、管理员端）
- `src/router`: 路由配置
- `src/store`: Pinia 状态管理
- `src/types`: TypeScript 类型定义
- `src/utils`: 工具函数（Axios 封装等）
- `src/views`: 页面视图

## 快速开始

1. 安装依赖
```bash
npm install
```

2. 启动开发服务器
```bash
npm run dev
```

3. 构建生产版本
```bash
npm run build
```

## 测试账号
- 管理员: admin / 123456
- 商家: merchant / 123456
- 用户: user / 123456

## 注意事项
- 后端接口代理配置在 `vite.config.ts` 中，默认指向 `http://localhost:8080`。
- 如果后端未启动，部分页面使用 Mock 数据展示。
