// 通用响应结构
export interface ApiResponse<T = any> {
  code: number;
  msg: string;
  data: T;
}

// 分页响应结构
export interface PageResult<T> {
  total: number;
  list: T[];
  pageNum?: number;
  pageSize?: number;
}

// 用户信息
export interface UserInfo {
  id: number;
  phone: string;
  nickname: string;
  avatarUrl?: string;
  role?: 'user' | 'merchant' | 'admin'; // 前端区分角色用
  roleCode?: string;
  permissionCodes?: string[];
}

// 登录响应
export interface LoginResult {
  token: string;
  userInfo: UserInfo;
}

// 商品信息
export interface Product {
  id: number;
  productName: string;
  merchantId: number;
  categoryId: number;
  price: number;
  stock: number;
  productImg: string;
  productDetailImg?: string;
  productDesc?: string;
  originPlace?: string;
  salesVolume?: number;
  score?: number;
  status?: number; // 0-待审核 1-已上架 2-已下架 3-已驳回
  merchantName?: string;
  categoryName?: string;
  traceCode?: string;
  batchNo?: string;
  productionDate?: string;
  harvestDate?: string;
  packagingDate?: string;
  inspectionReport?: string;
  plantingCycle?: string;
  originPlaceDetail?: string;
  fertilizerType?: string;
  storageMethod?: string;
  transportMethod?: string;
  qrCodeUrl?: string;
  traceExtra?: Record<string, string>;
}

export interface TraceField {
  key: string;
  label: string;
  value: string;
}

export interface TraceArchive {
  productId: number;
  traceCode: string;
  batchNo?: string;
  categoryId?: number;
  categoryName?: string;
  productName: string;
  productImg?: string;
  productDesc?: string;
  shopName?: string;
  originPlace?: string;
  originPlaceDetail?: string;
  plantingCycle?: string;
  fertilizerType?: string;
  storageMethod?: string;
  transportMethod?: string;
  inspectionReport?: string;
  productionDate?: string;
  harvestDate?: string;
  packagingDate?: string;
  baseFields?: TraceField[];
  featureFields?: TraceField[];
}

// 购物车项
export interface CartItem {
  id: number;
  productId: number;
  productName: string;
  productImg: string;
  price: number;
  productNum: number;
  stock: number;
  selectStatus: boolean;
}

// 订单信息
export interface Order {
  id: number;
  orderNo: string;
  totalAmount: number;
  orderStatus: number; // 1-待付款 2-待发货 3-待收货 4-已完成 5-已取消 6-支付异常 7-售后中 8-已完成售后
  createTime: string;
  updateTime?: string;
  orderItems: OrderItem[];
  items?: OrderItem[];
  cancelReason?: string;
  payDeadline?: string;
  payTime?: string;
  receiver?: string;
  receiverPhone?: string;
  receiverAddress?: string;
  remark?: string;
  logisticsCompany?: string;
  logisticsNo?: string;
  logisticsStatus?: number;
  abnormalReason?: string;
}

export interface OrderItem {
  id?: number;
  productId: number;
  productName: string;
  productImg: string;
  productPrice: number;
  productNum: number;
  productAmount?: number;
}

// 商家信息
export interface MerchantInfo {
  id: number;
  merchantName: string;
  contactPerson: string;
  phone: string;
  auditStatus: number; // 0-待审核 1-通过 2-驳回
}

export interface SysRole {
  id: number;
  roleName: string;
  roleCode: string;
  description?: string;
  createTime?: string;
  updateTime?: string;
}

export interface SysPermission {
  id: number;
  permName: string;
  permCode: string;
  parentId?: number;
  createTime?: string;
}

export interface NewsCategory {
  id: number;
  categoryName: string;
  categoryCode: string;
}

export interface News {
  id: number;
  title: string;
  categoryId: number;
  content: string;
  coverImg?: string;
  viewCount?: number;
  auditStatus?: number;
  createTime?: string;
}

export interface UserAddress {
  id: number;
  receiver: string;
  phone: string;
  province: string;
  city: string;
  county: string;
  town: string;
  detailAddress: string;
  isDefault: number;
}

export interface Comment {
  id: number;
  orderNo: string;
  productId: number;
  userId: number;
  score: number;
  content?: string;
  imgUrls?: string;
  mediaUrls?: string;
  auditStatus?: number;
  createTime?: string;
}

export interface ProductComment {
  id: number;
  orderNo: string;
  productId: number;
  productName?: string;
  productImg?: string;
  userId: number;
  nickname?: string;
  avatarUrl?: string;
  score: number;
  content?: string;
  imgUrls?: string;
  mediaUrls?: string;
  createTime?: string;
}

export interface AfterSale {
  id: number;
  afterSaleNo: string;
  orderNo: string;
  afterSaleType: number;
  applyReason: string;
  proofImgUrls?: string;
  afterSaleStatus: number;
  originOrderStatus?: number;
  returnLogisticsCompany?: string;
  returnLogisticsNo?: string;
  returnShipTime?: string;
  handleResult?: string;
  createTime?: string;
}

