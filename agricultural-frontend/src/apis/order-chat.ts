import request from '@/utils/request';
import type { OrderChatMessage, PageResult } from '@/types';

export const listUserOrderMessages = (orderNo: string, params?: any) =>
  request.get<any, PageResult<OrderChatMessage>>(`/user/orders/${orderNo}/messages`, { params });

export const listMerchantOrderMessages = (orderNo: string, params?: any) =>
  request.get<any, PageResult<OrderChatMessage>>(`/merchant/orders/${orderNo}/messages`, { params });

export const listAdminOrderMessages = (orderNo: string, params?: any) =>
  request.get<any, PageResult<OrderChatMessage>>(`/admin/orders/${orderNo}/messages`, { params });

