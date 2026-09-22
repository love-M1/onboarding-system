import http from './http'

export function listDepartmentOwners() {
  return http.get('/department-owners')
}

export function createDepartmentOwner(data) {
  return http.post('/department-owners', data)
}

export function updateDepartmentOwner(accountId, data) {
  return http.put(`/department-owners/${accountId}`, data)
}

export function disableDepartmentOwner(accountId) {
  return http.delete(`/department-owners/${accountId}`)
}

export function resetDepartmentOwnerPassword(accountId, data) {
  return http.post(`/department-owners/${accountId}/reset-password`, data)
}
