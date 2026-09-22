<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Delete,
  FolderChecked,
  Plus,
  Refresh,
  Search,
  View
} from '@element-plus/icons-vue'
import {
  archiveEmployee,
  createEmployee,
  deleteEmployee,
  getEmployees
} from '../api/employees'
import DepartmentSelect from '../components/DepartmentSelect.vue'
import { formatDate } from '../utils/date'
import { validateForm, waitForConfirmation } from '../utils/uiState'

const router = useRouter()
const loading = ref(false)
const records = ref([])
const total = ref(0)
const filters = reactive({
  empName: '',
  department: '',
  isArchived: null,
  pageNum: 1,
  pageSize: 10
})
const dialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref()
const form = reactive({
  empName: '',
  empPhone: '',
  empDepartment: '',
  empPosition: '',
  entryDate: ''
})

const rules = {
  empName: [{ required: true, message: '请输入员工姓名', trigger: 'blur' }],
  empPhone: [
    { required: true, message: '请输入手机号（工号）', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的11位手机号', trigger: 'blur' }
  ],
  empDepartment: [{ required: true, message: '请输入所属部门', trigger: 'blur' }],
  empPosition: [{ required: true, message: '请输入岗位名称', trigger: 'blur' }],
  entryDate: [{ required: true, message: '请选择入职日期', trigger: 'change' }]
}

async function loadData() {
  loading.value = true
  try {
    const data = await getEmployees({
      empName: filters.empName || undefined,
      department: filters.department || undefined,
      isArchived: filters.isArchived ?? undefined,
      pageNum: filters.pageNum,
      pageSize: filters.pageSize
    })
    records.value = data.list
    total.value = data.total
  } catch {
    // The HTTP interceptor already presents the server error.
  } finally {
    loading.value = false
  }
}

function search() {
  filters.pageNum = 1
  loadData()
}

function resetFilters() {
  Object.assign(filters, {
    empName: '',
    department: '',
    isArchived: null,
    pageNum: 1,
    pageSize: 10
  })
  loadData()
}

function openCreate() {
  Object.assign(form, {
    empName: '',
    empPhone: '',
    empDepartment: '',
    empPosition: '',
    entryDate: ''
  })
  dialogVisible.value = true
}

async function submitForm() {
  if (!(await validateForm(formRef))) return
  submitting.value = true
  try {
    const result = await createEmployee({
      empName: form.empName.trim(),
      empPhone: form.empPhone.trim() || null,
      empDepartment: form.empDepartment.trim(),
      empPosition: form.empPosition.trim(),
      entryTime: `${form.entryDate} 00:00:00`
    })
    dialogVisible.value = false
    ElMessage.success(result.message)
    await loadData()
  } catch {
    // The HTTP interceptor already presents the server error.
  } finally {
    submitting.value = false
  }
}

async function archive(row) {
  const confirmed = await waitForConfirmation(() => ElMessageBox.confirm(
    `确认归档“${row.empName}”的入职档案？归档后任务将只读。`,
    '归档确认',
    { type: 'warning', confirmButtonText: '确认归档', cancelButtonText: '取消' }
  ))
  if (!confirmed) return

  try {
    await archiveEmployee(row.empId)
    ElMessage.success('归档成功')
    await loadData()
  } catch {
    // The HTTP interceptor already presents the server error.
  }
}

async function remove(row) {
  const confirmed = await waitForConfirmation(() => ElMessageBox.confirm(
    `确认删除“${row.empName}”的档案及已完成任务？`,
    '删除确认',
    { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' }
  ))
  if (!confirmed) return

  try {
    await deleteEmployee(row.empId)
    ElMessage.success('删除成功')
    await loadData()
  } catch {
    // The HTTP interceptor already presents the server error.
  }
}

onMounted(loadData)
</script>

<template>
  <section class="page-section">
    <div class="section-heading">
      <div>
        <h2>入职档案</h2>
        <p>登记新员工后，系统会按全部任务模板一次性生成个人任务。</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增档案</el-button>
    </div>

    <div class="filter-bar filter-bar-wide">
      <el-input v-model="filters.empName" clearable placeholder="员工姓名" :prefix-icon="Search" />
      <el-input v-model="filters.department" clearable placeholder="所属部门" :prefix-icon="Search" />
      <el-select v-model="filters.isArchived" clearable placeholder="归档状态">
        <el-option label="未归档" :value="0" />
        <el-option label="已归档" :value="1" />
      </el-select>
      <el-button type="primary" :icon="Search" @click="search">查询</el-button>
      <el-button :icon="Refresh" @click="resetFilters">重置</el-button>
    </div>

    <el-table v-loading="loading" :data="records" border stripe>
      <el-table-column prop="empId" label="编号" width="90" align="center" />
      <el-table-column prop="empName" label="员工姓名" min-width="120" />
      <el-table-column prop="empDepartment" label="所属部门" min-width="140" />
      <el-table-column prop="empPosition" label="岗位" min-width="140" />
      <el-table-column prop="entryTime" label="入职时间" min-width="180">
        <template #default="{ row }">
          {{ formatDate(row.entryTime) }}
        </template>
      </el-table-column>
      <el-table-column label="状态" width="110" align="center">
        <template #default="{ row }">
          <el-tag :type="row.isArchived === 1 ? 'info' : 'success'" effect="plain">
            {{ row.isArchived === 1 ? '已归档' : '未归档' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="235" align="center" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link :icon="View" @click="router.push(`/employees/${row.empId}`)">
            详情
          </el-button>
          <el-button
            type="success"
            link
            :icon="FolderChecked"
            :disabled="row.isArchived === 1"
            @click="archive(row)"
          >
            归档
          </el-button>
          <el-button
            type="danger"
            link
            :icon="Delete"
            :disabled="row.isArchived === 1"
            @click="remove(row)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无入职档案" />
      </template>
    </el-table>

    <div class="pagination-row">
      <el-pagination
        v-model:current-page="filters.pageNum"
        v-model:page-size="filters.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @change="loadData"
      />
    </div>

    <el-dialog v-model="dialogVisible" title="新增入职档案" width="620px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="form-grid">
          <el-form-item label="员工姓名" prop="empName">
            <el-input v-model="form.empName" maxlength="50" />
          </el-form-item>
          <el-form-item label="手机号（工号）" prop="empPhone">
            <el-input v-model="form.empPhone" maxlength="11" />
          </el-form-item>
          <el-form-item label="所属部门" prop="empDepartment">
            <DepartmentSelect v-model="form.empDepartment" />
          </el-form-item>
          <el-form-item label="岗位名称" prop="empPosition">
            <el-input v-model="form.empPosition" maxlength="50" />
          </el-form-item>
          <el-form-item label="入职日期" prop="entryDate" class="form-grid-full">
            <el-date-picker
              v-model="form.entryDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="选择入职日期"
              style="width: 100%"
            />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">建档并生成任务</el-button>
      </template>
    </el-dialog>
  </section>
</template>
