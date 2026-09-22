<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Check,
  CircleCheck,
  Clock,
  Delete,
  Document,
  Download,
  Files,
  InfoFilled,
  OfficeBuilding,
  Refresh,
  Search,
  UploadFilled,
  User,
  WarningFilled
} from '@element-plus/icons-vue'
import {
  confirmTask,
  downloadTaskAttachment,
  getTaskDetail,
  getTasks,
  rejectTask,
  submitTask
} from '../api/tasks'
import { useAuthStore } from '../stores/auth'
import {
  buildTaskRoster,
  formatFileSize,
  getTaskProgress,
  getTaskStatusMeta,
  submitTaskMaterials
} from '../utils/onboardingTasks'
import { waitForConfirmation } from '../utils/uiState'

const auth = useAuthStore()
const loading = ref(false)
const detailLoading = ref(false)
const records = ref([])
const total = ref(0)
const fileInput = ref(null)
const selectedTaskId = ref('')
const reviewingTaskId = ref('')
const detailVisible = ref(false)
const submittingTaskId = ref('')
const submissions = reactive({})
const detailCache = reactive({})
const filters = reactive({
  empId: '',
  department: '',
  status: '',
  pageNum: 1,
  pageSize: 10
})
const rejectDialog = reactive({
  visible: false,
  task: null,
  reason: '',
  newDueDate: ''
})

const pageTitle = computed(() => {
  if (auth.isEmployee) return '我的任务'
  if (auth.isDepartment) return '部门任务处理'
  return '任务清单'
})
const showFilters = computed(() => !auth.isEmployee)
const taskRoster = computed(() => buildTaskRoster(records.value))
const selectedTask = computed(() => (
  taskRoster.value.find((task) => String(task.taskId) === selectedTaskId.value)
  || taskRoster.value[0]
))
const selectedSubmission = computed(() => (
  selectedTask.value ? submissionFor(selectedTask.value.taskId) : null
))
const selectedTaskDetail = computed(() => (
  selectedTask.value ? detailCache[detailKey(selectedTask.value.taskId)] : null
))
const selectedActions = computed(() => selectedTaskDetail.value?.actions || [])
const latestSelectedRejection = computed(() => (
  [...selectedActions.value].reverse().find((action) => action.actionType === 'REJECT')
))
const employeeProgress = computed(() => getTaskProgress(taskRoster.value))
const reviewTask = computed(() => (
  detailCache[detailKey(reviewingTaskId.value)]?.task
  || records.value.find((task) => String(task.taskId) === reviewingTaskId.value)
))
const reviewActions = computed(() => (
  detailCache[detailKey(reviewingTaskId.value)]?.actions || []
))
const latestReviewRejection = computed(() => (
  [...reviewActions.value].reverse().find((action) => action.actionType === 'REJECT')
))

function detailKey(taskId) {
  return String(taskId)
}

function submissionFor(taskId) {
  const key = detailKey(taskId)
  if (!submissions[key]) {
    submissions[key] = {
      files: [],
      note: ''
    }
  }
  return submissions[key]
}

function clearSubmission(taskId) {
  const submission = submissionFor(taskId)
  submission.files.splice(0)
  submission.note = ''
}

function taskStatusMeta(task) {
  return getTaskStatusMeta(task)
}

function isTaskDone(task) {
  return Number(task?.taskStatus) === 2
}

function isEntryReached(task) {
  if (!task?.entryTime) return true
  const entryDate = String(task.entryTime).slice(0, 10)
  return entryDate <= formatDateInput(new Date())
}

function canSubmitTask(task) {
  if (!auth.isEmployee || !task || task.archived || !isEntryReached(task)) return false
  return [0, 3].includes(Number(task.taskStatus))
}

function canReviewTask(task) {
  return auth.isDepartment && Number(task?.taskStatus) === 1 && !task?.archived
}

function selectTask(task) {
  selectedTaskId.value = detailKey(task.taskId)
  submissionFor(task.taskId)
  loadTaskDetail(task.taskId)
}

async function loadTaskDetail(taskId) {
  if (!taskId) return null
  detailLoading.value = true
  try {
    const detail = await getTaskDetail(taskId)
    detailCache[detailKey(taskId)] = detail
    return detail
  } catch {
    return null
  } finally {
    detailLoading.value = false
  }
}

async function openTaskDetail(task) {
  reviewingTaskId.value = detailKey(task.taskId)
  detailVisible.value = true
  await loadTaskDetail(task.taskId)
}

