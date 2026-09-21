import http from './http'

export function getEmployees(params) {
  return http.get('/employees', { params })
}

export function getEmployee(empId) {
  return http.get(`/employees/${empId}`)
}

export function createEmployee(data) {
  return http.post('/employees', data)
}

export function archiveEmployee(empId) {
  return http.post(`/employees/${empId}/archive`)
}

export function deleteEmployee(empId) {
  return http.delete(`/employees/${empId}`)
}
