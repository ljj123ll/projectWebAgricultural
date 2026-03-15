import axios, { type AxiosInstance, type AxiosResponse } from 'axios';
import { ElMessage } from 'element-plus';
import { useUserStore } from '@/stores/modules/user';

// 创建 axios 实例
const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 10000,
  headers: { 'Content-Type': 'application/json;charset=utf-8' }
});

// 请求拦截器
service.interceptors.request.use(
  (config) => {
    const userStore = useUserStore();
    if (userStore.token) {
      config.headers['Authorization'] = `Bearer ${userStore.token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// 响应拦截器
service.interceptors.response.use(
  (response: AxiosResponse) => {
    const { code, msg, message, data } = response.data;
    // 后端返回的code 200 表示成功
    if (code === 200) {
      return data; 
    } else {
      ElMessage.error(msg || message || '系统错误');
      return Promise.reject(new Error(msg || message || 'Error'));
    }
  },
  (error) => {
    let message = error.message || '请求失败';
    if (error.response?.status === 401) {
      message = '登录已过期，请重新登录';
      const userStore = useUserStore();
      userStore.logout();
      window.location.href = '/login';
    } else if (error.response?.data?.message) {
        message = error.response.data.message;
    }
    ElMessage.error(message);
    return Promise.reject(error);
  }
);

export default service;
