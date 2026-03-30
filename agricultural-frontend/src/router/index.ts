import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router';
import { useUserStore } from '@/stores/modules/user';
import { ElMessage } from 'element-plus';

/**
 * 前端总路由入口。
 * 答辩时如果老师问“某个功能页面从哪里进入”，优先从这里按角色查找：
 * 用户端 / 商家端 / 管理员端 的页面注册都集中在本文件。
 */
const routes: Array<RouteRecordRaw> = [
  // 落地页
  {
    path: '/',
    name: 'Landing',
    component: () => import('@/views/Landing.vue'),
    meta: { title: '欢迎访问' }
  },
  // 用户端路由
  {
    path: '/',
    component: () => import('@/layout/UserLayout.vue'),
    children: [
      {
        path: 'home',
        name: 'UserHome',
        component: () => import('@/views/user/Home.vue'),
        meta: { title: '首页' }
      },
      {
        path: 'products',
        name: 'ProductList',
        component: () => import('@/views/user/ProductList.vue'),
        meta: { title: '全部商品' }
      },
      {
        path: 'product/:id',
        name: 'ProductDetail',
        component: () => import('@/views/user/ProductDetail.vue'),
        meta: { title: '商品详情' }
      },
      {
        path: 'shop/:merchantId',
        name: 'UserMerchantShop',
        component: () => import('@/views/user/MerchantShop.vue'),
        meta: { title: '店铺详情' }
      },
      {
        path: 'news',
        name: 'News',
        component: () => import('@/views/user/News.vue'),
        meta: { title: '助农新闻' }
      },
      {
        path: 'news/:id',
        name: 'NewsDetail',
        component: () => import('@/views/user/NewsDetail.vue'),
        meta: { title: '新闻详情' }
      },
      {
        path: 'unsold',
        name: 'Unsold',
        component: () => import('@/views/user/Unsold.vue'),
        meta: { title: '滞销专区' }
      },
      {
        path: 'cart',
        name: 'Cart',
        component: () => import('@/views/user/Cart.vue'),
        meta: { title: '购物车', requiresAuth: true }
      },
      {
        path: 'orders',
        name: 'UserOrders',
        component: () => import('@/views/user/Orders.vue'),
        meta: { title: '我的订单', requiresAuth: true }
      },
      {
        path: 'order-confirm',
        name: 'OrderConfirm',
        component: () => import('@/views/user/OrderConfirm.vue'),
        meta: { title: '确认订单', requiresAuth: true }
      },
      {
        path: 'order-pay/:orderId',
        name: 'OrderPay',
        component: () => import('@/views/user/OrderPay.vue'),
        meta: { title: '订单支付', requiresAuth: true }
      },
      {
        path: 'order-detail/:orderId',
        name: 'OrderDetail',
        component: () => import('@/views/user/OrderDetail.vue'),
        meta: { title: '订单详情', requiresAuth: true }
      },
      {
        path: 'profile',
        name: 'UserProfile',
        component: () => import('@/views/user/Profile.vue'),
        meta: { title: '个人中心', requiresAuth: true }
      },
      {
        path: 'address',
        name: 'UserAddress',
        component: () => import('@/views/user/Address.vue'),
        meta: { title: '收货地址', requiresAuth: true }
      },
      {
        path: 'reviews',
        name: 'UserReviews',
        component: () => import('@/views/user/Reviews.vue'),
        meta: { title: '我的评价', requiresAuth: true }
      },
      {
        path: 'messages',
        name: 'UserMessages',
        component: () => import('@/views/user/Messages.vue'),
        meta: { title: '我的消息', requiresAuth: true }
      },
      {
        path: 'after-sale/:orderNo',
        name: 'AfterSaleApply',
        component: () => import('@/views/user/AfterSaleApply.vue'),
        meta: { title: '申请售后', requiresAuth: true }
      },
      {
        path: 'after-sale-detail/:afterSaleNo',
        name: 'AfterSaleDetail',
        component: () => import('@/views/user/AfterSaleDetail.vue'),
        meta: { title: '售后详情', requiresAuth: true }
      }
    ]
  },
  // 用户登录/注册
  {
    path: '/login',
    name: 'UserLogin',
    component: () => import('@/views/user/Login.vue'),
    meta: { title: '用户登录' }
  },
  {
    path: '/register',
    name: 'UserRegister',
    component: () => import('@/views/user/Register.vue'),
    meta: { title: '用户注册' }
  },
  {
    path: '/forgot-password',
    name: 'UserForgotPassword',
    component: () => import('@/views/user/ForgotPassword.vue'),
    meta: { title: '找回密码' }
  },
  {
    path: '/trace/:traceCode',
    name: 'TraceArchive',
    component: () => import('@/views/user/TraceArchive.vue'),
    meta: { title: '商品溯源档案' }
  },
  // 商家端路由
  {
    path: '/merchant/login',
    name: 'MerchantLogin',
    component: () => import('@/views/merchant/Login.vue'),
    meta: { title: '商家登录' }
  },
  {
    path: '/merchant/register',
    name: 'MerchantRegister',
    component: () => import('@/views/merchant/Register.vue'),
    meta: { title: '商家注册' }
  },
  {
    path: '/merchant',
    component: () => import('@/layout/MerchantLayout.vue'),
    meta: { requiresAuth: true, role: 'merchant' },
    children: [
      {
        path: 'dashboard',
        name: 'MerchantDashboard',
        component: () => import('@/views/merchant/Dashboard.vue'),
        meta: { title: '商家控制台' }
      },
      {
        path: 'shop',
        name: 'MerchantShop',
        component: () => import('@/views/merchant/Shop.vue'),
        meta: { title: '店铺信息' }
      },
      {
        path: 'products',
        name: 'MerchantProducts',
        component: () => import('@/views/merchant/Products.vue'),
        meta: { title: '商品管理' }
      },
      {
        path: 'product-add',
        name: 'MerchantProductAdd',
        component: () => import('@/views/merchant/ProductAdd.vue'),
        meta: { title: '新增商品' }
      },
      {
        path: 'product-edit/:id',
        name: 'MerchantProductEdit',
        component: () => import('@/views/merchant/ProductAdd.vue'),
        meta: { title: '编辑商品' }
      },
      {
        path: 'orders',
        name: 'MerchantOrders',
        component: () => import('@/views/merchant/Orders.vue'),
        meta: { title: '订单管理' }
      },
      {
        path: 'order-detail/:orderNo',
        name: 'MerchantOrderDetail',
        component: () => import('@/views/merchant/OrderDetail.vue'),
        meta: { title: '订单详情' }
      },
      {
        path: 'after-sales',
        name: 'MerchantAfterSales',
        component: () => import('@/views/merchant/AfterSales.vue'),
        meta: { title: '售后处理' }
      },
      {
        path: 'statistics',
        name: 'MerchantStatistics',
        component: () => import('@/views/merchant/Statistics.vue'),
        meta: { title: '数据统计' }
      },
      {
        path: 'messages',
        name: 'MerchantMessages',
        component: () => import('@/views/merchant/Messages.vue'),
        meta: { title: '消息中心' }
      },
      {
        path: 'account',
        name: 'MerchantAccount',
        component: () => import('@/views/merchant/Account.vue'),
        meta: { title: '对账与补贴' }
      }
    ]
  },
  // 管理员端路由
  {
    path: '/admin/login',
    name: 'AdminLogin',
    component: () => import('@/views/admin/Login.vue'),
    meta: { title: '管理员登录' }
  },
  {
    path: '/admin',
    component: () => import('@/layout/AdminLayout.vue'),
    meta: { requiresAuth: true, role: 'admin' },
    children: [
      {
        path: 'dashboard',
        name: 'AdminDashboard',
        component: () => import('@/views/admin/Dashboard.vue'),
        meta: { title: '数据看板' }
      },
      {
        path: 'users',
        name: 'AdminUsers',
        component: () => import('@/views/admin/Users.vue'),
        meta: { title: '用户管理' }
      },
      {
        path: 'merchants',
        name: 'AdminMerchants',
        component: () => import('@/views/admin/Merchants.vue'),
        meta: { title: '商家审核' }
      },
      {
        path: 'products',
        name: 'AdminProducts',
        component: () => import('@/views/admin/Products.vue'),
        meta: { title: '商品审核' }
      },
      {
        path: 'orders',
        name: 'AdminOrders',
        component: () => import('@/views/admin/Orders.vue'),
        meta: { title: '订单管理' }
      },
      {
        path: 'after-sales',
        name: 'AdminAfterSales',
        component: () => import('@/views/admin/AfterSales.vue'),
        meta: { title: '售后裁决' }
      },
      {
        path: 'payments',
        name: 'AdminPayments',
        component: () => import('@/views/admin/Payments.vue'),
        meta: { title: '资金管控' }
      },
      {
        path: 'risk-monitor',
        name: 'AdminRiskMonitor',
        component: () => import('@/views/admin/RiskMonitor.vue'),
        meta: { title: '风控监控' }
      },
      {
        path: 'transfers',
        name: 'AdminTransfers',
        component: () => import('@/views/admin/Transfers.vue'),
        meta: { title: '打款台账' }
      },
      {
        path: 'merchant-accounts',
        name: 'AdminMerchantAccounts',
        component: () => import('@/views/admin/MerchantAccounts.vue'),
        meta: { title: '收款账户审核' }
      },
      {
        path: 'withdrawals',
        name: 'AdminWithdrawals',
        component: () => import('@/views/admin/Withdrawals.vue'),
        meta: { title: '提现审核' }
      },
      {
        path: 'comments',
        name: 'AdminComments',
        component: () => import('@/views/admin/Comments.vue'),
        meta: { title: '评论管理' }
      },
      {
        path: 'news',
        name: 'AdminNews',
        component: () => import('@/views/admin/News.vue'),
        meta: { title: '新闻管理' }
      },
      {
        path: 'unsold',
        name: 'AdminUnsold',
        component: () => import('@/views/admin/Unsold.vue'),
        meta: { title: '滞销专区' }
      },
      {
        path: 'logs',
        name: 'AdminLogs',
        component: () => import('@/views/admin/Logs.vue'),
        meta: { title: '系统日志' }
      },
      {
        path: 'roles',
        name: 'AdminRoles',
        component: () => import('@/views/admin/Roles.vue'),
        meta: { title: '角色权限' }
      },
      {
        path: 'backup',
        name: 'AdminBackup',
        component: () => import('@/views/admin/Backup.vue'),
        meta: { title: '数据备份' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFound.vue')
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

// 全局前置守卫：统一处理登录校验、角色校验和页面标题。
router.beforeEach((to, _from, next) => {
  const userStore = useUserStore();
  const isAuthenticated = !!userStore.token;
  const userRole = userStore.role;
  const targetRole = to.meta.role as string | undefined;

  // 根据角色把未登录用户送到正确的登录页，避免三端入口混淆。
  const getLoginPathByRole = (role?: string) => {
    if (role === 'merchant') return '/merchant/login';
    if (role === 'admin') return '/admin/login';
    return '/login';
  };

  // 设置页面标题
  if (to.meta.title) {
    document.title = to.meta.title + ' - 助农电商平台';
  }

  // 需要登录的页面
  if (to.meta.requiresAuth && !isAuthenticated) {
    ElMessage.warning('请先登录');
    next({ path: getLoginPathByRole(targetRole), query: { redirect: to.fullPath } });
    return;
  }

  // 角色权限检查
  if (targetRole && targetRole !== userRole) {
    ElMessage.error('权限不足');
    next(getLoginPathByRole(targetRole));
    return;
  }

  next();
});

export default router;

