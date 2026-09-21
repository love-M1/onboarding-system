import http from './http'

export function requestCode(phone) {
  return http.post('/auth/verification-codes', { phone })
}

export function register(payload) {
  return http.post('/auth/register', payload)
}

export function login(payload) {
  return http.post('/auth/login', payload)
}

export function getCurrentUser() {
  return http.get('/auth/me')
}

export function logout() {
  return http.post('/auth/logout')
}
