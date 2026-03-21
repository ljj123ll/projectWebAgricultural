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
  plantingCycle?: string;
  originPlaceDetail?: string;
  fertilizerType?: string;
  storageMethod?: string;
  transportMethod?: string;
  qrCodeUrl?: string;
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
  orderStatus: number; // 1-待付款 2-待发货 3-待收货 4-已完成 5-已取消
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
  auditStatus?: number;
  createTime?: string;
}

export interface ProductComment {
  id: number;
  orderNo: string;
  productId: number;
  userId: number;
  nickname?: string;
  avatarUrl?: string;
  score: number;
  content?: string;
  imgUrls?: string;
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
}

export interface MerchantAccount {
  id: number;
  merchantId: number;
  accountType: number;
  accountNo: string;
  accountName: string;
  verifyStatus?: number;
  auditStatus?: number;
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
