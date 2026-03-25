import request from '@/utils/request';
import type {
  PageResult,
  Order,
  Product,
  MerchantAuditStatus,
  MerchantAccount,
  AfterSale,
  AfterSaleMessage,
  MerchantAccountOverview,
  ReconciliationRecord,
  SubsidyRecord,
  MerchantWithdrawRecord
} from '@/types';

export const merchantLogin = (data: any) => request.post('/merchant/login', data);
export const merchantRegister = (data: any) => request.post('/merchant/register', data);
export const merchantSendSms = (phone: string) => request.post('/merchant/sms/send', { phone });
export const getMerchantAuditStatus = () => request.get<any, MerchantAuditStatus>('/merchant/audit/status');

export const getShopInfo = () => request.get<any, any>('/merchant/shop');
export const updateShopInfo = (data: any) => request.put('/merchant/shop', data);

// 工作台统计数据
export const getMerchantStats = () => request.get<any, any>('/merchant/stats');

// 统计页面数据
export const getMerchantStatistics = (timeRange: string = 'week') => 
  request.get<any, any>('/merchant/statistics', { params: { timeRange } });

export const listAccounts = () => request.get<any, MerchantAccount[]>('/merchant/accounts');
export const getMerchantAccountOverview = () =>
  request.get<any, MerchantAccountOverview>('/merchant/account/overview');
export const saveAccount = (data: any) => request.post('/merchant/accounts', data);
export const initAccountVerify = (id: number) =>
  request.post<any, { verifyAmount: number; message: string }>(`/merchant/accounts/${id}/verify/init`);
export const confirmAccountVerify = (id: number, amount: number) =>
  request.post(`/merchant/accounts/${id}/verify/confirm`, { amount });
export const submitAccountAudit = (id: number) =>
  request.post(`/merchant/accounts/${id}/submit-audit`);
export const applyWithdraw = (data: { amount: number; accountId?: number }) =>
  request.post<any, { withdrawNo: string }>('/merchant/withdrawals', data);
export const listWithdrawals = (params: any) =>
  request.get<any, PageResult<MerchantWithdrawRecord>>('/merchant/withdrawals', { params });
export const getWithdrawAvailable = () =>
  request.get<any, { availableAmount: number; frozenAmount: number; withdrawSuccessAmount: number }>('/merchant/withdrawals/available');
export const cancelWithdraw = (id: number) =>
  request.put(`/merchant/withdrawals/${id}/cancel`);

export const listProducts = (params: any) => request.get<any, PageResult<Product>>('/merchant/products', { params });
export const createProduct = (data: any) => request.post<any, { id?: number }>('/merchant/products', data);
export const getProductDetail = (id: number) => request.get<any, Product>(`/merchant/products/${id}`);
export const updateProduct = (id: number, data: any) => request.put(`/merchant/products/${id}`, data);
export const deleteProduct = (id: number) => request.delete(`/merchant/products/${id}`);
export const updateProductStatus = (id: number, status: number) => request.put(`/merchant/products/${id}/status`, { status });
// 生成/刷新溯源二维码（返回 data:image/png;base64...）
export const generateProductQrcode = (id: number) =>
  request.post<any, { qrCodeUrl?: string }>(`/merchant/products/${id}/qrcode`).then(res => res?.qrCodeUrl || '');

export const listOrders = (params: any) => request.get<any, PageResult<Order>>('/merchant/orders', { params });
export const getOrder = (id: number) => request.get<any, Order>(`/merchant/orders/${id}`);
export const getOrderByNo = (orderNo: string) => request.get<any, Order>(`/merchant/orders/no/${orderNo}`);
export const shipOrder = (id: number, data: any) => request.put(`/merchant/orders/${id}/ship`, data);
export const cancelOrder = (id: number, reason: string) => request.put(`/merchant/orders/${id}/cancel`, { reason });

export const listAfterSale = (params: any) => request.get<any, PageResult<AfterSale>>('/merchant/after-sale', { params });
export const handleAfterSale = (id: number, data: any) => request.put(`/merchant/after-sale/${id}/handle`, data);
export const confirmAfterSaleReturn = (id: number, data?: { handleResult?: string }) =>
  request.put(`/merchant/after-sale/${id}/confirm-return`, data || {});
export const listAfterSaleMessages = (no: string, params: any) =>
  request.get<any, PageResult<AfterSaleMessage>>(`/merchant/after-sale/${no}/messages`, { params });
export const sendAfterSaleMessage = (no: string, data: any) =>
  request.post(`/merchant/after-sale/${no}/messages`, data);

// 对账明细
export const listReconciliation = (params: any) => 
  request.get<any, PageResult<ReconciliationRecord>>('/merchant/reconciliation', { params });

// 补贴明细
export const listSubsidy = (params: any) => 
  request.get<any, PageResult<SubsidyRecord>>('/merchant/subsidy', { params });
