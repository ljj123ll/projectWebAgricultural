import { defineStore } from 'pinia';
import { ref } from 'vue';
import type { Order, UserAddress } from '@/types';

export const OrderStatus = {
  PENDING_PAY: 1,
  PENDING_SHIP: 2,
  PENDING_RECEIVE: 3,
  COMPLETED: 4,
  CANCELLED: 5
} as const;

export const useOrderStore = defineStore('order', () => {
  // 当前订单
  const currentOrder = ref<Order | null>(null);
  
  // 选中的收货地址
  const selectedAddress = ref<UserAddress | null>(null);

  // 订单状态文本映射
  const statusTextMap: Record<number, string> = {
    [OrderStatus.PENDING_PAY]: '待付款',
    [OrderStatus.PENDING_SHIP]: '待发货',
    [OrderStatus.PENDING_RECEIVE]: '待收货',
    [OrderStatus.COMPLETED]: '已完成',
    [OrderStatus.CANCELLED]: '已取消'
  };

  // 订单状态标签类型映射
  const statusTypeMap: Record<number, '' | 'success' | 'warning' | 'info' | 'danger'> = {
    [OrderStatus.PENDING_PAY]: 'warning',
    [OrderStatus.PENDING_SHIP]: 'info',
    [OrderStatus.PENDING_RECEIVE]: 'success',
    [OrderStatus.COMPLETED]: 'info',
    [OrderStatus.CANCELLED]: 'danger'
  };

  // 设置当前订单
  function setCurrentOrder(order: Order) {
    currentOrder.value = order;
  }

  // 清除当前订单
  function clearCurrentOrder() {
    currentOrder.value = null;
  }

  // 设置选中的收货地址
  function setSelectedAddress(address: UserAddress) {
    selectedAddress.value = address;
  }

  // 获取订单状态文本
  function getStatusText(status: number): string {
    return statusTextMap[status] || '未知状态';
  }

  // 获取订单状态标签类型
  function getStatusType(status: number): '' | 'success' | 'warning' | 'info' | 'danger' {
    return statusTypeMap[status] || '';
  }

  return {
    currentOrder,
    selectedAddress,
    setCurrentOrder,
    clearCurrentOrder,
    setSelectedAddress,
    getStatusText,
    getStatusType
  };
});
