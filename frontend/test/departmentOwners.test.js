import test from 'node:test'
import assert from 'node:assert/strict'

import {
  buildDepartmentOwnerCreatePayload,
  buildDepartmentOwnerUpdatePayload,
  buildPasswordResetPayload
} from '../src/utils/departmentOwners.js'

test('department owner create payload trims values and includes password', () => {
  assert.deepEqual(buildDepartmentOwnerCreatePayload({
    phone: ' 13900000001 ',
    password: 'Owner123',
    displayName: ' 研发部责任人 ',
    department: ' 研发部 '
  }), {
    phone: '13900000001',
    password: 'Owner123',
    displayName: '研发部责任人',
    department: '研发部'
  })
})

test('department owner update payload excludes password and keeps status', () => {
  assert.deepEqual(buildDepartmentOwnerUpdatePayload({
    phone: '13900000001',
    displayName: '研发部责任人',
    department: '研发部',
    status: 'DISABLED'
  }), {
    phone: '13900000001',
    displayName: '研发部责任人',
    department: '研发部',
    status: 'DISABLED'
  })
})

test('password reset payload uses the backend field name', () => {
  assert.deepEqual(buildPasswordResetPayload(' Changed456 '), {
    newPassword: 'Changed456'
  })
})
