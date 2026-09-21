<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, FolderChecked, Refresh, Search, View } from '@element-plus/icons-vue'
import { archiveEmployee, deleteEmployee, getEmployees } from '../api/employees'

const router = useRouter()
const loading = ref(false)
const records = ref([])
const total = ref(0)
const filters = reactive({
  empName: '',
  department: '',
  isArchived: 0,
  pageNum: 1,
  pageSize: 10
})

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
    isArchived: 0,
    pageNum: 1,
    pageSize: 10
  })
  loadData()
}

async function archive(row) {
  await ElMessageBox.confirm(
    `仅当“${row.empName}”的所有任务完成后才能归档。确认继续？`,
    '归档确认',
    { type: 'warning', confirmButtonText: '确认归档', cancelButtonText: '取消' }
  )
  await archiveEmployee(row.empId)
  ElMessage.success('归档成功')
  loadData()
}

async function remove(row) {
  await ElMessageBox.confirm(
    `确认删除“${row.empName}”的档案？存在未完成任务时后端会拒绝。`,
    '删除确认',
    { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' }
  )
  await deleteEmployee(row.empId)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(loadData)
</script>

<template>
  <section class="page-section">
    <div class="section-heading">
      <div>
        <h2>归档管理</h2>
        <p>归档前系统会检查未完成任务；归档后的档案保持只读。</p>
      </div>
    </div>

    <div class="filter-bar filter-bar-wide">
      <el-input v-model="filters.empName" clearable placeholder="员工姓名" :prefix-icon="Search" />
      <el-input v-model="filters.department" clearable placeholder="所属部门" :prefix-icon="Search" />
      <el-select v-model="filters.isArchived" placeholder="档案状态">
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
      <el-table-column prop="entryTime" label="入职时间" min-width="180" />
      <el-table-column label="档案状态" width="110" align="center">
        <template #default="{ row }">
          <el-tag :type="row.isArchived === 1 ? 'info' : 'success'" effect="plain">
            {{ row.isArchived === 1 ? '已归档' : '未归档' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="225" align="center" fixed="right">
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
        <el-empty description="暂无档案记录" />
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
  </section>
</template>
