<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Check,
  CircleCheck,
  Clock,
  Delete,
  Document,
  Files,
  InfoFilled,
  OfficeBuilding,
  Refresh,
  Search,
  UploadFilled,
  User
} from '@element-plus/icons-vue'
import { getTasks, finishTask } from '../api/tasks'
import { useAuthStore } from '../stores/auth'
import {
  buildTaskRoster,
  formatFileSize,
  getTaskProgress,
  submitTaskMaterials
} from '../utils/onboardingTasks'
import { waitForConfirmation } from '../utils/uiState'

const auth = useAuthStore()
const loading = ref(false)
const records = ref([])
const total = ref(0)
const fileInput = ref(null)
const selectedTaskId = ref('')
const submittingTaskId = ref('')
const submissions = reactive({})
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
const taskRoster = computed(() => buildTaskRoster(records.value))
const selectedTask = computed(() => (
  taskRoster.value.find((task) => String(task.taskId) === selectedTaskId.value)
  || taskRoster.value[0]
))
const selectedSubmission = computed(() => (
  selectedTask.value ? submissionFor(selectedTask.value.taskId) : null
))
const employeeProgress = computed(() => getTaskProgress(taskRoster.value, submissions))

function submissionFor(taskId) {
  const key = String(taskId)
  if (!submissions[key]) {
    submissions[key] = {
      files: [],
      note: '',
      submitted: false,
      submittedAt: ''
    }
  }
  return submissions[key]
}

function isTaskDone(task) {
  return task.taskStatus === 1 || Boolean(submissions[String(task.taskId)]?.submitted)
}

function selectTask(task) {
  selectedTaskId.value = String(task.taskId)
  submissionFor(task.taskId)
}

function openFilePicker() {
  if (isTaskDone(selectedTask.value)) {
    ElMessage.info('该任务已提交，当前为展示状态')
    return
  }
  fileInput.value?.click()
}

