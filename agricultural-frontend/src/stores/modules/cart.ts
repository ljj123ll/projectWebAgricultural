import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import type { CartItem } from '@/types';

export const useCartStore = defineStore('cart', () => {
  // 购物车列表
  const cartList = ref<CartItem[]>([]);

  // 购物车商品总数
  const totalCount = computed(() => {
    return cartList.value.reduce((sum, item) => sum + item.productNum, 0);
  });

  // 购物车选中商品总数
  const selectedCount = computed(() => {
    return cartList.value
      .filter(item => item.selectStatus)
      .reduce((sum, item) => sum + item.productNum, 0);
  });

  // 购物车选中商品总价
  const selectedAmount = computed(() => {
    return cartList.value
      .filter(item => item.selectStatus)
      .reduce((sum, item) => sum + item.price * item.productNum, 0);
  });

  // 是否全选
  const isAllSelected = computed(() => {
    return cartList.value.length > 0 && cartList.value.every(item => item.selectStatus);
  });

  // 设置购物车列表 (从后端同步)
  function setCartList(items: CartItem[]) {
    cartList.value = items.map(item => ({
      ...item,
      selectStatus: true // 默认选中
    }));
  }

  // 添加商品到购物车
  function addToCart(item: CartItem) {
    const existingItem = cartList.value.find(i => i.productId === item.productId);
    if (existingItem) {
      existingItem.productNum += item.productNum;
    } else {
      cartList.value.push({ ...item, selectStatus: true });
    }
  }

  // 更新商品数量
  function updateQuantity(productId: number, quantity: number) {
    const item = cartList.value.find(i => i.productId === productId);
    if (item) {
      item.productNum = quantity;
    }
  }

  // 删除商品
  function removeFromCart(productId: number) {
    const index = cartList.value.findIndex(i => i.productId === productId);
    if (index > -1) {
      cartList.value.splice(index, 1);
    }
  }

  // 清空购物车
  function clearCart() {
    cartList.value = [];
  }

  // 切换商品选中状态
  function toggleSelect(productId: number) {
    const item = cartList.value.find(i => i.productId === productId);
    if (item) {
      item.selectStatus = !item.selectStatus;
    }
  }

  // 全选/取消全选
  function toggleSelectAll(selected: boolean) {
    cartList.value.forEach(item => {
      item.selectStatus = selected;
    });
  }

  // 获取选中的商品（用于结算）
  function getSelectedItems(): CartItem[] {
    return cartList.value.filter(item => item.selectStatus);
  }

  // 清空已选中商品（支付成功后调用）
  function clearSelected() {
    cartList.value = cartList.value.filter(item => !item.selectStatus);
  }

  return {
    cartList,
    totalCount,
    selectedCount,
    selectedAmount,
    isAllSelected,
    setCartList, // Export setCartList
    addToCart,
    updateQuantity,
    removeFromCart,
    clearCart,
    toggleSelect,
    toggleSelectAll,
    getSelectedItems,
    clearSelected
  }
}, {
  persist: true
})
