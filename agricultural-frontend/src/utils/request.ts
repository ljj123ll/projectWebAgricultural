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
    const config = response.config;
    // 后端返回的code 200 表示成功
    if (code === 200) {
      return data; 
    } else {
      // Create a custom error object containing the response data
      const customError = new Error(msg || message || 'Error');
      (customError as any).response = { data: response.data };
      (customError as any).code = code;
      
      // 403 错误（审核状态等）由调用方处理，其他错误显示默认提示
      // 401错误对于某些接口（如获取用户信息）不显示错误，由调用方处理
      if (code !== 403 && code !== 401) {
        console.error('API Error:', config.url, response.data);
        ElMessage.error(msg || message || '系统错误');
      }
      return Promise.reject(customError);
    }
  },
  (error) => {
    const config = error.config;
    let message = error.message || '请求失败';
    
    // 对于某些接口，401错误不显示提示（如游客访问获取用户信息）
    const silentUrls = ['/user/info'];
    const isSilent = silentUrls.some(url => config?.url?.includes(url));
    
    // 记录详细的错误信息
    console.error('Request Error:', {
      url: config?.url,
      status: error.response?.status,
      statusText: error.response?.statusText,
      data: error.response?.data,
      message: error.message
    });
    
    if (error.response?.status === 401) {
      // 如果是静默接口，不显示错误消息
      if (isSilent) {
        return Promise.reject(error);
      }
      message = '登录已过期，请重新登录';
      const userStore = useUserStore();
      const currentRole = userStore.role || sessionStorage.getItem('role') || localStorage.getItem('role') || 'user';
      userStore.logout();
      const loginPath = currentRole === 'admin'
        ? '/admin/login'
        : currentRole === 'merchant'
          ? '/merchant/login'
          : '/login';
      window.location.href = loginPath;
    } else if (error.response?.data?.message) {
        message = error.response.data.message;
    }
    
    if (!isSilent) {
      ElMessage.error(message);
    }
    return Promise.reject(error);
  }
);

export default service;
