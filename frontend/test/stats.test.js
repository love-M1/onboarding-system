import test from 'node:test'
import assert from 'node:assert/strict'

import { loadStatsDashboard } from '../src/utils/stats.js'

test('refreshing the dashboard reloads the currently selected employee', async () => {
  const loadedEmployeeIds = []

  const result = await loadStatsDashboard({
    selectedEmpId: 7,
    listEmployees: async () => ({
      list: [{ empId: 7, empName: '宋江' }]
    }),
    listDepartmentStats: async () => [{ dutyDept: '人事部', finishedCount: 1 }],
    listOverdueTasks: async () => [],
    loadEmployeeStats: async (empId) => {
      loadedEmployeeIds.push(empId)
      return { empId, finishedCount: 1, totalCount: 5 }
    }
  })

  assert.deepEqual(loadedEmployeeIds, [7])
  assert.equal(result.employeeStats.finishedCount, 1)
})
