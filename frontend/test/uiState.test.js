import test from 'node:test'
import assert from 'node:assert/strict'

import { formatDate } from '../src/utils/date.js'
import { validateForm, waitForConfirmation } from '../src/utils/uiState.js'

test('formatDate renders a date without a time component', () => {
  assert.equal(formatDate('2026-09-21T00:00:00'), '2026-09-21')
  assert.equal(formatDate('2026-09-21'), '2026-09-21')
})

test('formatDate keeps an empty value readable', () => {
  assert.equal(formatDate(null), '-')
})

test('validateForm returns false when element validation rejects', async () => {
  const formRef = {
    value: {
      validate: async () => {
        throw new Error('invalid')
      }
    }
  }

  assert.equal(await validateForm(formRef), false)
})

test('validateForm returns true when validation passes', async () => {
  const formRef = {
    value: {
      validate: async () => true
    }
  }

  assert.equal(await validateForm(formRef), true)
})

test('waitForConfirmation returns false when a dialog is dismissed', async () => {
  const confirmation = async () => {
    throw 'cancel'
  }

  assert.equal(await waitForConfirmation(confirmation), false)
})

test('waitForConfirmation returns true after confirmation', async () => {
  const confirmation = async () => 'confirm'

  assert.equal(await waitForConfirmation(confirmation), true)
})

test('waitForConfirmation rethrows unexpected dialog errors', async () => {
  const confirmation = async () => {
    throw new Error('unexpected')
  }

  await assert.rejects(
    () => waitForConfirmation(confirmation),
    /unexpected/
  )
})