function openFilePicker() {
  if (!canSubmitTask(selectedTask.value)) {
    ElMessage.info(submitDisabledReason(selectedTask.value) || '当前任务不能提交材料')
    return
  }
  fileInput.value?.click()
}

function addFiles(fileList) {
  if (!selectedTask.value) return
  const submission = submissionFor(selectedTask.value.taskId)
  const incoming = Array.from(fileList || [])
  const accepted = []
  let invalidTypeCount = 0
  let rejectedSizeCount = 0
  let duplicateCount = 0
  let overLimitCount = 0

  incoming.forEach((file) => {
    const extension = file.name.split('.').pop()?.toLowerCase()
    if (!['jpg', 'jpeg', 'png', 'pdf'].includes(extension)) {
      invalidTypeCount += 1
      return
    }
    if (file.size > 10 * 1024 * 1024) {
      rejectedSizeCount += 1
      return
    }
    const duplicate = [...submission.files, ...accepted].some((item) => (
      item.name === file.name
      && item.size === file.size
      && item.lastModified === file.lastModified
    ))
    if (duplicate) {
      duplicateCount += 1
      return
    }
    if (submission.files.length + accepted.length >= 10) {
      overLimitCount += 1
      return
    }
    accepted.push(file)
  })

  submission.files.push(...accepted)

  if (invalidTypeCount) {
    ElMessage.warning(`${invalidTypeCount} 个文件格式不支持，仅可上传 JPG、PNG、PDF`)
  }
  if (rejectedSizeCount) {
    ElMessage.warning(`${rejectedSizeCount} 个文件超过 10 MB，未加入提交清单`)
  }
  if (overLimitCount) {
    ElMessage.warning('每次最多上传 10 个附件')
  }
  if (duplicateCount) {
    ElMessage.info(`${duplicateCount} 个重复文件已忽略`)
  }
  if (accepted.length) {
    ElMessage.success(`已选择 ${accepted.length} 个材料文件`)
  }
}

function handleFileChange(event) {
  addFiles(event.target.files)
  event.target.value = ''
}

function handleDrop(event) {
  addFiles(event.dataTransfer?.files)
}

function removeFile(index) {
  selectedSubmission.value?.files.splice(index, 1)
}

async function submitMaterials() {
  const task = selectedTask.value
  const submission = selectedSubmission.value
  if (!canSubmitTask(task)) {
    ElMessage.warning(submitDisabledReason(task) || '当前任务不能提交材料')
    return
  }
  if (!submission?.files.length) {
    ElMessage.warning('请先选择需要提交的材料文件')
    return
  }

  submittingTaskId.value = detailKey(task.taskId)
  try {
    await submitTaskMaterials(task, submission, submitTask)
    ElMessage.success('材料已提交，等待部门责任人确认')
    clearSubmission(task.taskId)
    await loadData()
  } catch {
    // The HTTP interceptor already presents the server error.
  } finally {
    submittingTaskId.value = ''
  }
}

async function confirmSelectedTask() {
  const task = reviewTask.value
  if (!canReviewTask(task)) return
  const confirmed = await waitForConfirmation(() => ElMessageBox.confirm(
    `确认“${task.taskName}”已经办理完成？确认后将计入员工完成进度。`,
    '确认完成',
    { type: 'warning', confirmButtonText: '确认完成', cancelButtonText: '取消' }
  ))
  if (!confirmed) return

  try {
    await confirmTask(task.taskId)
    ElMessage.success('任务已确认完成')
    await loadData()
    await loadTaskDetail(task.taskId)
  } catch {
    // The HTTP interceptor already presents the server error.
  }
}

function openRejectDialog(task) {
  rejectDialog.task = task
  rejectDialog.reason = ''
  rejectDialog.newDueDate = defaultNewDueDate()
  rejectDialog.visible = true
}

async function submitRejection() {
  const task = rejectDialog.task
  const reason = rejectDialog.reason.trim()
  if (!reason) {
    ElMessage.warning('请填写退回原因')
    return
  }
  if (!rejectDialog.newDueDate) {
    ElMessage.warning('请选择新的截止日期')
    return
  }
  if (rejectDialog.newDueDate < formatDateInput(new Date())) {
    ElMessage.warning('新截止日期不能早于今天')
    return
  }

  try {
    await rejectTask(task.taskId, {
      reason,
      newDueDate: rejectDialog.newDueDate
    })
    ElMessage.success('任务已退回，员工可重新提交材料')
    rejectDialog.visible = false
    await loadData()
    await loadTaskDetail(task.taskId)
  } catch {
    // The HTTP interceptor already presents the server error.
  }
}

