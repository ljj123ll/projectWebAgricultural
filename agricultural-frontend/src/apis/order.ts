import request from '@/utils/request';
import type { CartItem, Order, PageResult, LogisticsInfo } from '@/types';

/**
 * 用户端订单与购物车接口。
 * 答辩时如果老师问“前端是怎么调用下单、支付、取消订单接口的”，直接从这里往后查。
 */

// 购物车接口：商品详情、购物车页、确认订单页都会复用这里的方法。
export const getCart = () => request.get<any, CartItem[]>('/user/cart');
export const addToCart = (data: { productId: number; productNum: number }) => request.post('/user/cart', data);
export const updateCartItem = (id: number, data: any) => request.put(`/user/cart/${id}`, data);
export const deleteCartItem = (id: number) => request.delete(`/user/cart/${id}`);
export const clearCart = () => request.delete('/user/cart');

// 订单接口：覆盖创建订单、订单列表、支付、取消、确认收货和物流查询。
export const createOrder = (data: any) => request.post('/user/orders', data);
export const getOrders = (params: any) => request.get<any, PageResult<Order>>('/user/orders', { params });
export const getOrderDetail = (id: number) => request.get<any, Order>(`/user/orders/${id}`);
export const cancelOrder = (id: number) => request.put(`/user/orders/${id}/cancel`);
export function payOrder(id: number, success: boolean = true) {
  return request({
    url: `/user/orders/${id}/pay`,
    method: 'post',
    params: { success }
  });
}
export const receiveOrder = (id: number) => request.put(`/user/orders/${id}/receive`);
export const getLogistics = (id: number) => request.get<any, LogisticsInfo>(`/user/orders/${id}/logistics`);
export const getLogisticsByOrderNo = (orderNo: string) =>
  request.get<any, LogisticsInfo>(`/common/logistics/${orderNo}`);

export const getMerchantOrders = (params: any) => request.get<any, PageResult<Order>>('/merchant/orders', { params });
export const getMerchantOrderDetail = (id: number) => request.get<any, Order>(`/merchant/orders/${id}`);
export const shipOrder = (id: number, logisticsInfo: any) => request.put(`/merchant/orders/${id}/ship`, logisticsInfo);
export const cancelMerchantOrder = (id: number, data: any) => request.put(`/merchant/orders/${id}/cancel`, data);
