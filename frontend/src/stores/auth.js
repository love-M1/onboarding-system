import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import * as authApi from '../api/auth'
import {
  clearStoredAuth,
  persistAuth,
  readStoredAuth
} from '../utils/authStorage'

export const useAuthStore = defineStore('auth', () => {
  const stored = readStoredAuth()
  const token = ref(stored?.token || '')
  const accountId = ref(stored?.accountId || null)
  const phone = ref(stored?.phone || '')
  const displayName = ref(stored?.displayName || '')
  const role = ref(stored?.role || '')
  const empId = ref(stored?.empId || null)
  const department = ref(stored?.department || '')

  const isHr = computed(() => role.value === 'HR')
  const isEmployee = computed(() => role.value === 'EMPLOYEE')
  const isDepartment = computed(() => role.value === 'DEPARTMENT')
  const isLoggedIn = computed(() => Boolean(token.value && role.value))
  const homePath = computed(() => (isHr.value ? '/templates' : '/tasks'))
  const operator = computed(() => displayName.value || phone.value)

  function applyUser(user) {
    accountId.value = user.accountId
    phone.value = user.phone
    displayName.value = user.displayName
    role.value = user.role
    empId.value = user.empId || null
    department.value = user.department || ''
  }

  function persist() {
    persistAuth({
      token: token.value,
      accountId: accountId.value,
      phone: phone.value,
      displayName: displayName.value,
      role: role.value,
      empId: empId.value,
      department: department.value
    })
  }

  function applySession(result) {
    token.value = result.token
    applyUser(result.user)
    persist()
  }

  function clear() {
    token.value = ''
    accountId.value = null
    phone.value = ''
    displayName.value = ''
    role.value = ''
    empId.value = null
    department.value = ''
    clearStoredAuth()
  }

  async function login(payload) {
    const result = await authApi.login(payload)
    applySession(result)
    return result
  }

  async function register(payload) {
    const result = await authApi.register(payload)
    applySession(result)
    return result
  }

  async function restore() {
    if (!token.value) return
    try {
      const user = await authApi.getCurrentUser()
      applyUser(user)
      persist()
    } catch {
      clear()
    }
  }

  async function logout() {
    try {
      if (token.value) {
        await authApi.logout()
      }
    } finally {
      clear()
    }
  }

  return {
    token,
    accountId,
    phone,
    displayName,
    role,
    empId,
    department,
    operator,
    isHr,
    isEmployee,
    isDepartment,
    isLoggedIn,
    homePath,
    login,
    register,
    restore,
    logout,
    clear
  }
})
