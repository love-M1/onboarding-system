<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, CircleCheck, Clock, Warning } from '@element-plus/icons-vue'
import { getEmployee } from '../api/employees'
import { getEmployeeStats } from '../api/stats'
import { formatDate } from '../utils/date'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const detail = ref(null)
const stats = ref({
  totalCount: 0,
  finishedCount: 0,
  pendingCount: 0,
  overdueCount: 0
})

const progress = computed(() => {
  if (!stats.value.totalCount) return 0
  return Math.round((stats.value.finishedCount / stats.value.totalCount) * 100)
})

async function loadData() {
  loading.value = true
  try {
    const empId = Number(route.params.empId)
    const [detailData, statsData] = await Promise.all([
      getEmployee(empId),
      getEmployeeStats(empId)
    ])
    detail.value = detailData
    stats.value = statsData
  } catch {
    // The HTTP interceptor already presents the server error.
  } finally {
    loading.value = false
  }
}

function taskTag(task) {
  if (task.taskStatus === 1) return { label: '已完成', type: 'success' }
  if (task.overdue) return { label: '已逾期', type: 'danger' }
  return { label: '待完成', type: 'warning' }
}

onMounted(loadData)
</script>

<template>
  <section v-loading="loading" class="page-section">
    <div class="section-heading">
      <div class="heading-with-back">
        <el-button :icon="ArrowLeft" circle @click="router.push('/employees')" />
        <div>
          <h2>档案详情</h2>
          <p>查看员工信息、任务应完成日期和整体办理进度。</p>
        </div>
      </div>
    </div>

    <template v-if="detail">
      <div class="detail-band">
        <div class="detail-identity">
          <span class="detail-avatar">{{ detail.empName.slice(0, 1) }}</span>
          <div>
            <h3>{{ detail.empName }}</h3>
            <p>{{ detail.empDepartment }} · {{ detail.empPosition }}</p>
          </div>
        </div>
        <dl class="detail-list">
          <div>
            <dt>档案编号</dt>
            <dd>{{ detail.empId }}</dd>
          </div>
          <div>
            <dt>联系电话</dt>
            <dd>{{ detail.empPhone || '未填写' }}</dd>
          </div>
          <div>
            <dt>入职时间</dt>
            <dd>{{ formatDate(detail.entryTime) }}</dd>
          </div>
          <div>
            <dt>档案状态</dt>
            <dd>
              <el-tag :type="detail.isArchived === 1 ? 'info' : 'success'" effect="plain">
                {{ detail.isArchived === 1 ? '已归档' : '未归档' }}
              </el-tag>
            </dd>
          </div>
        </dl>
      </div>

      <div class="metric-grid">
        <article class="metric-item">
          <span>任务总数</span>
          <strong>{{ stats.totalCount }}</strong>
        </article>
        <article class="metric-item">
          <span><el-icon><CircleCheck /></el-icon> 已完成</span>
          <strong>{{ stats.finishedCount }}</strong>
        </article>
        <article class="metric-item">
          <span><el-icon><Clock /></el-icon> 待完成</span>
          <strong>{{ stats.pendingCount }}</strong>
        </article>
        <article class="metric-item warning">
          <span><el-icon><Warning /></el-icon> 已逾期</span>
          <strong>{{ stats.overdueCount }}</strong>
        </article>
      </div>

      <div class="progress-row">
        <span>整体完成进度</span>
        <el-progress :percentage="progress" :stroke-width="12" />
      </div>

      <div class="table-block">
        <div class="table-title">
          <h3>任务清单</h3>
          <span>截止日当天不计入逾期</span>
        </div>
        <el-table :data="detail.tasks" border stripe>
          <el-table-column prop="taskId" label="任务编号" width="105" align="center" />
          <el-table-column prop="taskName" label="任务名称" min-width="210" />
          <el-table-column prop="dutyDept" label="责任部门" min-width="150" />
          <el-table-column prop="dueDate" label="应完成日期" min-width="140" align="center" />
          <el-table-column label="状态" width="110" align="center">
            <template #default="{ row }">
              <el-tag :type="taskTag(row).type" effect="plain">
                {{ taskTag(row).label }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="finishTime" label="完成时间" min-width="180">
            <template #default="{ row }">
              {{ row.finishTime || '—' }}
            </template>
          </el-table-column>
        </el-table>
      </div>
    </template>
  </section>
</template>
