import http from './http'

export function getTasks(params) {
  return http.get('/tasks', { params })
}

export function finishTask(taskId) {
  return http.post(`/tasks/${taskId}/finish`)
}

export function getOverdueTasks() {
  return http.get('/tasks/overdue')
}
