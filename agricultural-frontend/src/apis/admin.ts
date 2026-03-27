import request from '@/utils/request';
import type {
  PageResult,
  UserInfo,
  MerchantInfo,
  Comment,
  News,
  NewsCategory,
  Order,
  AfterSale,
  PaymentRecord,
  ReconciliationRecord,
  SysRole,
  SysPermission,
  AfterSaleMessage,
  UnsalableProduct,
  UnsalableSummary
} from '@/types';

export const adminLogin = (data: any) => request.post('/admin/login', data);
export const adminSendSms = (phone: string) => request.post('/admin/sms/send', { phone });
export const getCurrentAdminPermissions = () => request.get<any, string[]>('/admin/me/permissions');

// 管理员Dashboard
export const getDashboard = (timeRange = 'all') => request.get<any, any>('/admin/dashboard', { params: { timeRange } });
export const getMerchantRank = (limit = 10) => request.get<any, any[]>('/admin/merchants/rank', { params: { limit } });

export const listUsers = (params: any) => request.get<any, PageResult<UserInfo>>('/admin/users', { params });
export const createUser = (data: any) => request.post('/admin/users', data);
export const updateUserStatus = (id: number, status: number) => request.put(`/admin/users/${id}/status`, { status });
export const deleteUser = (id: number) => request.delete(`/admin/users/${id}`);

// 滞销专区
export const getUnsalableProducts = (params: any) => request.get<any, PageResult<UnsalableProduct>>('/admin/unsalable', { params });
export const getUnsalableSummary = (params?: any) => request.get<any, UnsalableSummary>('/admin/unsalable/summary', { params });
export const setUnsalable = (id: number, isUnsalable: number) => request.put(`/admin/unsalable/${id}`, null, { params: { isUnsalable } });

export const listMerchants = (params: any) => request.get<any, PageResult<MerchantInfo>>('/admin/merchants', { params });
export const updateMerchantStatus = (id: number, status: number) => request.put(`/admin/merchants/${id}/status`, { status });
export const listMerchantAudit = (params: any) => request.get<any, PageResult<MerchantInfo>>('/admin/merchants/audit', { params });
export const auditMerchant = (id: number, data: any) => request.put(`/admin/merchants/${id}/audit`, data);
export const getMerchantDetail = (id: number) => request.get<any, any>(`/admin/merchants/${id}`);
export const listProductAudit = (params: any) => request.get<any, PageResult<any>>('/admin/products/audit', { params });
export const auditProduct = (id: number, data: any) => request.put(`/admin/products/${id}/audit`, data);

export const listComments = (params: any) => request.get<any, PageResult<Comment>>('/admin/comments', { params });
export const auditComment = (id: number, auditStatus: number) => request.put(`/admin/comments/${id}/audit`, { auditStatus });
export const deleteComment = (id: number) => request.delete(`/admin/comments/${id}`);

export const listNews = (params: any) => request.get<any, PageResult<News>>('/admin/news', { params });
export const getNews = (id: number) => request.get<any, News>(`/admin/news/${id}`);
export const createNews = (data: any) => request.post('/admin/news', data);
export const updateNews = (id: number, data: any) => request.put(`/admin/news/${id}`, data);
export const deleteNews = (id: number) => request.delete(`/admin/news/${id}`);
export const listNewsCategories = () => request.get<any, NewsCategory[]>('/admin/news/categories');
export const createNewsCategory = (data: any) => request.post('/admin/news/categories', data);
export const updateNewsCategory = (id: number, data: any) => request.put(`/admin/news/categories/${id}`, data);
export const deleteNewsCategory = (id: number) => request.delete(`/admin/news/categories/${id}`);

export const listOrders = (params: any) => request.get<any, PageResult<Order>>('/admin/orders', { params });
export const getOrder = (id: number) => request.get<any, Order>(`/admin/orders/${id}`);
export const cancelOrder = (id: number, reason?: string) => request.put(`/admin/orders/${id}/cancel`, { reason });

export const listAfterSale = (params: any) => request.get<any, PageResult<AfterSale>>('/admin/after-sale', { params });
export const handleAfterSale = (id: number, data: any) => request.put(`/admin/after-sale/${id}/handle`, data);
export const listAdminAfterSaleMessages = (afterSaleNo: string, params: any) =>
  request.get<any, PageResult<AfterSaleMessage>>(`/admin/after-sale/${afterSaleNo}/messages`, { params });
export const sendAdminAfterSaleMessage = (afterSaleNo: string, data: { content: string }) =>
  request.post(`/admin/after-sale/${afterSaleNo}/messages`, data);

export const listPayments = (params: any) => request.get<any, PageResult<PaymentRecord>>('/admin/payments', { params });
export const updateRefund = (id: number, refundStatus: number) => request.put(`/admin/payments/${id}/refund`, { refundStatus });
export const listTransfers = (params: any) => request.get<any, PageResult<ReconciliationRecord>>('/admin/transfers', { params });
export const manualTransfer = (id: number) => request.post(`/admin/transfers/${id}/manual`);

export const listOperationLogs = (params: any) => request.get('/admin/operation-logs', { params });
export const listAuditRecords = (params: any) => request.get('/admin/audit-records', { params });
export const listRiskOrders = (params: any) => request.get('/admin/orders/risk-abnormal', { params });
export const listLogisticsAbnormal = (params: any) => request.get('/admin/orders/logistics-abnormal', { params });
export const listRoles = () => request.get<any, SysRole[]>('/admin/roles');
export const listPermissions = () => request.get<any, SysPermission[]>('/admin/permissions');
export const getRolePermissionIds = (id: number) => request.get<any, number[]>(`/admin/roles/${id}/permissions`);
export const updateRolePermissionIds = (id: number, permIds: number[]) =>
  request.put(`/admin/roles/${id}/permissions`, { permIds });
export const listMerchantAccounts = (params: any) => request.get<any, PageResult<any>>('/admin/merchant-accounts', { params });
export const auditMerchantAccount = (id: number, data: { approve: boolean; reason?: string }) =>
  request.put(`/admin/merchant-accounts/${id}/audit`, data);
export const listWithdrawals = (params: any) =>
  request.get<any, PageResult<any>>('/admin/withdrawals', { params });
export const auditWithdrawal = (id: number, data: { approve: boolean; remark?: string }) =>
  request.put(`/admin/withdrawals/${id}/audit`, data);
export const manualTransferWithdrawal = (id: number) =>
  request.post(`/admin/withdrawals/${id}/manual-transfer`);

// 数据备份
export const listBackups = () => request.get<any, any[]>('/admin/backup');
export const createBackup = () => request.post('/admin/backup');
export const restoreBackup = (id: number) => request.post(`/admin/backup/${id}/restore`);
export const deleteBackup = (id: number) => request.delete(`/admin/backup/${id}`);
