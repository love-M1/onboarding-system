import http from './http'

export function getTasks(params) {
  return http.get('/tasks', { params })
}

export function getOverdueTasks() {
  return http.get('/tasks/overdue')
}

export function getTaskDetail(taskId) {
  return http.get(`/tasks/${taskId}`)
}

export function submitTask(taskId, { note = '', files = [] } = {}) {
  const formData = new FormData()
  if (note) {
    formData.append('note', note)
  }
  files.forEach((file) => formData.append('files', file))
  return http.post(`/tasks/${taskId}/submissions`, formData)
}

export function confirmTask(taskId) {
  return http.post(`/tasks/${taskId}/confirm`)
}

export function rejectTask(taskId, data) {
  return http.post(`/tasks/${taskId}/reject`, data)
}

export function downloadTaskAttachment(taskId, attachmentId) {
  return http.get(`/tasks/${taskId}/attachments/${attachmentId}`, {
    responseType: 'blob'
  })
}
