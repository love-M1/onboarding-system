export async function loadStatsDashboard({
  selectedEmpId,
  listEmployees,
  listDepartmentStats,
  listOverdueTasks,
  loadEmployeeStats
}) {
  const [employees, departmentStats, overdueTasks] = await Promise.all([
    listEmployees(),
    listDepartmentStats(),
    listOverdueTasks()
  ])
  const resolvedEmpId = selectedEmpId || employees.list[0]?.empId || null
  const employeeStats = resolvedEmpId
    ? await loadEmployeeStats(resolvedEmpId)
    : null

  return {
    employeeOptions: employees.list,
    departmentStats,
    overdueTasks,
    selectedEmpId: resolvedEmpId,
    employeeStats
  }
}
