<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Delete,
  EditPen,
  Key,
  Lock,
  Plus,
  Refresh,
  Search,
  Unlock
} from '@element-plus/icons-vue'
import {
  createDepartmentOwner,
  disableDepartmentOwner,
  listDepartmentOwners,
  resetDepartmentOwnerPassword,
  updateDepartmentOwner
} from '../api/departmentOwners'
import DepartmentSelect from '../components/DepartmentSelect.vue'
import {
  buildDepartmentOwnerCreatePayload,
  buildDepartmentOwnerUpdatePayload,
  buildPasswordResetPayload
} from '../utils/departmentOwners'
import { validateForm, waitForConfirmation } from '../utils/uiState'

const loading = ref(false)
const records = ref([])
const keyword = ref('')
const formRef = ref()
const passwordFormRef = ref()
const dialogVisible = ref(false)
const passwordDialogVisible = ref(false)
const submitting = ref(false)
const resettingPassword = ref(false)
const editingId = ref(null)
const passwordTarget = ref(null)

const form = reactive({
  phone: '',
  password: '',
  displayName: '',
  department: '',
  status: 'ACTIVE'
})
const passwordForm = reactive({ newPassword: '' })

const dialogTitle = computed(() => (editingId.value ? '修改部门责任人' : '新增部门责任人'))
const filteredRecords = computed(() => {
  const query = keyword.value.trim()
  if (!query) return records.value
  return records.value.filter((record) => (
    record.displayName.includes(query)
    || record.phone.includes(query)
    || record.department.includes(query)
  ))
})

const passwordValidator = (_rule, value, callback) => {
  if (editingId.value) {
    callback()
    return
  }
  if (!value) {
    callback(new Error('请输入初始密码'))
    return
  }
  if (value.length < 8 || value.length > 32 || !/[A-Za-z]/.test(value) || !/\d/.test(value)) {
    callback(new Error('密码需为8至32位且同时包含字母和数字'))
    return
  }
  callback()
}

const resetPasswordValidator = (_rule, value, callback) => {
  if (!value) {
    callback(new Error('请输入新密码'))
    return
  }
  if (value.length < 8 || value.length > 32 || !/[A-Za-z]/.test(value) || !/\d/.test(value)) {
    callback(new Error('密码需为8至32位且同时包含字母和数字'))
    return
  }
  callback()
}

const rules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的11位手机号', trigger: 'blur' }
  ],
  password: [{ validator: passwordValidator, trigger: 'blur' }],
  displayName: [{ required: true, message: '请输入责任人姓名', trigger: 'blur' }],
  department: [{ required: true, message: '请选择所属部门', trigger: 'change' }],
  status: [{ required: true, message: '请选择账号状态', trigger: 'change' }]
}

const passwordRules = {
  newPassword: [{ validator: resetPasswordValidator, trigger: 'blur' }]
}

