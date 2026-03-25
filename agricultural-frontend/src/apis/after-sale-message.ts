import request from '@/utils/request';

export const getAfterSaleUnreadCount = (afterSaleNo: string, senderType: number) =>
  request.get<any, number>(`/common/after-sale/${afterSaleNo}/messages/unread`, {
    params: { senderType }
  });

export const markAfterSaleMessagesRead = (afterSaleNo: string, senderType: number) =>
  request.put(`/common/after-sale/${afterSaleNo}/messages/read`, null, {
    params: { senderType }
  });
