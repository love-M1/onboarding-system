import http from './http'

export function getEmployeeStats(empId) {
  return http.get(`/stats/employees/${empId}`)
}

export function getDepartmentStats() {
  return http.get('/stats/departments')
}
