import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import AppLayout from '../layouts/AppLayout.vue'
import LoginView from '../views/LoginView.vue'
import TemplateView from '../views/TemplateView.vue'
import EmployeeView from '../views/EmployeeView.vue'
import EmployeeDetailView from '../views/EmployeeDetailView.vue'
import TaskView from '../views/TaskView.vue'
import StatsView from '../views/StatsView.vue'
import ArchiveView from '../views/ArchiveView.vue'
import DepartmentOwnerView from '../views/DepartmentOwnerView.vue'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: LoginView,
    meta: { public: true, title: '登录' }
  },
  {
    path: '/',
    component: AppLayout,
    redirect: '/templates',
    children: [
      {
        path: 'templates',
        name: 'templates',
        component: TemplateView,
        meta: { title: '任务模板', roles: ['HR'] }
      },
      {
        path: 'employees',
        name: 'employees',
        component: EmployeeView,
        meta: { title: '入职档案', roles: ['HR'] }
      },
      {
        path: 'department-owners',
        name: 'department-owners',
        component: DepartmentOwnerView,
        meta: { title: '部门责任人', roles: ['HR'] }
      },
      {
        path: 'employees/:empId',
        name: 'employee-detail',
        component: EmployeeDetailView,
        meta: { title: '档案详情', roles: ['HR'] }
      },
      {
        path: 'tasks',
        name: 'tasks',
        component: TaskView,
        meta: { title: '任务清单', roles: ['HR', 'EMPLOYEE', 'DEPARTMENT'] }
      },
      {
        path: 'stats',
        name: 'stats',
        component: StatsView,
        meta: { title: '进度汇总', roles: ['HR'] }
      },
      {
        path: 'archive',
        name: 'archive',
        component: ArchiveView,
        meta: { title: '归档管理', roles: ['HR'] }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  document.title = `${to.meta.title || '工作台'} | 新员工入职任务协同系统`
  if (to.meta.public) {
    if (to.name === 'login' && auth.isLoggedIn) {
      return auth.homePath
    }
    return true
  }
  if (!auth.isLoggedIn) {
    return { name: 'login' }
  }
  if (to.meta.roles && !to.meta.roles.includes(auth.role)) {
    return auth.homePath
  }
  return true
})

export default router
