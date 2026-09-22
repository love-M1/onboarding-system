function normalizeText(value) {
  return String(value ?? '').trim()
}

export function buildDepartmentOwnerCreatePayload(form) {
  return {
    phone: normalizeText(form.phone),
    password: String(form.password ?? ''),
    displayName: normalizeText(form.displayName),
    department: normalizeText(form.department)
  }
}

export function buildDepartmentOwnerUpdatePayload(form) {
  return {
    phone: normalizeText(form.phone),
    displayName: normalizeText(form.displayName),
    department: normalizeText(form.department),
    status: normalizeText(form.status)
  }
}

export function buildPasswordResetPayload(password) {
  return {
    newPassword: normalizeText(password)
  }
}
