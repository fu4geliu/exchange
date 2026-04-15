const jsonHeaders = {
  'Content-Type': 'application/json',
}

export async function request(url, options = {}) {
  const { redirectOn401 = false, ...fetchOptions } = options
  const token = localStorage.getItem('accessToken')
  const headers = {
    ...jsonHeaders,
    ...(fetchOptions.headers || {}),
  }

  if (token) {
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
    // 默认不清理登录态，避免调试时提交表单即丢现场。
    // 需要强制跳转时可在调用方显式传 redirectOn401: true。
    if (redirectOn401) {
      localStorage.removeItem('accessToken')
      localStorage.removeItem('currentUser')
      window.location.href = '/login'
    }
    const unauthorizedError = new Error(body?.message || 'UNAUTHORIZED')
    unauthorizedError.status = 401
    throw unauthorizedError
  }

  if (!response.ok) {
    const error = new Error(body?.message || `HTTP_${response.status}`)
    error.status = response.status
    throw error
  }

  return body
}
