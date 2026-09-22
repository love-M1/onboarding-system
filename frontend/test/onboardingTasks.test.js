import test from 'node:test'
import assert from 'node:assert/strict'

import {
  ONBOARDING_TASKS,
  buildTaskRoster,
  formatFileSize,
  getTaskStatusMeta,
  getTaskProgress,
  submitTaskMaterials
} from '../src/utils/onboardingTasks.js'

test('defines five onboarding tasks with guidance and visual assets', () => {
  assert.equal(ONBOARDING_TASKS.length, 5)

  ONBOARDING_TASKS.forEach((task) => {
    assert.ok(task.name)
    assert.ok(task.summary)
    assert.ok(task.guide.length >= 3)
    assert.ok(task.materials.length >= 1)
    assert.match(task.image, /^https:\/\//)
  })
})

test('buildTaskRoster uses backend records without fabricating demo tasks', () => {
  const records = [
    { taskId: 11, taskName: '办理停车证', taskStatus: 0, dueDate: '2026-09-22' },
    { taskId: 9, taskName: '开通企业邮箱', taskStatus: 1, dueDate: '2026-09-23' }
  ]

  const roster = buildTaskRoster(records)

  assert.equal(roster.length, 2)
  assert.deepEqual(roster.map((task) => task.taskId), [11, 9])
  assert.equal(roster[0].name, '办理停车证')
  assert.ok(roster[0].summary)
  assert.equal(roster[1].shortName, '企业邮箱')
})

test('getTaskStatusMeta covers all four statuses and returned overdue copy', () => {
  assert.equal(getTaskStatusMeta({ taskStatus: 0 }).label, '待员工处理')
  assert.equal(getTaskStatusMeta({ taskStatus: 1 }).label, '待部门确认')
  assert.equal(getTaskStatusMeta({ taskStatus: 2 }).label, '已完成')
  assert.equal(getTaskStatusMeta({ taskStatus: 3, overdue: false }).label, '已退回 · 待补交')
  assert.equal(getTaskStatusMeta({ taskStatus: 3, overdue: true }).label, '已退回 · 已逾期')
})

test('formatFileSize renders readable file sizes', () => {
  assert.equal(formatFileSize(0), '0 B')
  assert.equal(formatFileSize(1024), '1.0 KB')
  assert.equal(formatFileSize(2.5 * 1024 * 1024), '2.5 MB')
})

test('getTaskProgress counts only department-confirmed tasks', () => {
  const roster = [
    { taskId: 1, taskStatus: 2 },
    { taskId: 2, taskStatus: 1 },
    { taskId: 3, taskStatus: 3 },
    { taskId: 4, taskStatus: 0 },
    { taskId: 5, taskStatus: 0 }
  ]

  assert.deepEqual(getTaskProgress(roster), {
    completed: 1,
    total: 5,
    percent: 20
  })
})

test('submitting materials sends real files and note through the submission API', async () => {
  const persisted = []
  const file = new File(['pdf'], '身份证.pdf', { type: 'application/pdf' })
  const submission = {
    files: [file],
    note: '材料已准备'
  }

  await submitTaskMaterials(
    { taskId: 17 },
    submission,
    async (taskId, payload) => persisted.push({ taskId, payload })
  )

  assert.equal(persisted[0].taskId, 17)
  assert.equal(persisted[0].payload.note, '材料已准备')
  assert.deepEqual(persisted[0].payload.files, [file])
})