async function loadData() {
  loading.value = true
  try {
    records.value = await listDepartmentOwners()
  } catch {
    // The HTTP interceptor already presents the server error.
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  Object.assign(form, {
    phone: '',
    password: '',
    displayName: '',
    department: '',
    status: 'ACTIVE'
  })
  dialogVisible.value = true
}

function openEdit(row) {
  editingId.value = row.accountId
  Object.assign(form, {
    phone: row.phone,
    password: '',
    displayName: row.displayName,
    department: row.department,
    status: row.status
  })
  dialogVisible.value = true
}

async function submitForm() {
  if (!(await validateForm(formRef))) return
  submitting.value = true
  try {
    if (editingId.value) {
      await updateDepartmentOwner(
        editingId.value,
        buildDepartmentOwnerUpdatePayload(form)
      )
      ElMessage.success('部门责任人修改成功')
    } else {
      await createDepartmentOwner(buildDepartmentOwnerCreatePayload(form))
      ElMessage.success('部门责任人新增成功')
    }
    dialogVisible.value = false
    await loadData()
  } catch {
    // The HTTP interceptor already presents the server error.
  } finally {
    submitting.value = false
  }
}

async function toggleStatus(row) {
  if (row.status === 'DISABLED') {
    try {
      await updateDepartmentOwner(row.accountId, buildDepartmentOwnerUpdatePayload({
        ...row,
        status: 'ACTIVE'
      }))
      ElMessage.success('账号已启用')
      await loadData()
    } catch {
      // The HTTP interceptor already presents the server error.
    }
    return
  }

  const confirmed = await waitForConfirmation(() => ElMessageBox.confirm(
    `确认停用“${row.displayName}”的登录账号？`,
    '停用确认',
    { type: 'warning', confirmButtonText: '确认停用', cancelButtonText: '取消' }
  ))
  if (!confirmed) return
  try {
    await disableDepartmentOwner(row.accountId)
    ElMessage.success('账号已停用')
    await loadData()
  } catch {
    // The HTTP interceptor already presents the server error.
  }
}

async function remove(row) {
  const confirmed = await waitForConfirmation(() => ElMessageBox.confirm(
    `确认删除“${row.displayName}”的部门责任人账号？历史任务记录会保留。`,
    '删除确认',
    { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' }
  ))
  if (!confirmed) return
  try {
    await disableDepartmentOwner(row.accountId)
    ElMessage.success('部门责任人账号已停用')
    await loadData()
  } catch {
    // The HTTP interceptor already presents the server error.
  }
}

function openResetPassword(row) {
  passwordTarget.value = row
  passwordForm.newPassword = ''
  passwordDialogVisible.value = true
}

async function submitPasswordReset() {
  if (!(await validateForm(passwordFormRef))) return
  resettingPassword.value = true
  try {
    await resetDepartmentOwnerPassword(
      passwordTarget.value.accountId,
      buildPasswordResetPayload(passwordForm.newPassword)
    )
    passwordDialogVisible.value = false
    ElMessage.success('密码重置成功')
  } catch {
    // The HTTP interceptor already presents the server error.
  } finally {
    resettingPassword.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <section class="page-section">
    <div class="section-heading">
      <div>
        <h2>部门责任人</h2>
        <p>每个部门最多启用一个责任人账号，负责确认或退回该部门的员工任务。</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增责任人</el-button>
    </div>

    <div class="filter-bar owner-filter-bar">
      <el-input
        v-model="keyword"
        clearable
        placeholder="按姓名、手机号或部门筛选"
        :prefix-icon="Search"
      />
      <el-button :icon="Refresh" @click="loadData">刷新</el-button>
    </div>

    <el-table v-loading="loading" :data="filteredRecords" border stripe>
      <el-table-column prop="accountId" label="账号编号" width="100" align="center" />
      <el-table-column prop="displayName" label="责任人" min-width="140" />
      <el-table-column prop="phone" label="手机号" min-width="140" />
      <el-table-column prop="department" label="负责部门" min-width="150" />
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" effect="plain">
            {{ row.status === 'ACTIVE' ? '已启用' : '已停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="updatedAt" label="更新时间" min-width="180" />
      <el-table-column label="操作" width="330" align="center" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link :icon="EditPen" @click="openEdit(row)">修改</el-button>
          <el-button
            :type="row.status === 'ACTIVE' ? 'warning' : 'success'"
            link
            :icon="row.status === 'ACTIVE' ? Lock : Unlock"
            @click="toggleStatus(row)"
          >
            {{ row.status === 'ACTIVE' ? '停用' : '启用' }}
          </el-button>
          <el-button type="primary" link :icon="Key" @click="openResetPassword(row)">
            重置密码
          </el-button>
          <el-button type="danger" link :icon="Delete" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无部门责任人" />
      </template>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="form-grid">
          <el-form-item label="责任人姓名" prop="displayName">
            <el-input v-model="form.displayName" maxlength="50" />
          </el-form-item>
          <el-form-item label="手机号" prop="phone">
            <el-input v-model="form.phone" maxlength="11" />
          </el-form-item>
          <el-form-item label="负责部门" prop="department">
            <DepartmentSelect v-model="form.department" />
          </el-form-item>
          <el-form-item v-if="editingId" label="账号状态" prop="status">
            <el-select v-model="form.status" style="width: 100%">
              <el-option label="启用" value="ACTIVE" />
              <el-option label="停用" value="DISABLED" />
            </el-select>
          </el-form-item>
          <el-form-item v-else label="初始密码" prop="password" class="form-grid-full">
            <el-input
              v-model="form.password"
              type="password"
              show-password
              maxlength="32"
              placeholder="8至32位，同时包含字母和数字"
            />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="passwordDialogVisible" title="重置密码" width="440px" destroy-on-close>
      <el-form
        ref="passwordFormRef"
        :model="passwordForm"
        :rules="passwordRules"
        label-position="top"
      >
        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="passwordForm.newPassword"
            type="password"
            show-password
            maxlength="32"
            placeholder="8至32位，同时包含字母和数字"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="resettingPassword" @click="submitPasswordReset">
          确认重置
        </el-button>
      </template>
    </el-dialog>
  </section>
</template>
