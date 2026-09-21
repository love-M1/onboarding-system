<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Calendar,
  Collection,
  DataAnalysis,
  FolderOpened,
  OfficeBuilding,
  SwitchButton,
  UserFilled
} from '@element-plus/icons-vue'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const menuItems = computed(() => {
  if (auth.isHr) {
    return [
      { path: '/templates', label: '任务模板', icon: Collection },
      { path: '/employees', label: '入职档案', icon: UserFilled },
      { path: '/stats', label: '进度汇总', icon: DataAnalysis },
      { path: '/archive', label: '归档管理', icon: FolderOpened }
    ]
  }
  if (auth.isEmployee) {
    return [
      { path: '/tasks', label: '我的任务', icon: Calendar }
    ]
  }
  return [
    { path: '/tasks', label: '部门任务', icon: Calendar }
  ]
})

const workspaceName = computed(() => {
  if (auth.isHr) return '人事工作台'
  if (auth.isEmployee) return '员工任务工作台'
  return '部门任务工作台'
})

const roleName = computed(() => {
  if (auth.isHr) return '人事管理员'
  if (auth.isEmployee) return '员工'
  return '部门责任人'
})

async function logout() {
  await auth.logout()
  router.replace('/login')
}
</script>

<template>
  <div class="app-shell">
    <aside class="app-sidebar">
      <div class="brand">
        <el-icon :size="21"><OfficeBuilding /></el-icon>
        <div>
          <strong>入职协同</strong>
          <span>ONBOARDING WORKSPACE</span>
        </div>
      </div>

      <nav class="app-menu" aria-label="主导航">
        <RouterLink
          v-for="item in menuItems"
          :key="item.path"
          :to="item.path"
          class="menu-item"
          :class="{ active: route.path.startsWith(item.path) }"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </RouterLink>
      </nav>

      <div class="sidebar-footer">
        <span class="sidebar-avatar">{{ auth.operator.slice(0, 1) }}</span>
        <div>
          <strong>{{ roleName }}</strong>
          <span>{{ auth.isHr ? auth.department : workspaceName }}</span>
        </div>
      </div>
    </aside>

    <section class="app-main">
      <header class="app-header">
        <div class="header-context">
          <span>入职协同</span>
          <i>/</i>
          <strong>{{ workspaceName }}</strong>
        </div>
        <div class="header-actions">
          <div class="user-chip">
            <span class="user-avatar">{{ auth.operator.slice(0, 1) }}</span>
            <div>
              <strong>{{ auth.operator }}</strong>
              <span>{{ auth.isHr ? '全部权限' : roleName }}</span>
            </div>
          </div>
          <el-button class="logout-button" :icon="SwitchButton" plain @click="logout">
            退出
          </el-button>
        </div>
      </header>

      <main class="page-content">
        <RouterView />
      </main>
    </section>
  </div>
</template>
