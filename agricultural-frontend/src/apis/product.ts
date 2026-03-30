import request from '@/utils/request';
import type { Product, PageResult, ProductCategory, ProductComment, ShopInfo, TraceArchive, UnsalableProduct } from '@/types';

// 商品相关接口（与后端路径统一）
export const searchProducts = (params: any) => request.get<any, PageResult<Product>>('/user/products/search', { params });
export const getUnsalableProductsPublic = (params: any) =>
  request.get<any, PageResult<UnsalableProduct>>('/user/unsalable/products', { params });
export const getProductDetail = (id: number) => request.get<any, Product>(`/user/products/${id}`);
export const hotProducts = (params: any) => request.get<any, Product[]>('/user/products/hot', { params });
export const listCategories = () => request.get<any, ProductCategory[]>('/user/categories');
export const listOrigins = () => request.get<any, string[]>('/user/origins');

export const getMerchantShop = (id: number) => request.get<any, ShopInfo>(`/user/merchants/${id}`);
export const getMerchantProductsPublic = (id: number, params: any) =>
  request.get<any, PageResult<Product>>(`/user/merchants/${id}/products`, { params });
export const getMerchantComments = (id: number, params: any) =>
  request.get<any, PageResult<ProductComment>>(`/user/merchants/${id}/comments`, { params });
export const getProductComments = (id: number, params: any) =>
  request.get<any, PageResult<ProductComment>>(`/user/products/${id}/comments`, { params });
export const getTraceArchive = (traceCode: string) =>
  request.get<any, TraceArchive>(`/user/traces/${traceCode}`);

export const getMerchantProducts = (params: any) => request.get<any, PageResult<Product>>('/merchant/products', { params });
export const addProduct = (data: any) => request.post('/merchant/products', data);
export const updateProductStatus = (id: number, status: number) => request.put(`/merchant/products/${id}/status`, { status });
export const updateProduct = (id: number, data: any) => request.put(`/merchant/products/${id}`, data);
export const deleteProduct = (id: number) => request.delete(`/merchant/products/${id}`);