async function downloadAttachment(taskId, attachment) {
  try {
    const blob = await downloadTaskAttachment(taskId, attachment.attachmentId)
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

async function loadData() {
  loading.value = true
  try {
    const data = await getTasks({
      empId: filters.empId || undefined,
      department: filters.department || undefined,
      status: filters.status || undefined,
      pageNum: auth.isEmployee ? 1 : filters.pageNum,
      pageSize: auth.isEmployee ? 100 : filters.pageSize
    })
    records.value = data.list
    total.value = data.total

    if (!records.value.some((task) => detailKey(task.taskId) === selectedTaskId.value)) {
      selectedTaskId.value = detailKey(records.value[0]?.taskId || '')
    }
    if (auth.isEmployee && records.value[0]) {
      await loadTaskDetail(selectedTaskId.value)
    }
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

function submitDisabledReason(task) {
  if (!task) return '请选择任务'
  if (task.archived) return '档案已归档，不能修改任务状态'
  if (!isEntryReached(task)) return '入职日期尚未到来，暂不能提交'
  if (Number(task.taskStatus) === 1) return '材料已提交，等待部门确认'
  if (Number(task.taskStatus) === 2) return '任务已确认完成'
  return ''
}

function actionMeta(type) {
  if (type === 'SUBMIT') {
    return { label: '员工提交材料', type: 'primary' }
  }
  if (type === 'CONFIRM') {
    return { label: '部门确认完成', type: 'success' }
  }
  if (type === 'REJECT') {
    return { label: '部门退回', type: 'warning' }
  }
  return { label: '任务处理', type: 'info' }
}

function formatDateTime(value) {
  if (!value) return ''
  return String(value).replace('T', ' ').slice(0, 16)
}

function formatDateInput(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function defaultNewDueDate() {
  const date = new Date()
  date.setDate(date.getDate() + 3)
  return formatDateInput(date)
}

function disabledDate(date) {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return date.getTime() < today.getTime()
}

onMounted(loadData)
</script>

<template>
  <section class="page-section">
    <div class="section-heading">
      <div>
        <h2>{{ pageTitle }}</h2>
        <p v-if="auth.isEmployee">依次办理入职任务，提交材料后由责任部门确认完成。</p>
        <p v-else-if="auth.isDepartment">
          仅显示 {{ auth.department }} 负责的任务，可查看材料、确认完成或退回补交。
        </p>
        <p v-else>只读查看任务状态、部门处理记录与逾期情况。</p>
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
        <el-option label="待员工处理" value="0" />
        <el-option label="待部门确认" value="1" />
        <el-option label="已完成" value="2" />
        <el-option label="已退回" value="3" />
        <el-option label="已逾期" value="overdue" />
      </el-select>
      <el-button type="primary" :icon="Search" @click="search">查询</el-button>
      <el-button :icon="Refresh" @click="resetFilters">重置</el-button>
    </div>

    <div v-if="auth.isEmployee" v-loading="loading" class="employee-task-workspace">
      <aside class="employee-task-rail">
        <div class="employee-task-rail-head">
          <div>
            <span>入职办理清单</span>
            <strong>{{ employeeProgress.completed }} / {{ employeeProgress.total }} 已完成</strong>
          </div>
          <el-progress
            :percentage="employeeProgress.percent"
            :show-text="false"
            :stroke-width="7"
          />
        </div>

        <div class="employee-task-rail-list">
          <button
            v-for="(task, index) in taskRoster"
            :key="task.taskId"
            type="button"
            class="employee-task-rail-item"
            :class="{
              active: String(task.taskId) === String(selectedTask?.taskId),
              complete: isTaskDone(task),
              returned: Number(task.taskStatus) === 3,
              overdue: task.overdue
            }"
            @click="selectTask(task)"
          >
            <span class="employee-task-order">{{ String(index + 1).padStart(2, '0') }}</span>
            <span class="employee-task-rail-copy">
              <strong>{{ task.shortName }}</strong>
              <small>{{ taskStatusMeta(task).label }}</small>
            </span>
            <el-icon>
              <CircleCheck v-if="isTaskDone(task)" />
              <Clock v-else-if="Number(task.taskStatus) === 1" />
              <WarningFilled v-else-if="Number(task.taskStatus) === 3 || task.overdue" />
              <Document v-else />
            </el-icon>
          </button>
          <el-empty v-if="!taskRoster.length" description="暂无入职任务" />
        </div>

        <div class="employee-task-rail-tip">
          <el-icon><InfoFilled /></el-icon>
          <span>只有部门责任人确认完成后，任务才会计入完成进度。</span>
        </div>
      </aside>

      <section v-if="selectedTask" class="employee-task-detail">
        <div class="employee-task-hero">
          <img :src="selectedTask.image" :alt="selectedTask.imageAlt" />
          <div class="employee-task-hero-shade"></div>
          <div class="employee-task-hero-copy">
            <div class="employee-task-eyebrow">
              <span>{{ selectedTask.badge }}</span>
              <span>{{ selectedTask.category }}</span>
            </div>
            <h3>{{ selectedTask.name }}</h3>
            <p>{{ selectedTask.summary }}</p>
          </div>
          <el-tag
            class="employee-task-hero-tag"
            :type="taskStatusMeta(selectedTask).type"
            effect="dark"
          >
            {{ taskStatusMeta(selectedTask).label }}
          </el-tag>
        </div>

        <div class="employee-task-facts">
          <article>
            <el-icon><Clock /></el-icon>
            <span>当前截止日期</span>
            <strong>{{ selectedTask.dueDate || '未设置' }}</strong>
          </article>
          <article>
            <el-icon><OfficeBuilding /></el-icon>
            <span>责任部门</span>
            <strong>{{ selectedTask.assignedDept }}</strong>
          </article>
          <article>
            <el-icon><User /></el-icon>
            <span>当前状态</span>
            <strong :title="taskStatusMeta(selectedTask).label">
              {{ taskStatusMeta(selectedTask).label }}
            </strong>
          </article>
        </div>

        <div class="employee-task-content-grid">
          <section class="employee-task-guide">
            <div class="employee-task-panel-head">
              <div>
                <span>办理指引</span>
                <h3>按步骤准备材料</h3>
              </div>
              <el-tag effect="plain">{{ selectedTask.guide.length }} 个步骤</el-tag>
            </div>

            <ol class="employee-guide-list">
              <li v-for="(step, index) in selectedTask.guide" :key="step.title">
                <span>{{ String(index + 1).padStart(2, '0') }}</span>
                <div>
                  <strong>{{ step.title }}</strong>
                  <p>{{ step.detail }}</p>
                </div>
              </li>
            </ol>

            <div class="employee-task-note">
              <el-icon><InfoFilled /></el-icon>
              <div>
                <strong>办理提醒</strong>
                <p>{{ selectedTask.note }}</p>
              </div>
            </div>

            <section class="employee-task-history">
              <div class="employee-task-panel-head">
                <div>
                  <span>处理记录</span>
                  <h3>{{ selectedActions.length }} 次操作</h3>
                </div>
              </div>
              <el-empty
                v-if="!selectedActions.length"
                :image-size="70"
                description="尚无提交或部门处理记录"
              />
              <ul v-else class="task-action-list">
                <li v-for="action in selectedActions" :key="action.actionId">
                  <span class="task-action-dot" :class="`is-${action.actionType.toLowerCase()}`" />
                  <div class="task-action-copy">
                    <div class="task-action-head">
                      <strong>{{ actionMeta(action.actionType).label }}</strong>
                      <time>{{ formatDateTime(action.actionTime) }}</time>
                    </div>
                    <p>{{ action.actorNameSnapshot }}</p>
                    <p v-if="action.reason" class="task-action-reason">
                      {{ action.reason }}
                    </p>
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
                        @click="downloadAttachment(selectedTask.taskId, attachment)"
                      >
                        {{ attachment.originalName }}
                      </el-button>
                    </div>
                  </div>
                </li>
              </ul>
            </section>
          </section>

          <aside class="employee-material-panel">
            <div class="employee-task-panel-head">
              <div>
                <span>材料提交</span>
                <h3>
                  {{ Number(selectedTask.taskStatus) === 3 ? '补充并重新提交' : '上传办理材料' }}
                </h3>
              </div>
              <el-tag :type="taskStatusMeta(selectedTask).type" effect="plain">
                {{ taskStatusMeta(selectedTask).label }}
              </el-tag>
            </div>

            <el-alert
              v-if="selectedTask.archived"
              title="档案已归档，任务不可再修改"
              type="info"
              :closable="false"
              show-icon
            />

            <div v-if="latestSelectedRejection" class="task-return-alert">
              <el-icon><WarningFilled /></el-icon>
              <div>
                <strong>材料已退回，请按意见补交</strong>
                <p>{{ latestSelectedRejection.reason }}</p>
                <span>新截止日期：{{ latestSelectedRejection.newDueDate }}</span>
              </div>
            </div>

            <template v-if="canSubmitTask(selectedTask)">
              <input
                ref="fileInput"
                class="employee-material-input"
                type="file"
                multiple
                accept=".jpg,.jpeg,.png,.pdf"
                @change="handleFileChange"
              />
              <div
                class="employee-material-dropzone"
                role="button"
                tabindex="0"
                @click="openFilePicker"
                @keydown.enter="openFilePicker"
                @keydown.space.prevent="openFilePicker"
                @dragover.prevent
                @drop.prevent="handleDrop"
              >
                <el-icon><UploadFilled /></el-icon>
                <strong>点击选择或拖拽材料</strong>
                <span>支持 JPG、PNG、PDF，单个文件不超过 10 MB，最多 10 个</span>
              </div>

              <div v-if="selectedSubmission?.files.length" class="employee-material-files">
                <div
                  v-for="(file, index) in selectedSubmission.files"
                  :key="`${file.name}-${file.size}-${file.lastModified}`"
                >
                  <span class="employee-file-icon"><el-icon><Files /></el-icon></span>
                  <span class="employee-file-copy">
                    <strong>{{ file.name }}</strong>
                    <small>{{ formatFileSize(file.size) }}</small>
                  </span>
                  <el-button
                    type="danger"
                    link
                    :icon="Delete"
                    @click.stop="removeFile(index)"
                  />
                </div>
              </div>

              <el-input
                v-model="selectedSubmission.note"
                type="textarea"
                :rows="3"
                maxlength="500"
                show-word-limit
                placeholder="选填：补充材料说明，或回应部门退回意见"
              />

              <el-button
                type="primary"
                :icon="UploadFilled"
                :loading="submittingTaskId === String(selectedTask.taskId)"
                :disabled="
                  submittingTaskId === String(selectedTask.taskId)
                  || !selectedSubmission?.files.length
                "
                @click="submitMaterials"
              >
                {{ Number(selectedTask.taskStatus) === 3 ? '重新提交材料' : '提交材料' }}
              </el-button>
            </template>

            <div v-else class="employee-submit-state">
              <el-icon>
                <CircleCheck v-if="Number(selectedTask.taskStatus) === 2" />
                <Clock v-else-if="Number(selectedTask.taskStatus) === 1" />
                <WarningFilled v-else />
              </el-icon>
              <strong>{{ taskStatusMeta(selectedTask).label }}</strong>
              <p>{{ submitDisabledReason(selectedTask) }}</p>
            </div>

            <p class="employee-submit-time">
              提交后由部门责任人在系统内核对材料，未确认前不会计入完成进度。
            </p>
          </aside>
        </div>
      </section>

      <el-empty v-else description="当前没有入职任务" />
    </div>

    <template v-else>
      <el-table v-loading="loading" :data="records" border stripe class="task-table-desktop">
        <el-table-column prop="taskId" label="任务编号" width="105" align="center" />
        <el-table-column prop="empName" label="员工" min-width="110" />
        <el-table-column prop="taskName" label="任务名称" min-width="190" />
        <el-table-column prop="assignedDept" label="责任部门" min-width="130" />
        <el-table-column prop="currentDueDate" label="当前截止日期" min-width="145" align="center" />
        <el-table-column label="状态" width="165" align="center">
          <template #default="{ row }">
            <el-tag :type="taskStatusMeta(row).type" effect="plain">
              {{ taskStatusMeta(row).label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="125" align="center" fixed="right">
          <template #default="{ row }">
            <el-button
              :type="canReviewTask(row) ? 'primary' : 'default'"
              :link="!canReviewTask(row)"
              @click="openTaskDetail(row)"
            >
              {{ canReviewTask(row) ? '处理' : '查看' }}
            </el-button>
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
            <el-tag :type="taskStatusMeta(task).type" effect="plain">
              {{ taskStatusMeta(task).label }}
            </el-tag>
          </div>
          <dl class="task-mobile-meta">
            <div>
              <dt>员工</dt>
              <dd>{{ task.empName }}</dd>
            </div>
            <div>
              <dt>责任部门</dt>
              <dd>{{ task.assignedDept }}</dd>
            </div>
            <div>
              <dt>当前截止日期</dt>
              <dd>{{ task.currentDueDate }}</dd>
            </div>
          </dl>
          <div class="task-mobile-action">
            <el-button
              :type="canReviewTask(task) ? 'primary' : 'default'"
              :plain="canReviewTask(task)"
              @click="openTaskDetail(task)"
            >
              {{ canReviewTask(task) ? '处理任务' : '查看详情' }}
            </el-button>
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
    </template>

    <el-drawer
      v-model="detailVisible"
      title="任务处理详情"
      size="min(620px, 94vw)"
      class="task-detail-drawer"
    >
      <div v-loading="detailLoading" class="task-detail-drawer-body">
        <template v-if="reviewTask">
          <div class="task-drawer-heading">
            <div>
              <span>{{ reviewTask.empName }} · 任务 #{{ reviewTask.taskId }}</span>
              <h3>{{ reviewTask.taskName }}</h3>
            </div>
            <el-tag :type="taskStatusMeta(reviewTask).type" effect="plain">
              {{ taskStatusMeta(reviewTask).label }}
            </el-tag>
          </div>

          <dl class="task-drawer-meta">
            <div>
              <dt>责任部门</dt>
              <dd>{{ reviewTask.assignedDept }}</dd>
            </div>
            <div>
              <dt>原始截止日期</dt>
              <dd>{{ reviewTask.baseDueDate }}</dd>
            </div>
            <div>
              <dt>当前截止日期</dt>
              <dd>{{ reviewTask.currentDueDate }}</dd>
            </div>
            <div>
              <dt>确认人</dt>
              <dd>{{ reviewTask.finishByName || '尚未确认' }}</dd>
            </div>
            <div>
              <dt>确认时间</dt>
              <dd>{{ formatDateTime(reviewTask.finishTime) || '尚未确认' }}</dd>
            </div>
          </dl>

          <div v-if="latestReviewRejection" class="task-return-alert">
            <el-icon><WarningFilled /></el-icon>
            <div>
              <strong>最近一次退回</strong>
              <p>{{ latestReviewRejection.reason }}</p>
              <span>要求补交截止日期：{{ latestReviewRejection.newDueDate }}</span>
            </div>
          </div>

          <section class="task-drawer-history">
            <div class="task-drawer-section-title">
              <div>
                <span>操作历史</span>
                <h3>提交、退回与确认记录</h3>
              </div>
              <el-tag effect="plain">{{ reviewActions.length }} 条</el-tag>
            </div>
            <el-empty
              v-if="!reviewActions.length"
              :image-size="70"
              description="员工尚未提交材料"
            />
            <ul v-else class="task-action-list">
              <li v-for="action in reviewActions" :key="action.actionId">
                <span class="task-action-dot" :class="`is-${action.actionType.toLowerCase()}`" />
                <div class="task-action-copy">
                  <div class="task-action-head">
                    <strong>{{ actionMeta(action.actionType).label }}</strong>
                    <time>{{ formatDateTime(action.actionTime) }}</time>
                  </div>
                  <p>{{ action.actorNameSnapshot }}</p>
                  <p v-if="action.reason" class="task-action-reason">
                    {{ action.reason }}
                  </p>
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
                      @click="downloadAttachment(reviewTask.taskId, attachment)"
                    >
                      {{ attachment.originalName }}
                    </el-button>
                  </div>
                </div>
              </li>
            </ul>
          </section>
        </template>
      </div>

      <template v-if="canReviewTask(reviewTask)" #footer>
        <div class="task-review-actions">
          <el-button type="danger" plain @click="openRejectDialog(reviewTask)">
            退回并要求补交
          </el-button>
          <el-button type="primary" :icon="Check" @click="confirmSelectedTask">
            确认完成
          </el-button>
        </div>
      </template>
    </el-drawer>

    <el-dialog v-model="rejectDialog.visible" title="退回任务" width="520px">
      <el-form label-position="top">
        <el-form-item label="任务">
          <el-input :model-value="rejectDialog.task?.taskName" disabled />
        </el-form-item>
        <el-form-item label="退回原因">
          <el-input
            v-model="rejectDialog.reason"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="说明材料缺失、内容不清晰或需要补充的信息"
          />
        </el-form-item>
        <el-form-item label="新的补交截止日期">
          <el-date-picker
            v-model="rejectDialog.newDueDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择日期"
            :disabled-date="disabledDate"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectDialog.visible = false">取消</el-button>
        <el-button type="danger" @click="submitRejection">确认退回</el-button>
      </template>
    </el-dialog>
  </section>
</template>
