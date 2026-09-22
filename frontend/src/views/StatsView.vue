<script setup>
import { onMounted, ref } from 'vue'
import { CircleCheck, Clock, DataAnalysis, Warning } from '@element-plus/icons-vue'
import { getEmployees } from '../api/employees'
import { getDepartmentStats, getEmployeeStats } from '../api/stats'
import { getOverdueTasks } from '../api/tasks'
import { getTaskStatusMeta } from '../utils/onboardingTasks'
import { loadStatsDashboard } from '../utils/stats'

const loading = ref(false)
const employeeLoading = ref(false)
const employeeOptions = ref([])
const selectedEmpId = ref(null)
const employeeStats = ref(null)
const departmentStats = ref([])
const overdueTasks = ref([])

async function loadBaseData() {
  loading.value = true
  try {
    const data = await loadStatsDashboard({
      selectedEmpId: selectedEmpId.value,
      listEmployees: () => getEmployees({ pageNum: 1, pageSize: 100 }),
      listDepartmentStats: getDepartmentStats,
      listOverdueTasks: getOverdueTasks,
      loadEmployeeStats: getEmployeeStats
    })
    employeeOptions.value = data.employeeOptions
    departmentStats.value = data.departmentStats
    overdueTasks.value = data.overdueTasks
    selectedEmpId.value = data.selectedEmpId
    employeeStats.value = data.employeeStats
  } catch {
    // The HTTP interceptor already presents the server error.
  } finally {
    loading.value = false
  }
}

async function searchEmployees(keyword) {
  employeeLoading.value = true
  try {
    const result = await getEmployees({
      empName: keyword || undefined,
      pageNum: 1,
      pageSize: 50
    })
    employeeOptions.value = result.list
  } catch {
    // The HTTP interceptor already presents the server error.
  } finally {
    employeeLoading.value = false
  }
}

async function loadEmployeeStats() {
  if (!selectedEmpId.value) {
    employeeStats.value = null
    return
  }
  try {
    employeeStats.value = await getEmployeeStats(selectedEmpId.value)
  } catch {
    // The HTTP interceptor already presents the server error.
  }
}

function progress(row) {
  if (!row.totalCount) return 0
  return Math.round((row.finishedCount / row.totalCount) * 100)
}

function taskStatusMeta(task) {
  return getTaskStatusMeta(task)
}

onMounted(loadBaseData)
</script>

<template>
  <section v-loading="loading" class="page-section">
    <div class="section-heading">
      <div>
        <h2>进度汇总</h2>
        <p>按员工查看完成数量，按责任部门核对办理进度和逾期情况。</p>
      </div>
    </div>

    <div class="stats-toolbar">
      <div>
        <span>选择员工</span>
        <el-select
          v-model="selectedEmpId"
          filterable
          remote
          reserve-keyword
          :remote-method="searchEmployees"
          :loading="employeeLoading"
          placeholder="选择员工档案"
          style="width: 260px"
          @change="loadEmployeeStats"
        >
          <el-option
            v-for="item in employeeOptions"
            :key="item.empId"
            :label="`${item.empName}（${item.empDepartment}）`"
            :value="item.empId"
          />
        </el-select>
      </div>
      <el-button :icon="DataAnalysis" @click="loadBaseData">刷新汇总</el-button>
    </div>

    <div v-if="employeeStats" class="metric-grid">
      <article class="metric-item">
        <span>任务总数</span>
        <strong>{{ employeeStats.totalCount }}</strong>
      </article>
      <article class="metric-item">
        <span><el-icon><CircleCheck /></el-icon> 已完成</span>
        <strong>{{ employeeStats.finishedCount }}</strong>
      </article>
      <article class="metric-item">
        <span><el-icon><Clock /></el-icon> 待完成</span>
        <strong>{{ employeeStats.pendingCount }}</strong>
      </article>
      <article class="metric-item warning">
        <span><el-icon><Warning /></el-icon> 已逾期</span>
        <strong>{{ employeeStats.overdueCount }}</strong>
      </article>
    </div>

    <div class="table-block">
      <div class="table-title">
        <h3>部门完成情况</h3>
        <span>按任务责任部门分组统计</span>
      </div>
      <el-table :data="departmentStats" border stripe>
        <el-table-column prop="dutyDept" label="责任部门" min-width="160" />
        <el-table-column prop="totalCount" label="任务总数" width="110" align="center" />
        <el-table-column prop="finishedCount" label="已完成" width="100" align="center" />
        <el-table-column prop="pendingCount" label="待完成" width="100" align="center" />
        <el-table-column prop="overdueCount" label="已逾期" width="100" align="center" />
        <el-table-column label="完成率" min-width="220">
          <template #default="{ row }">
            <el-progress :percentage="progress(row)" :stroke-width="10" />
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="table-block">
      <div class="table-title">
        <h3>全部逾期任务</h3>
        <span>按应完成日期升序排列</span>
      </div>
      <el-table :data="overdueTasks" border stripe>
        <el-table-column prop="taskId" label="任务编号" width="105" align="center" />
        <el-table-column prop="empName" label="员工" min-width="110" />
        <el-table-column prop="taskName" label="任务名称" min-width="200" />
        <el-table-column prop="assignedDept" label="责任部门" min-width="140" />
        <el-table-column prop="currentDueDate" label="当前截止日期" min-width="140" align="center" />
        <el-table-column label="状态" width="165" align="center">
          <template #default="{ row }">
            <el-tag :type="taskStatusMeta(row).type" effect="plain">
              {{ taskStatusMeta(row).label }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </section>
</template>