function addFiles(fileList) {
  if (!selectedTask.value) return
  const submission = submissionFor(selectedTask.value.taskId)
  const incoming = Array.from(fileList || [])
  const accepted = []
  let rejectedSizeCount = 0

  incoming.forEach((file) => {
    if (file.size > 10 * 1024 * 1024) {
      rejectedSizeCount += 1
      return
    }
    const duplicate = submission.files.some((item) => (
      item.name === file.name && item.size === file.size
    ))
    if (!duplicate) {
      accepted.push({
        name: file.name,
        size: file.size,
        type: file.type
      })
    }
  })

  submission.files.push(...accepted)
  submission.submitted = false

  if (rejectedSizeCount) {
    ElMessage.warning(`${rejectedSizeCount} 个文件超过 10 MB，未加入展示清单`)
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
  if (!selectedSubmission.value) return
  selectedSubmission.value.files.splice(index, 1)
  selectedSubmission.value.submitted = false
  selectedSubmission.value.submittedAt = ''
}

async function submitMaterials() {
  const task = selectedTask.value
  const submission = selectedSubmission.value
  if (!task || !submission?.files.length) {
    ElMessage.warning('请先选择需要提交的材料文件')
    return
  }

  submittingTaskId.value = String(task.taskId)
  try {
    await submitTaskMaterials(task, submission, finishTask)
    ElMessage.success('材料提交成功，任务进度已同步给人事管理员')
    await loadData()
  } catch (error) {
    if (!error?.code) {
      ElMessage.error(error?.message || '材料提交失败，请稍后重试')
    }
  } finally {
    submittingTaskId.value = ''
  }
}

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
    if (!selectedTaskId.value) {
      selectedTaskId.value = String(taskRoster.value[0]?.taskId || '')
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
        <p v-if="auth.isEmployee">从左侧依次办理 5 项入职任务，查看指引并提交材料。</p>
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
              complete: isTaskDone(task)
            }"
            @click="selectTask(task)"
          >
            <span class="employee-task-order">{{ String(index + 1).padStart(2, '0') }}</span>
            <span class="employee-task-rail-copy">
              <strong>{{ task.shortName }}</strong>
              <small v-if="isTaskDone(task)">材料已提交</small>
              <small v-else-if="task.dueDate">应完成 {{ task.dueDate }}</small>
              <small v-else>待办理</small>
            </span>
            <el-icon>
              <CircleCheck v-if="isTaskDone(task)" />
              <Document v-else />
            </el-icon>
          </button>
        </div>

        <div class="employee-task-rail-tip">
          <el-icon><InfoFilled /></el-icon>
          <span>提交后任务进度会实时同步给人事管理员。</span>
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
            :type="isTaskDone(selectedTask) ? 'success' : 'warning'"
            effect="dark"
          >
            {{ isTaskDone(selectedTask) ? '已完成' : '待办理' }}
          </el-tag>
        </div>

        <div class="employee-task-facts">
          <article>
            <el-icon><Clock /></el-icon>
            <span>预计用时</span>
            <strong>{{ selectedTask.duration }}</strong>
          </article>
          <article>
            <el-icon><OfficeBuilding /></el-icon>
            <span>责任部门</span>
            <strong>{{ selectedTask.department }}</strong>
          </article>
          <article>
            <el-icon><User /></el-icon>
            <span>对接人</span>
            <strong>{{ selectedTask.contact }}</strong>
          </article>
        </div>

        <div class="employee-task-content-grid">
          <section class="employee-task-guide">
            <div class="employee-task-panel-head">
              <div>
                <span>办理指引</span>
                <h3>按步骤完成，减少来回沟通</h3>
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
          </section>

          <aside class="employee-material-panel">
            <div class="employee-task-panel-head">
              <div>
                <span>材料提交</span>
                <h3>{{ selectedTask.materials.length }} 项材料</h3>
              </div>
              <el-tag v-if="selectedSubmission?.submitted" type="success" effect="plain">
                已提交
              </el-tag>
            </div>

            <ul class="employee-material-list">
              <li v-for="material in selectedTask.materials" :key="material.name">
                <span class="employee-material-icon">
                  <Check v-if="selectedSubmission?.files.length" />
                  <Document v-else />
                </span>
                <div>
                  <strong>{{ material.name }}</strong>
                  <span>{{ material.format }}</span>
                </div>
                <em v-if="material.required">必交</em>
              </li>
            </ul>

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
              :class="{ disabled: isTaskDone(selectedTask) }"
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
              <span>支持 JPG、PNG、PDF，单个文件不超过 10 MB</span>
            </div>

            <div v-if="selectedSubmission?.files.length" class="employee-material-files">
              <div v-for="(file, index) in selectedSubmission.files" :key="`${file.name}-${index}`">
                <span class="employee-file-icon"><el-icon><Files /></el-icon></span>
                <span class="employee-file-copy">
                  <strong>{{ file.name }}</strong>
                  <small>{{ formatFileSize(file.size) }}</small>
                </span>
                <el-button
                  type="danger"
                  link
                  :icon="Delete"
                  :disabled="selectedSubmission.submitted"
                  @click.stop="removeFile(index)"
                />
              </div>
            </div>

            <el-input
              v-model="selectedSubmission.note"
              type="textarea"
              :rows="3"
              maxlength="120"
              show-word-limit
              placeholder="补充说明，例如证书预计补交时间"
              :disabled="selectedSubmission?.submitted"
            />

            <el-button
              type="primary"
              :icon="UploadFilled"
              :loading="submittingTaskId === String(selectedTask.taskId)"
              :disabled="
                submittingTaskId === String(selectedTask.taskId)
                || !selectedSubmission?.files.length
                || selectedSubmission?.submitted
                || selectedTask.taskStatus === 1
              "
              @click="submitMaterials"
            >
              {{
                selectedSubmission?.submitted || selectedTask.taskStatus === 1
                  ? '材料已提交'
                  : '提交材料'
              }}
            </el-button>
            <p v-if="selectedSubmission?.submitted" class="employee-submit-time">
              已于 {{ selectedSubmission.submittedAt }} 提交，任务进度已同步
            </p>
            <p class="employee-demo-note">仅作功能演示，不会上传或长期保存文件。</p>
          </aside>
        </div>
      </section>
    </div>

    <template v-else>
      <el-table v-loading="loading" :data="records" border stripe class="task-table-desktop">
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
            <div>
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
    </template>
  </section>
</template>
