import test from 'node:test'
import assert from 'node:assert/strict'

import {
  ONBOARDING_TASKS,
  buildTaskRoster,
  formatFileSize,
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

test('buildTaskRoster always keeps five task cards and attaches matching records', () => {
  const records = [
    { taskId: 9, taskName: '开通企业邮箱', taskStatus: 1, dueDate: '2026-09-22' },
    { taskId: 10, taskName: '签署劳动合同', taskStatus: 0, dueDate: '2026-09-23' }
  ]

  const roster = buildTaskRoster(records)

  assert.equal(roster.length, 5)
  assert.equal(roster[0].taskId, 'demo-materials')
  assert.equal(roster[1].taskId, 10)
  assert.equal(roster[3].taskId, 9)
  assert.equal(roster[3].taskStatus, 1)
})

test('formatFileSize renders readable demo file sizes', () => {
  assert.equal(formatFileSize(0), '0 B')
  assert.equal(formatFileSize(1024), '1.0 KB')
  assert.equal(formatFileSize(2.5 * 1024 * 1024), '2.5 MB')
})

test('getTaskProgress combines backend completion and local demo submissions', () => {
  const roster = [
    { taskId: 1, taskStatus: 1 },
    { taskId: 2, taskStatus: 0 },
    { taskId: 3, taskStatus: 0 },
    { taskId: 4, taskStatus: 0 },
    { taskId: 5, taskStatus: 0 }
  ]
  const submissions = {
    2: { files: [{ name: '身份证.pdf', size: 1024 }], submitted: true },
    3: { files: [{ name: '草稿.png', size: 2048 }], submitted: false }
  }

  assert.deepEqual(getTaskProgress(roster, submissions), {
    completed: 2,
    total: 5,
    percent: 40
  })
})

test('submitting materials persists task completion before marking the submission complete', async () => {
  const persisted = []
  const submission = {
    files: [{ name: '身份证.pdf', size: 1024 }],
    note: '',
    submitted: false,
    submittedAt: ''
  }

  await submitTaskMaterials(
    { taskId: 17 },
    submission,
    async (taskId) => persisted.push(taskId)
  )

  assert.deepEqual(persisted, [17])
  assert.equal(submission.submitted, true)
  assert.ok(submission.submittedAt)
})
