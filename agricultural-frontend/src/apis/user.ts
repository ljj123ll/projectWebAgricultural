import request from '@/utils/request';
import type { LoginResult, UserInfo, PageResult, UserAddress, Comment, AfterSale, AfterSaleMessage, News, NewsCategory } from '@/types';

// 用户认证接口
export const loginPassword = (data: any) => request.post<any, LoginResult>('/user/login/password', data);
export const loginSms = (data: any) => request.post<any, LoginResult>('/user/login/sms', data);
export const register = (data: any) => request.post<any, LoginResult>('/user/register', data);
export const resetPassword = (data: any) => request.put('/user/password/reset', data);
export const logout = () => request.post('/user/logout');
export const sendSms = (phone: string) => request.post('/user/sms/send', { phone });

export const getUserInfo = () => request.get<any, UserInfo>('/user/info');
export const updateUserInfo = (data: any) => request.put('/user/info', data);

export const getHomeData = () => request.get<any, any>('/user/home');

export const getAddresses = () => request.get<any, UserAddress[]>('/user/addresses');
export const addAddress = (data: any) => request.post('/user/addresses', data);
export const updateAddress = (id: number, data: any) => request.put(`/user/addresses/${id}`, data);
export const deleteAddress = (id: number) => request.delete(`/user/addresses/${id}`);
export const setDefaultAddress = (id: number) => request.put(`/user/addresses/${id}/default`);

export const listComments = (params: any) => request.get<any, PageResult<Comment>>('/user/comments', { params });
export const submitComment = (data: any) => request.post('/user/comments', data);

export const applyAfterSale = (data: any) => request.post('/user/after-sale', data);
export const listAfterSale = (params: any) => request.get<any, PageResult<AfterSale>>('/user/after-sale', { params });
export const escalateAfterSale = (id: number) => request.put(`/user/after-sale/${id}/escalate`);
export const listAfterSaleMessages = (no: string, params: any) =>
  request.get<any, PageResult<AfterSaleMessage>>(`/user/after-sale/${no}/messages`, { params });
export const sendAfterSaleMessage = (no: string, data: any) =>
  request.post(`/user/after-sale/${no}/messages`, data);

export const listNews = (params: any) => request.get<any, PageResult<News>>('/user/news', { params });
export const getNewsDetail = (id: number) => request.get<any, News>(`/user/news/${id}`);
export const listNewsCategories = () => request.get<any, NewsCategory[]>('/user/news/categories');
