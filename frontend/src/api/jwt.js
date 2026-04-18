/**
 * JWT 载荷解析（不验证签名；与常见 SPA 行为一致，实际鉴权仍以服务端为准）。
 */

function base64UrlToJson(base64Url) {
  const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/')
  const padded = base64 + '='.repeat((4 - (base64.length % 4)) % 4)
  const binary = atob(padded)
  const bytes = Uint8Array.from(binary, (c) => c.charCodeAt(0))
  const decoded = new TextDecoder().decode(bytes)
  return JSON.parse(decoded)
}

export function decodeJwtPayload(token) {
  if (!token || typeof token !== 'string') return null
  const parts = token.split('.')
  if (parts.length !== 3) return null
  try {
    return base64UrlToJson(parts[1])
  } catch {
    return null
  }
}

/**
 * @param {string} token
 * @param {number} skewMs 时钟偏差容忍（默认 30s）
 * @returns {boolean} true：不可再用于客户端判断为“仍有效”（无 token、格式错误、或 exp 已过期）
 */
export function isAccessTokenExpired(token, skewMs = 30_000) {
  if (!token || typeof token !== 'string') return true
  const payload = decodeJwtPayload(token)
  if (!payload) return true
  if (typeof payload.exp !== 'number') return false
  const expMs = payload.exp * 1000
  return Date.now() >= expMs - skewMs
}
