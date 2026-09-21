<script setup>
import { computed, onBeforeUnmount, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Iphone, Key, Lock, OfficeBuilding } from '@element-plus/icons-vue'
import { requestCode } from '../api/auth'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()
const mode = ref('login')
const sendingCode = ref(false)
const submitting = ref(false)
const countdown = ref(0)
let countdownTimer = null

const loginForm = reactive({
  phone: '',
  password: ''
})

const registerForm = reactive({
  phone: '',
  code: '',
  password: '',
  confirmPassword: ''
})

const phonePattern = /^1[3-9]\d{9}$/
const canSendCode = computed(() => (
  phonePattern.test(registerForm.phone.trim()) && countdown.value === 0 && !sendingCode.value
))

function startCountdown(seconds) {
  countdown.value = seconds
  window.clearInterval(countdownTimer)
  countdownTimer = window.setInterval(() => {
    countdown.value -= 1
    if (countdown.value <= 0) {
      window.clearInterval(countdownTimer)
      countdownTimer = null
      countdown.value = 0
    }
  }, 1000)
}

async function sendCode() {
  const phone = registerForm.phone.trim()
  if (!phonePattern.test(phone)) {
    ElMessage.warning('请输入正确的11位手机号')
    return
  }
  sendingCode.value = true
  try {
    const result = await requestCode(phone)
    startCountdown(result.resendAfter || 60)
    if (result.devCode) {
      registerForm.code = result.devCode
      ElMessage.success(`开发验证码：${result.devCode}`)
    } else {
      ElMessage.success('验证码已发送')
    }
  } finally {
    sendingCode.value = false
  }
}

async function submitLogin() {
  if (!phonePattern.test(loginForm.phone.trim())) {
    ElMessage.warning('请输入正确的11位手机号')
    return
  }
  if (!loginForm.password) {
    ElMessage.warning('请输入密码')
    return
  }
  submitting.value = true
  try {
    await auth.login({
      phone: loginForm.phone.trim(),
      password: loginForm.password
    })
    ElMessage.success('登录成功')
    router.replace(auth.homePath)
  } finally {
    submitting.value = false
  }
}

async function submitRegister() {
  const phone = registerForm.phone.trim()
  if (!phonePattern.test(phone)) {
    ElMessage.warning('请输入正确的11位手机号')
    return
  }
  if (!/^\d{6}$/.test(registerForm.code.trim())) {
    ElMessage.warning('请输入6位验证码')
    return
  }
  if (registerForm.password !== registerForm.confirmPassword) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }
  submitting.value = true
  try {
    await auth.register({
      phone,
      code: registerForm.code.trim(),
      password: registerForm.password
    })
    ElMessage.success('注册成功')
    router.replace(auth.homePath)
  } finally {
    submitting.value = false
  }
}

onBeforeUnmount(() => {
  window.clearInterval(countdownTimer)
})
</script>

<template>
  <main class="login-page">
    <section class="login-intro">
      <div class="login-brand">
        <div class="login-mark">
          <el-icon :size="24"><OfficeBuilding /></el-icon>
        </div>
        <div>
          <strong>入职协同</strong>
          <span>ONBOARDING WORKSPACE</span>
        </div>
      </div>
      <p class="login-kicker">标准化 · 可追踪 · 可归档</p>
      <h1>新员工入职任务协同系统</h1>
      <p class="login-copy">
        从任务模板、员工建档到个人任务确认、进度汇总和档案归档，所有节点在同一套流程中完成。
      </p>
      <dl class="login-facts">
        <div>
          <dt>自动生成</dt>
          <dd>HR 建档后立即生成个人任务</dd>
        </div>
        <div>
          <dt>个人任务</dt>
          <dd>员工登录后只查看自己的待办</dd>
        </div>
        <div>
          <dt>闭环归档</dt>
          <dd>全部完成后方可归档并转为只读</dd>
        </div>
      </dl>
    </section>

    <section class="login-panel">
      <div class="login-panel-header">
        <p>账号验证</p>
        <h2>{{ mode === 'login' ? '登录工作台' : '注册员工账号' }}</h2>
      </div>

      <el-radio-group v-model="mode" size="large" class="auth-mode-switch">
        <el-radio-button label="login">登录</el-radio-button>
        <el-radio-button label="register">注册</el-radio-button>
      </el-radio-group>

      <div v-if="mode === 'login'" class="auth-form">
        <label class="login-field">
          <span>手机号（工号）</span>
          <el-input
            v-model="loginForm.phone"
            size="large"
            maxlength="11"
            :prefix-icon="Iphone"
            placeholder="请输入手机号"
            @keyup.enter="submitLogin"
          />
        </label>
        <label class="login-field">
          <span>密码</span>
          <el-input
            v-model="loginForm.password"
            size="large"
            type="password"
            show-password
            :prefix-icon="Lock"
            placeholder="请输入密码"
            @keyup.enter="submitLogin"
          />
        </label>
        <el-button
          class="auth-submit"
          type="primary"
          size="large"
          :loading="submitting"
          @click="submitLogin"
        >
          登录
        </el-button>
      </div>

      <div v-else class="auth-form">
        <label class="login-field">
          <span>手机号（工号）</span>
          <el-input
            v-model="registerForm.phone"
            size="large"
            maxlength="11"
            :prefix-icon="Iphone"
            placeholder="请输入 HR 建档手机号"
          />
        </label>
        <label class="login-field">
          <span>短信验证码</span>
          <div class="verification-row">
            <el-input
              v-model="registerForm.code"
              size="large"
              maxlength="6"
              :prefix-icon="Key"
              placeholder="6位验证码"
            />
            <el-button
              size="large"
              :disabled="!canSendCode"
              :loading="sendingCode"
              @click="sendCode"
            >
              {{ countdown > 0 ? `${countdown} 秒` : '获取验证码' }}
            </el-button>
          </div>
        </label>
        <label class="login-field">
          <span>密码</span>
          <el-input
            v-model="registerForm.password"
            size="large"
            type="password"
            show-password
            :prefix-icon="Lock"
            placeholder="8至32位，包含字母和数字"
          />
        </label>
        <label class="login-field">
          <span>确认密码</span>
          <el-input
            v-model="registerForm.confirmPassword"
            size="large"
            type="password"
            show-password
            :prefix-icon="Lock"
            placeholder="再次输入密码"
            @keyup.enter="submitRegister"
          />
        </label>
        <el-button
          class="auth-submit"
          type="primary"
          size="large"
          :loading="submitting"
          @click="submitRegister"
        >
          完成注册
        </el-button>
      </div>
    </section>
  </main>
</template>
