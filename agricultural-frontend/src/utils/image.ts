/**
 * 图片URL处理工具函数
 */

const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'

/**
 * 获取完整的图片URL
 * @param url 图片路径（可能是相对路径或绝对路径）
 * @returns 完整的图片URL
 */
export function getFullImageUrl(url: string | undefined | null): string {
  if (!url) return ''
  
  // 如果已经是完整URL，直接返回
  if (url.startsWith('http://') || url.startsWith('https://')) {
    return url
  }
  
  // 如果路径以 /api 开头，去掉 /api 前缀
  const cleanPath = url.startsWith('/api') ? url.slice(4) : url
  
  // 拼接基础URL
  const base = BASE_URL.endsWith('/api') ? BASE_URL.slice(0, -4) : BASE_URL
  return `${base}/api${cleanPath.startsWith('/') ? cleanPath : '/' + cleanPath}`
}

/**
 * 获取图片预览列表的完整URL数组
 * @param urls 图片路径数组
 * @returns 完整的图片URL数组
 */
export function getFullImageUrlList(urls: (string | undefined | null)[]): string[] {
  return urls.filter(Boolean).map(url => getFullImageUrl(url as string))
}
