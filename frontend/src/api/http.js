import axios from 'axios'
import { ElMessage } from 'element-plus'
import { clearStoredAuth, readStoredAuth } from '../utils/authStorage'

const http = axios.create({
  baseURL: '/api',
  timeout: 15000
})

http.interceptors.request.use((config) => {
  const auth = readStoredAuth()
  if (auth?.token) {
    config.headers.Authorization = `Bearer ${auth.token}`
  }
  return config
})

function redirectToLogin() {
  clearStoredAuth()
  if (window.location.pathname !== '/login') {
    window.location.replace('/login')
  }
}

http.interceptors.response.use(
  (response) => {
    const payload = response.data
    if (payload?.code === 401) {
      redirectToLogin()
    }
    if (payload?.code !== 200) {
      const error = new Error(payload?.message || '请求处理失败')
      error.code = payload?.code
      ElMessage.error(error.message)
      return Promise.reject(error)
    }
    return payload.data
  },
  (error) => {
    if (error.response?.status === 401 || error.response?.data?.code === 401) {
      redirectToLogin()
    }
    const message = error.response?.data?.message || error.message || '网络连接失败'
    ElMessage.error(message)
    return Promise.reject(error)
  }
)

export default http
