import { AFTER_SALE_STATUS, getAfterSaleResponsibleLabel, getAfterSaleStatusText } from '@/utils/afterSale';

export type MerchantMessageType = 'order' | 'aftersale';

export interface MerchantMessageItem {
  id: string;
  title: string;
  content: string;
  time: string;
  type: MerchantMessageType;
  isRead: boolean;
  link: string;
}

const READ_STORAGE_PREFIX = 'merchant_message_read_map';
const CACHE_STORAGE_PREFIX = 'merchant_message_cache';
const MAX_CACHE_SIZE = 300;

export const getMerchantReadMapKey = (merchantId?: number | string) =>
  `${READ_STORAGE_PREFIX}_${merchantId || 'anonymous'}`;

export const getMerchantMessageCacheKey = (merchantId?: number | string) =>
  `${CACHE_STORAGE_PREFIX}_${merchantId || 'anonymous'}`;

export const loadMerchantReadMap = (merchantId?: number | string) => {
  try {
    const raw = localStorage.getItem(getMerchantReadMapKey(merchantId));
    return raw ? JSON.parse(raw) as Record<string, boolean> : {};
  } catch (error) {
    console.warn('load merchant read map failed', error);
    return {};
  }
};

export const saveMerchantReadMap = (merchantId: number | string | undefined, readMap: Record<string, boolean>) => {
  localStorage.setItem(getMerchantReadMapKey(merchantId), JSON.stringify(readMap));
};

export const loadMerchantMessageCache = (merchantId?: number | string) => {
  try {
    const raw = localStorage.getItem(getMerchantMessageCacheKey(merchantId));
    return raw ? JSON.parse(raw) as MerchantMessageItem[] : [];
  } catch (error) {
    console.warn('load merchant message cache failed', error);
    return [];
  }
};

export const saveMerchantMessageCache = (merchantId: number | string | undefined, messages: MerchantMessageItem[]) => {
  localStorage.setItem(getMerchantMessageCacheKey(merchantId), JSON.stringify(messages.slice(0, MAX_CACHE_SIZE)));
};

export const mergeMerchantMessages = (
  merchantId: number | string | undefined,
  liveMessages: MerchantMessageItem[]
) => {
  const readMap = loadMerchantReadMap(merchantId);
  const cachedMessages = loadMerchantMessageCache(merchantId);
  const mergedMap = new Map<string, MerchantMessageItem>();

  cachedMessages.forEach(message => {
    mergedMap.set(message.id, {
      ...message,
      isRead: !!readMap[message.id] || !!message.isRead
    });
  });

  liveMessages.forEach(message => {
    const existing = mergedMap.get(message.id);
    mergedMap.set(message.id, {
      ...(existing || {}),
      ...message,
      isRead: !!readMap[message.id] || !!existing?.isRead
    });
  });

  const merged = [...mergedMap.values()]
    .sort((a, b) => new Date(b.time || 0).getTime() - new Date(a.time || 0).getTime())
    .slice(0, MAX_CACHE_SIZE);

  saveMerchantMessageCache(merchantId, merged);
  return merged;
};

export const markMerchantMessageRead = (
  merchantId: number | string | undefined,
  messageId: string,
  messages: MerchantMessageItem[]
) => {
  const readMap = loadMerchantReadMap(merchantId);
  readMap[messageId] = true;
  saveMerchantReadMap(merchantId, readMap);
  const updated = messages.map(message =>
    message.id === messageId ? { ...message, isRead: true } : message
  );
  saveMerchantMessageCache(merchantId, updated);
  return updated;
};

export const markAllMerchantMessagesRead = (
  merchantId: number | string | undefined,
  messages: MerchantMessageItem[]
) => {
  const readMap = loadMerchantReadMap(merchantId);
  messages.forEach(message => {
    readMap[message.id] = true;
  });
  saveMerchantReadMap(merchantId, readMap);
  const updated = messages.map(message => ({ ...message, isRead: true }));
  saveMerchantMessageCache(merchantId, updated);
  return updated;
};

export const buildAfterSaleMessageMeta = (status?: number) => {
  if (status === AFTER_SALE_STATUS.PENDING_MERCHANT) {
    return {
      title: '售后待处理',
      prefix: '该售后等待商家处理，请尽快审核。'
    };
  }
  if (status === AFTER_SALE_STATUS.WAIT_MERCHANT_REFUND) {
    return {
      title: '退货待签收退款',
      prefix: '用户已回传退货物流，请确认签收并退款。'
    };
  }
  if (status === AFTER_SALE_STATUS.WAIT_USER_RETURN) {
    return {
      title: '等待用户退货',
      prefix: '商家已同意退货退款，当前等待用户回寄商品。'
    };
  }
  if (status === AFTER_SALE_STATUS.ADMIN) {
    return {
      title: '管理员介入处理中',
      prefix: '该售后已进入平台裁决阶段。'
    };
  }
  return {
    title: `售后状态：${getAfterSaleStatusText(status)}`,
    prefix: getAfterSaleResponsibleLabel(status)
  };
};
