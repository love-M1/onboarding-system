<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check, Refresh, Search } from '@element-plus/icons-vue'
import { getTasks, finishTask } from '../api/tasks'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const loading = ref(false)
const records = ref([])
const total = ref(0)
const filters = reactive({
  empId: '',
  department: '',
  status: '',
  pageNum: 1,
  pageSize: 10
})

async function loadData() {
  loading.value = true
  try {
    const data = await getTasks({
      empId: filters.empId || undefined,
      department: auth.isHr ? filters.department || undefined : undefined,
      status: filters.status || undefined,
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
    empId: '',
    department: '',
    status: '',
    pageNum: 1,
    pageSize: 10
  })
  loadData()
}

function taskTag(task) {
  if (task.taskStatus === 1) return { label: '已完成', type: 'success' }
  if (task.overdue) return { label: '已逾期', type: 'danger' }
  return { label: '待完成', type: 'warning' }
}

function disabledReason(task) {
  if (task.taskStatus === 1) return '任务已经完成'
  if (task.archived) return '档案已归档，不能修改任务状态'
  if (!task.canFinish) return '入职日期尚未到来'
  return ''
}

async function confirmFinish(task) {
  await ElMessageBox.confirm(
    `确认“${task.taskName}”已经办理完成？`,
    '任务确认',
    { type: 'warning', confirmButtonText: '确认完成', cancelButtonText: '取消' }
  )
  await finishTask(task.taskId)
  ElMessage.success('任务已确认完成')
  loadData()
}

onMounted(loadData)
</script>

<template>
  <section class="page-section">
    <div class="section-heading">
      <div>
        <h2>部门任务</h2>
        <p v-if="auth.isDepartment">
          仅显示 {{ auth.department }} 负责的任务，逾期事项仍可确认完成。
        </p>
        <p v-else>按部门、状态或员工查询任务并处理异常事项。</p>
      </div>
    </div>

    <div class="filter-bar filter-bar-wide">
      <el-input v-model="filters.empId" clearable placeholder="员工编号" :prefix-icon="Search" />
      <el-input
        v-if="auth.isHr"
        v-model="filters.department"
        clearable
        placeholder="责任部门"
        :prefix-icon="Search"
      />
      <el-select v-model="filters.status" clearable placeholder="任务状态">
        <el-option label="待完成" value="0" />
        <el-option label="已完成" value="1" />
        <el-option label="已逾期" value="overdue" />
      </el-select>
      <el-button type="primary" :icon="Search" @click="search">查询</el-button>
      <el-button :icon="Refresh" @click="resetFilters">重置</el-button>
    </div>

    <el-table v-loading="loading" :data="records" border stripe>
      <el-table-column prop="taskId" label="任务编号" width="105" align="center" />
      <el-table-column prop="empName" label="员工" min-width="110" />
      <el-table-column prop="taskName" label="任务名称" min-width="200" />
      <el-table-column prop="dutyDept" label="责任部门" min-width="140" />
      <el-table-column prop="dueDate" label="应完成日期" min-width="135" align="center" />
      <el-table-column label="状态" width="110" align="center">
        <template #default="{ row }">
          <el-tag :type="taskTag(row).type" effect="plain">
            {{ taskTag(row).label }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="130" align="center" fixed="right">
        <template #default="{ row }">
          <el-tooltip
            :content="disabledReason(row)"
            :disabled="!disabledReason(row)"
            placement="top"
          >
            <span>
              <el-button
                type="primary"
                link
                :icon="Check"
                :disabled="Boolean(disabledReason(row))"
                @click="confirmFinish(row)"
              >
                确认完成
              </el-button>
            </span>
          </el-tooltip>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无符合条件的任务" />
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
