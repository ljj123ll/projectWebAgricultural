import request from '@/utils/request';
import type { CartItem, Order, PageResult, LogisticsInfo } from '@/types';

export const getCart = () => request.get<any, CartItem[]>('/user/cart');
export const addToCart = (data: { productId: number; productNum: number }) => request.post('/user/cart', data);
export const updateCartItem = (id: number, data: any) => request.put(`/user/cart/${id}`, data);
export const deleteCartItem = (id: number) => request.delete(`/user/cart/${id}`);
export const clearCart = () => request.delete('/user/cart');

export const createOrder = (data: any) => request.post('/user/orders', data);
export const getOrders = (params: any) => request.get<any, PageResult<Order>>('/user/orders', { params });
export const getOrderDetail = (id: number) => request.get<any, Order>(`/user/orders/${id}`);
export const cancelOrder = (id: number) => request.put(`/user/orders/${id}/cancel`);
export const payOrder = (id: number) => request.post(`/user/orders/${id}/pay`);
export const receiveOrder = (id: number) => request.put(`/user/orders/${id}/receive`);
export const getLogistics = (id: number) => request.get<any, LogisticsInfo>(`/user/orders/${id}/logistics`);

export const getMerchantOrders = (params: any) => request.get<any, PageResult<Order>>('/merchant/orders', { params });
export const getMerchantOrderDetail = (id: number) => request.get<any, Order>(`/merchant/orders/${id}`);
export const shipOrder = (id: number, logisticsInfo: any) => request.put(`/merchant/orders/${id}/ship`, logisticsInfo);
export const cancelMerchantOrder = (id: number, data: any) => request.put(`/merchant/orders/${id}/cancel`, data);
