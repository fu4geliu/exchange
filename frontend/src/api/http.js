import { isAccessTokenExpired } from './jwt'

const jsonHeaders = {
  'Content-Type': 'application/json',
}

export async function request(url, options = {}) {
  const { skipExpiryCheck = false, ...fetchOptions } = options
  const token = localStorage.getItem('accessToken')
  const headers = {
    ...jsonHeaders,
    ...(fetchOptions.headers || {}),
  }

  if (token) {
    if (!skipExpiryCheck && isAccessTokenExpired(token)) {
      const err = new Error('登录已过期，请重新登录')
      err.status = 401
      err.code = 'TOKEN_EXPIRED'
      throw err
    }
    headers.Authorization = `Bearer ${token}`
  }

  const response = await fetch(url, { ...fetchOptions, headers })

  let body = null
  try {
    body = await response.json()
  } catch {
    body = null
  }

  if (response.status === 401) {
    const unauthorizedError = new Error(body?.message || 'UNAUTHORIZED')
    unauthorizedError.status = 401
    unauthorizedError.code = 'HTTP_401'
    throw unauthorizedError
  }

  if (!response.ok) {
    const error = new Error(body?.message || `HTTP_${response.status}`)
    error.status = response.status
    throw error
  }

  return body
}
