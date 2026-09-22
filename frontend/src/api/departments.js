import http from './http'

export function getDepartments() {
  return http.get('/auth/departments')
}
