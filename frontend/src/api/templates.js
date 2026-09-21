import http from './http'

export function getTemplates(params) {
  return http.get('/templates', { params })
}

export function getTemplate(tplId) {
  return http.get(`/templates/${tplId}`)
}

export function createTemplate(data) {
  return http.post('/templates', data)
}

export function updateTemplate(tplId, data) {
  return http.put(`/templates/${tplId}`, data)
}
