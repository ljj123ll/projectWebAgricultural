import { defineStore } from 'pinia';
import { ref } from 'vue';
import type { UserInfo, LoginResult } from '@/types';

export const useUserStore = defineStore('user', () => {
  const token = ref<string>('');
  const userInfo = ref<UserInfo | null>(null);
  const role = ref<string>(''); // 'user', 'merchant', 'admin'
  const tokenExpiresAt = ref<number>(0);

  const readAuthFromStorage = (storage: Storage) => {
    const storedToken = storage.getItem('token') || '';
    const storedUserInfo = storage.getItem('userInfo');
    const storedRole = storage.getItem('role') || '';
    const storedExpiresAt = Number(storage.getItem('tokenExpiresAt') || '0');
    if (!storedToken) return null;
    if (storedExpiresAt && Date.now() > storedExpiresAt) {
      storage.removeItem('token');
      storage.removeItem('userInfo');
      storage.removeItem('role');
      storage.removeItem('tokenExpiresAt');
      return null;
    }
    return {
      token: storedToken,
      userInfo: storedUserInfo ? JSON.parse(storedUserInfo) : null,
      role: storedRole,
      tokenExpiresAt: storedExpiresAt
    };
  };

  // 当 session/local 同时存在时，优先 session（通常代表最近一次登录）
  const sessionState = readAuthFromStorage(sessionStorage);
  const localState = sessionState ? null : readAuthFromStorage(localStorage);

  if (sessionState) {
    token.value = sessionState.token;
    userInfo.value = sessionState.userInfo;
    role.value = sessionState.role;
    tokenExpiresAt.value = sessionState.tokenExpiresAt;
  } else if (localState) {
    token.value = localState.token;
    userInfo.value = localState.userInfo;
    role.value = localState.role;
    tokenExpiresAt.value = localState.tokenExpiresAt;
  }

  function setLoginState(data: LoginResult, options?: { expiresAt?: number; remember?: boolean }) {
    // 切换身份登录前先清空两端存储，避免残留旧角色导致路由守卫误判
    localStorage.removeItem('token');
    localStorage.removeItem('userInfo');
    localStorage.removeItem('role');
    localStorage.removeItem('tokenExpiresAt');
    sessionStorage.removeItem('token');
    sessionStorage.removeItem('userInfo');
    sessionStorage.removeItem('role');
    sessionStorage.removeItem('tokenExpiresAt');

    token.value = data.token;
    userInfo.value = data.userInfo;
    if (data.userInfo.role) {
      role.value = data.userInfo.role;
    }
    tokenExpiresAt.value = options?.expiresAt || 0;

    const storage = options?.remember ? localStorage : sessionStorage;
    storage.setItem('token', token.value);
    storage.setItem('userInfo', JSON.stringify(userInfo.value));
    storage.setItem('role', role.value);
    if (tokenExpiresAt.value) {
      storage.setItem('tokenExpiresAt', String(tokenExpiresAt.value));
    } else {
      storage.removeItem('tokenExpiresAt');
    }
  }

  function logout() {
    token.value = '';
    userInfo.value = null;
    role.value = '';
    tokenExpiresAt.value = 0;
    localStorage.removeItem('token');
    localStorage.removeItem('userInfo');
    localStorage.removeItem('role');
    localStorage.removeItem('tokenExpiresAt');
    sessionStorage.removeItem('token');
    sessionStorage.removeItem('userInfo');
    sessionStorage.removeItem('role');
    sessionStorage.removeItem('tokenExpiresAt');
  }

  return { token, userInfo, role, tokenExpiresAt, setLoginState, logout };
});