export interface AfterSaleMessage {
  id: number;
  afterSaleNo: string;
  senderType: number;
  senderId: number;
  content: string;
  isRead: number;
  createTime?: string;
}

export interface UserMessage {
  id: number;
  userId: number;
  senderType: number; // 1-系统 2-商家 3-管理员
  title?: string;
  content: string;
  refType?: string;
  refNo?: string;
  isRead: number; // 0-未读 1-已读
  createTime?: string;
}

export interface UnsalableProduct extends Product {
  manualIncluded?: boolean;
  algorithmIncluded?: boolean;
  inclusionSource?: 'manual' | 'algorithm' | 'manual_algorithm' | string;
  unsalableScore?: number;
  ageDays?: number;
  unsalableReason?: string;
}

export interface UnsalableSummary {
  totalCount: number;
  manualCount: number;
  algorithmCount: number;
  mixedCount: number;
}

export interface ProductCategory {
  id: number;
  categoryName: string;
  parentId: number;
  categoryLevel: number;
}

export interface ShopInfo {
  id: number;
  merchantId: number;
  shopName: string;
  shopIntro?: string;
  qualificationImg?: string;
  shopType?: string;
  categories?: string;
  shopAddress?: string;
  productCount?: number;
  commentCount?: number;
  reviewCount?: number;
  totalSalesVolume?: number;
  averageScore?: number;
}

export interface MerchantAccount {
  id: number;
  merchantId: number;
  accountType: number;
  accountNo: string;
  accountName: string;
  verifyStatus?: number;
  auditStatus?: number;
  verifyAmount?: number;
  verifyExpireTime?: string;
  verifiedTime?: string;
  auditSubmitTime?: string;
  rejectReason?: string;
}

export interface MerchantAuditStatus {
  auditStatus: number;
  rejectReason?: string;
  status?: number;
}

export interface MerchantAccountOverview {
  balance: number;
  totalIncome: number;
  totalServiceFee: number;
  todayIncome: number;
  weekIncome: number;
  monthIncome: number;
  pendingTransferCount: number;
  failedTransferCount: number;
  manualFallbackCount: number;
  subsidyTotal: number;
  subsidyMonth: number;
  subsidyOrderCount: number;
  subsidyPendingCount: number;
  subsidyRejectedCount: number;
  hasPassedReceivingAccount: boolean;
  approvedAccountType?: number;
  approvedAccountName?: string;
  approvedAccountNoMask?: string;
  withdrawAvailableAmount?: number;
  withdrawFrozenAmount?: number;
  withdrawSuccessAmount?: number;
}

export interface ReconciliationRecord {
  id: number;
  merchantId: number;
  orderNo?: string;
  orderAmount: number;
  actualIncome: number;
  serviceFee: number;
  paymentTime?: string;
  transferStatus?: number; // 0-待打款 1-已打款 2-失败待重试 3-人工兜底
  transferTime?: string;
  transferNo?: string;
  retryCount?: number;
  createTime?: string;
}

export interface SubsidyRecord {
  id: number;
  merchantId: number;
  orderNo?: string;
  subsidyType: string;
  subsidyAmount: number;
  auditStatus?: number; // 0-待审核 1-已通过 2-已驳回
  grantStatus?: number; // 0-待发放 1-已发放
  grantTime?: string;
  rejectReason?: string;
  createTime?: string;
}

export interface MerchantWithdrawRecord {
  id: number;
  withdrawNo: string;
  merchantId: number;
  accountId: number;
  accountType: number;
  accountNo: string;
  accountName: string;
  applyAmount: number;
  feeAmount: number;
  actualAmount: number;
  status: number; // 0-待审核 1-待打款 2-已驳回 3-打款成功 4-打款失败待重试 5-打款失败人工处理 6-已取消
  auditAdminId?: number;
  auditRemark?: string;
  auditTime?: string;
  transferNo?: string;
  transferTime?: string;
  retryCount?: number;
  failReason?: string;
  createTime?: string;
}

export interface RegionNode {
  value: string;
  label: string;
  children?: RegionNode[];
}

export interface LogisticsInfo {
  id: number;
  orderNo: string;
  logisticsCompany?: string;
  logisticsNo?: string;
  logisticsStatus?: number;
  abnormalReason?: string;
}

export interface PaymentRecord {
  id: number;
  orderNo: string;
  payType?: number;
  payAmount: number;
  payStatus: number;
  payTime?: string;
  transactionId?: string;
  refundStatus?: number;
  refundAmount?: number;
  refundTime?: string;
}

export interface OrderChatMessage {
  id: number;
  orderNo: string;
  senderType: number; // 1-用户 2-商家 3-管理员
  senderId: number;
  messageType?: number; // 1-文本 2-图片 3-视频
  content: string;
  mediaUrl?: string;
  mediaName?: string;
  createTime?: string;
}

