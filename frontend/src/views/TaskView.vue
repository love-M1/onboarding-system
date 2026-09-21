<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check, Refresh, Search } from '@element-plus/icons-vue'
import { getTasks, finishTask } from '../api/tasks'
import { useAuthStore } from '../stores/auth'
import { waitForConfirmation } from '../utils/uiState'

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
const pageTitle = computed(() => (auth.isEmployee ? '我的任务' : '部门任务'))
const showFilters = computed(() => !auth.isEmployee)
const canConfirm = computed(() => auth.isEmployee || auth.isDepartment)

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
  const confirmed = await waitForConfirmation(() => ElMessageBox.confirm(
    `确认“${task.taskName}”已经办理完成？`,
    '任务确认',
    { type: 'warning', confirmButtonText: '确认完成', cancelButtonText: '取消' }
  ))
  if (!confirmed) return

  try {
    await finishTask(task.taskId)
    ElMessage.success('任务已确认完成')
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
        <h2>{{ pageTitle }}</h2>
        <p v-if="auth.isEmployee">按应完成日期查看本人的全部入职任务。</p>
        <p v-else-if="auth.isDepartment">
          仅显示 {{ auth.department }} 负责的任务，逾期事项仍可确认完成。
        </p>
        <p v-else>按部门、状态或员工查询任务并处理异常事项。</p>
      </div>
    </div>

    <div v-if="showFilters" class="filter-bar filter-bar-wide">
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

    <el-table v-loading="loading" :data="records" border stripe class="task-table-desktop">
      <el-table-column prop="taskId" label="任务编号" width="105" align="center" />
      <el-table-column v-if="!auth.isEmployee" prop="empName" label="员工" min-width="110" />
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
      <el-table-column v-if="canConfirm" label="操作" width="130" align="center" fixed="right">
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

    <div v-loading="loading" class="task-mobile-list">
      <el-empty v-if="!records.length" description="暂无符合条件的任务" />
      <article v-for="task in records" :key="task.taskId" class="task-mobile-card">
        <div class="task-mobile-head">
          <div>
            <span>任务 #{{ task.taskId }}</span>
            <strong>{{ task.taskName }}</strong>
          </div>
          <el-tag :type="taskTag(task).type" effect="plain">
            {{ taskTag(task).label }}
          </el-tag>
        </div>
        <dl class="task-mobile-meta">
          <div v-if="!auth.isEmployee">
            <dt>员工</dt>
            <dd>{{ task.empName }}</dd>
          </div>
          <div>
            <dt>责任部门</dt>
            <dd>{{ task.dutyDept }}</dd>
          </div>
          <div>
            <dt>应完成日期</dt>
            <dd>{{ task.dueDate }}</dd>
          </div>
        </dl>
        <div v-if="canConfirm" class="task-mobile-action">
          <el-button
            type="primary"
            plain
            :icon="Check"
            :disabled="Boolean(disabledReason(task))"
            @click="confirmFinish(task)"
          >
            确认完成
          </el-button>
          <span v-if="disabledReason(task)">{{ disabledReason(task) }}</span>
        </div>
      </article>
    </div>

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
