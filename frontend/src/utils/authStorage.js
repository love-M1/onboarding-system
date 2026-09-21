const STORAGE_KEY = 'onboarding-auth'

export function readStoredAuth() {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY) || 'null')
  } catch {
    return null
  }
}

export function persistAuth(data) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(data))
}

export function clearStoredAuth() {
  localStorage.removeItem(STORAGE_KEY)
}
