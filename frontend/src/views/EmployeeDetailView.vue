<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ArrowLeft,
  CircleCheck,
  Clock,
  Download,
  Warning
} from '@element-plus/icons-vue'
import { getEmployee } from '../api/employees'
import { getEmployeeStats } from '../api/stats'
import { downloadTaskAttachment, getTaskDetail } from '../api/tasks'
import { formatDate } from '../utils/date'
import { getTaskStatusMeta } from '../utils/onboardingTasks'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const historyLoading = ref(false)
const detail = ref(null)
const historyVisible = ref(false)
const historyTask = ref(null)
const historyActions = ref([])
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
  return getTaskStatusMeta(task)
}

async function openHistory(task) {
  historyTask.value = task
  historyActions.value = []
  historyVisible.value = true
  historyLoading.value = true
  try {
    const taskDetail = await getTaskDetail(task.taskId)
    historyActions.value = taskDetail.actions
  } catch {
    // The HTTP interceptor already presents the server error.
  } finally {
    historyLoading.value = false
  }
}

async function downloadAttachment(attachment) {
  try {
    const blob = await downloadTaskAttachment(historyTask.value.taskId, attachment.attachmentId)
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = attachment.originalName || '任务附件'
    document.body.appendChild(link)
    link.click()
    link.remove()
    URL.revokeObjectURL(url)
  } catch {
    // The HTTP interceptor already presents the server error.
  }
}

function actionLabel(type) {
  if (type === 'SUBMIT') return '员工提交材料'
  if (type === 'CONFIRM') return '部门确认完成'
  if (type === 'REJECT') return '部门退回'
  return '任务处理'
}

function formatDateTime(value) {
  if (!value) return ''
  return String(value).replace('T', ' ').slice(0, 16)
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
          <el-table-column prop="assignedDept" label="责任部门" min-width="140" />
          <el-table-column prop="currentDueDate" label="当前截止日期" min-width="140" align="center" />
          <el-table-column label="状态" width="155" align="center">
            <template #default="{ row }">
              <el-tag :type="taskTag(row).type" effect="plain">
                {{ taskTag(row).label }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="finishByName" label="确认人" min-width="120">
            <template #default="{ row }">
              {{ row.finishByName || '—' }}
            </template>
          </el-table-column>
          <el-table-column prop="finishTime" label="确认时间" min-width="165">
            <template #default="{ row }">
              {{ formatDateTime(row.finishTime) || '—' }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100" align="center" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link @click="openHistory(row)">处理记录</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <el-drawer
        v-model="historyVisible"
        title="任务处理记录"
        size="min(560px, 94vw)"
        class="task-detail-drawer"
      >
        <div v-loading="historyLoading" class="task-detail-drawer-body">
          <div v-if="historyTask" class="task-drawer-heading">
            <div>
              <span>任务 #{{ historyTask.taskId }}</span>
              <h3>{{ historyTask.taskName }}</h3>
            </div>
            <el-tag :type="taskTag(historyTask).type" effect="plain">
              {{ taskTag(historyTask).label }}
            </el-tag>
          </div>

          <section class="task-drawer-history">
            <el-empty
              v-if="!historyActions.length"
              :image-size="70"
              description="尚无提交或部门处理记录"
            />
            <ul v-else class="task-action-list">
              <li v-for="action in historyActions" :key="action.actionId">
                <span class="task-action-dot" :class="`is-${action.actionType.toLowerCase()}`" />
                <div class="task-action-copy">
                  <div class="task-action-head">
                    <strong>{{ actionLabel(action.actionType) }}</strong>
                    <time>{{ formatDateTime(action.actionTime) }}</time>
                  </div>
                  <p>{{ action.actorNameSnapshot }}</p>
                  <p v-if="action.reason" class="task-action-reason">{{ action.reason }}</p>
                  <p v-if="action.newDueDate" class="task-action-due">
                    新截止日期：{{ action.newDueDate }}
                  </p>
                  <div v-if="action.attachments.length" class="task-action-attachments">
                    <el-button
                      v-for="attachment in action.attachments"
                      :key="attachment.attachmentId"
                      type="primary"
                      link
                      :icon="Download"
                      @click="downloadAttachment(attachment)"
                    >
                      {{ attachment.originalName }}
                    </el-button>
                  </div>
                </div>
              </li>
            </ul>
          </section>
        </div>
      </el-drawer>
    </template>
  </section>
</template>
