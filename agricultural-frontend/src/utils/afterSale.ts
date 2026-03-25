export const AFTER_SALE_STATUS = {
  PENDING_MERCHANT: 1,
  WAIT_MERCHANT_REFUND: 2,
  RESOLVED: 3,
  ADMIN: 4,
  REJECTED: 5,
  WAIT_USER_RETURN: 6
} as const;

export type AfterSaleStatus = number | undefined | null;

export const getAfterSaleStatusText = (status: AfterSaleStatus) => {
  if (status === AFTER_SALE_STATUS.PENDING_MERCHANT) return '待商家处理';
  if (status === AFTER_SALE_STATUS.WAIT_MERCHANT_REFUND) return '待商家签收退款';
  if (status === AFTER_SALE_STATUS.RESOLVED) return '已完成售后';
  if (status === AFTER_SALE_STATUS.ADMIN) return '管理员介入中';
  if (status === AFTER_SALE_STATUS.REJECTED) return '已驳回';
  if (status === AFTER_SALE_STATUS.WAIT_USER_RETURN) return '待用户退货';
  return '未知状态';
};

export const getAfterSaleStatusTagType = (status: AfterSaleStatus) => {
  if (status === AFTER_SALE_STATUS.RESOLVED) return 'success';
  if (status === AFTER_SALE_STATUS.REJECTED) return 'danger';
  if (status === AFTER_SALE_STATUS.PENDING_MERCHANT) return 'warning';
  return 'info';
};

export const getAfterSaleResponsibleLabel = (status: AfterSaleStatus) => {
  if (status === AFTER_SALE_STATUS.PENDING_MERCHANT) return '当前等待商家处理';
  if (status === AFTER_SALE_STATUS.WAIT_USER_RETURN) return '当前等待您回寄商品';
  if (status === AFTER_SALE_STATUS.WAIT_MERCHANT_REFUND) return '当前等待商家签收并退款';
  if (status === AFTER_SALE_STATUS.ADMIN) return '当前等待管理员裁决';
  if (status === AFTER_SALE_STATUS.REJECTED) return '申请已驳回，可沟通后申请平台介入';
  if (status === AFTER_SALE_STATUS.RESOLVED) return '售后流程已结束';
  return '状态更新中';
};

export const canUserEscalate = (status: AfterSaleStatus) => {
  return status === AFTER_SALE_STATUS.PENDING_MERCHANT
    || status === AFTER_SALE_STATUS.WAIT_MERCHANT_REFUND
    || status === AFTER_SALE_STATUS.WAIT_USER_RETURN
    || status === AFTER_SALE_STATUS.REJECTED;
};

export const canAfterSaleTalk = (status: AfterSaleStatus) => {
  return status === AFTER_SALE_STATUS.PENDING_MERCHANT
    || status === AFTER_SALE_STATUS.WAIT_MERCHANT_REFUND
    || status === AFTER_SALE_STATUS.WAIT_USER_RETURN
    || status === AFTER_SALE_STATUS.ADMIN
    || status === AFTER_SALE_STATUS.REJECTED;
};

