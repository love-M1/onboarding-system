<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { EditPen, Plus, Refresh, Search } from '@element-plus/icons-vue'
import {
  createTemplate,
  getTemplates,
  updateTemplate
} from '../api/templates'

const loading = ref(false)
const records = ref([])
const filters = reactive({ taskName: '', dutyDept: '' })
const dialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref()
const editingId = ref(null)
const form = reactive({ taskName: '', dutyDept: '', offsetDay: 0 })

const rules = {
  taskName: [{ required: true, message: '请输入任务名称', trigger: 'blur' }],
  dutyDept: [{ required: true, message: '请输入责任部门', trigger: 'blur' }],
  offsetDay: [{ required: true, message: '请输入偏移天数', trigger: 'change' }]
}

async function loadData() {
  loading.value = true
  try {
    records.value = await getTemplates({
      taskName: filters.taskName || undefined,
      dutyDept: filters.dutyDept || undefined
    })
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  filters.taskName = ''
  filters.dutyDept = ''
  loadData()
}

function openCreate() {
  editingId.value = null
  Object.assign(form, { taskName: '', dutyDept: '', offsetDay: 0 })
  dialogVisible.value = true
}

function openEdit(row) {
  editingId.value = row.tplId
  Object.assign(form, {
    taskName: row.taskName,
    dutyDept: row.dutyDept,
    offsetDay: row.offsetDay
  })
  dialogVisible.value = true
}

async function submitForm() {
  await formRef.value.validate()
  submitting.value = true
  try {
    const payload = {
      taskName: form.taskName.trim(),
      dutyDept: form.dutyDept.trim(),
      offsetDay: form.offsetDay
    }
    if (editingId.value) {
      await updateTemplate(editingId.value, payload)
      ElMessage.success('模板修改成功')
    } else {
      await createTemplate(payload)
      ElMessage.success('模板新增成功')
    }
    dialogVisible.value = false
    await loadData()
  } finally {
    submitting.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <section class="page-section">
    <div class="section-heading">
      <div>
        <h2>任务模板</h2>
        <p>模板决定建档时自动生成的任务、责任部门和相对截止天数。</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增模板</el-button>
    </div>

    <div class="filter-bar">
      <el-input v-model="filters.taskName" clearable placeholder="按任务名称筛选" :prefix-icon="Search" />
      <el-input v-model="filters.dutyDept" clearable placeholder="按责任部门筛选" :prefix-icon="Search" />
      <el-button type="primary" :icon="Search" @click="loadData">查询</el-button>
      <el-button :icon="Refresh" @click="resetFilters">重置</el-button>
    </div>

    <el-table v-loading="loading" :data="records" border stripe>
      <el-table-column prop="tplId" label="模板编号" width="110" align="center" />
      <el-table-column prop="taskName" label="任务名称" min-width="220" />
      <el-table-column prop="dutyDept" label="责任部门" min-width="160" />
      <el-table-column prop="offsetDay" label="偏移天数" width="130" align="center">
        <template #default="{ row }">
          {{ row.offsetDay }} 天
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120" align="center" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link :icon="EditPen" @click="openEdit(row)">修改</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无任务模板" />
      </template>
    </el-table>

    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '修改任务模板' : '新增任务模板'"
      width="520px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="任务名称" prop="taskName">
          <el-input v-model="form.taskName" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="责任部门" prop="dutyDept">
          <el-input v-model="form.dutyDept" maxlength="50" />
        </el-form-item>
        <el-form-item label="入职后偏移天数" prop="offsetDay">
          <el-input-number v-model="form.offsetDay" :min="0" :max="365" controls-position="right" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>
