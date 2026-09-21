export async function validateForm(formRef) {
  if (!formRef?.value) return false

  try {
    const valid = await formRef.value.validate()
    return valid !== false
  } catch {
    return false
  }
}

export async function waitForConfirmation(confirmation) {
  try {
    await confirmation()
    return true
  } catch (error) {
    if (error === 'cancel' || error === 'close') return false
    throw error
  }
}
