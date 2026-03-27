export interface RealtimeRefreshPayload {
  reason?: string;
  refNo?: string;
  timestamp?: number;
}

export const USER_REALTIME_EVENT = 'user-realtime-refresh';
export const MERCHANT_REALTIME_EVENT = 'merchant-realtime-refresh';
export const ADMIN_REALTIME_EVENT = 'admin-realtime-refresh';

export const buildRealtimeStreamUrl = (role: 'user' | 'merchant' | 'admin', token: string) => {
  const basePath = (import.meta.env.VITE_API_BASE_URL || '/api').replace(/\/$/, '');
  return `${basePath}/${role}/realtime/stream?token=${encodeURIComponent(token)}`;
};

export const buildRealtimeWsUrl = (token: string) => {
  const apiBase = (import.meta.env.VITE_API_BASE_URL || '/api').replace(/\/$/, '');
  if (apiBase.startsWith('http://') || apiBase.startsWith('https://')) {
    return `${apiBase.replace(/^http/, 'ws')}/ws/realtime?token=${encodeURIComponent(token)}`;
  }
  const wsProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  return `${wsProtocol}//${window.location.host}${apiBase}/ws/realtime?token=${encodeURIComponent(token)}`;
};

export const parseRealtimePayload = (event: MessageEvent | Event): RealtimeRefreshPayload => {
  const source = 'data' in event ? event.data : (event as CustomEvent<RealtimeRefreshPayload>).detail;
  if (!source) return {};
  if (typeof source === 'object') {
    const typedSource = source as { type?: string; data?: RealtimeRefreshPayload };
    if (typedSource?.type === 'refresh' && typedSource.data && typeof typedSource.data === 'object') {
      return typedSource.data;
    }
    return source as RealtimeRefreshPayload;
  }
  try {
    const parsed = JSON.parse(String(source));
    if (!parsed || typeof parsed !== 'object') return {};
    if (parsed.type === 'refresh' && parsed.data && typeof parsed.data === 'object') {
      return parsed.data as RealtimeRefreshPayload;
    }
    return parsed as RealtimeRefreshPayload;
  } catch (error) {
    console.warn('parse realtime payload failed', error);
    return {};
  }
};

export const dispatchRealtimeRefresh = (eventName: string, payload: RealtimeRefreshPayload) => {
  window.dispatchEvent(new CustomEvent<RealtimeRefreshPayload>(eventName, { detail: payload }));
};
