export function formatDate(value) {
  if (!value) return '-'
  const date = String(value).match(/^\d{4}-\d{2}-\d{2}/)
  return date ? date[0] : String(value)
}
