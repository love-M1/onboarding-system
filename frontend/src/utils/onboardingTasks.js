export const ONBOARDING_TASKS = [
  {
    code: 'materials',
    name: '提交入职材料',
    shortName: '入职材料',
    badge: '第 1 步',
    category: '资料准备',
    department: '人事部',
    duration: '约 15 分钟',
    summary: '完成身份、学历与银行卡资料的线上提交，人事审核通过后会收到确认通知。',
    image: 'https://images.unsplash.com/photo-1450101499163-c8848c66ca85?auto=format&fit=crop&w=1200&q=80',
    imageAlt: '办公桌整理入职纸质材料',
    accent: '#3370ff',
    guide: [
      { title: '核对个人信息', detail: '确认姓名、身份证号、手机号与录用通知一致。' },
      { title: '准备清晰原件', detail: '拍照时保持四角完整、无反光，单张文件不超过 10 MB。' },
      { title: '上传并提交', detail: '按材料类型上传，人事将在 1 个工作日内完成审核。' }
    ],
    materials: [
      { name: '身份证正反面', format: 'JPG / PNG / PDF', required: true },
      { name: '最高学历证明', format: 'PDF', required: true },
      { name: '银行卡正面', format: 'JPG / PNG', required: true }
    ],
    contact: '人事部 · 李老师',
    note: '请保证姓名与证件完全一致，银行卡需为本人可正常使用的一类账户。'
  },
  {
    code: 'contract',
    name: '签署劳动合同',
    shortName: '劳动合同',
    badge: '第 2 步',
    category: '合同签署',
    department: '人事部',
    duration: '约 20 分钟',
    summary: '核对岗位、薪酬、试用期与工作地点等关键条款后完成电子签署。',
    image: 'https://images.unsplash.com/photo-1554224155-6726b3ff858f?auto=format&fit=crop&w=1200&q=80',
    imageAlt: '桌面上的劳动合同与计算器',
    accent: '#00a870',
    guide: [
      { title: '阅读完整合同', detail: '重点确认岗位名称、合同期限、薪资构成和工作地点。' },
      { title: '处理疑问条款', detail: '签约前可联系 HR 说明疑问，确认后再进行签署。' },
      { title: '完成签署留存', detail: '签署后下载 PDF 副本，平台会同步保存电子版。' }
    ],
    materials: [
      { name: '已签署劳动合同', format: 'PDF', required: true },
      { name: '合同疑问确认记录', format: '图片 / PDF', required: false }
    ],
    contact: '人事部 · 王老师',
    note: '电子签署具有法律效力，请勿代签或使用昵称签署。'
  },
  {
    code: 'badge',
    name: '办理员工工牌',
    shortName: '员工工牌',
    badge: '第 3 步',
    category: '门禁与身份',
    department: '行政部',
    duration: '约 10 分钟',
    summary: '提交工牌照片并确认领取方式，用于办公区门禁、打印与访客登记。',
    image: 'https://images.unsplash.com/photo-1556742049-0cfed4f6a45d?auto=format&fit=crop&w=1200&q=80',
    imageAlt: '员工在办公前台办理证件',
    accent: '#ff7d00',
    guide: [
      { title: '准备证件照', detail: '使用近半年正面免冠照片，背景简洁，不要使用自拍滤镜。' },
      { title: '填写领取信息', detail: '选择现场领取或由部门接口人代领。' },
      { title: '领取后激活', detail: '收到工牌后先在门禁机刷卡一次，确认权限已生效。' }
    ],
    materials: [
      { name: '一寸证件照', format: 'JPG / PNG', required: true },
      { name: '工牌领取方式确认', format: '在线确认', required: true }
    ],
    contact: '行政部 · 前台服务台',
    note: '工牌仅限本人使用，如遗失请立即联系行政部挂失。'
  },
  {
    code: 'email',
    name: '开通企业邮箱',
    shortName: '企业邮箱',
    badge: '第 4 步',
    category: '账号开通',
    department: '信息技术部',
    duration: '约 8 分钟',
    summary: '确认邮箱命名规则并完成首次登录，用于接收工作通知与项目往来邮件。',
    image: 'https://images.unsplash.com/photo-1497366754035-f200968a6e72?auto=format&fit=crop&w=1200&q=80',
    imageAlt: '现代办公室中的笔记本电脑',
    accent: '#7b61ff',
    guide: [
      { title: '确认邮箱地址', detail: '默认按“姓名拼音@公司域名”生成，重名时由 IT 追加数字。' },
      { title: '激活初始账号', detail: '在 24 小时内使用临时密码登录并修改为强密码。' },
      { title: '绑定安全验证', detail: '完成手机或验证器绑定，避免异地登录时被拦截。' }
    ],
    materials: [
      { name: '邮箱地址确认单', format: '在线确认', required: true },
      { name: '首登成功截图', format: 'PNG / JPG', required: true }
    ],
    contact: '信息技术部 · 服务台',
    note: '不要在邮件中发送密码、身份证号等敏感信息。'
  },
  {
    code: 'equipment',
    name: '领取办公设备',
    shortName: '办公设备',
    badge: '第 5 步',
    category: '设备领用',
    department: '行政部',
    duration: '约 12 分钟',
    summary: '确认电脑、配件和耗材清单，完成资产签收与基础网络测试。',
    image: 'https://images.unsplash.com/photo-1517336714731-489689fd1ca8?auto=format&fit=crop&w=1200&q=80',
    imageAlt: '桌面上的办公笔记本电脑',
    accent: '#e5484d',
    guide: [
      { title: '当面核对设备', detail: '确认设备型号、序列号和配件数量与领用单一致。' },
      { title: '完成开机检查', detail: '连接公司 Wi-Fi，测试摄像头、麦克风和常用办公软件。' },
      { title: '上传签收凭证', detail: '在资产系统签署领用单，并将照片提交至本任务。' }
    ],
    materials: [
      { name: '设备领用签收单', format: 'PDF / JPG', required: true },
      { name: '设备外观照片', format: 'JPG / PNG', required: false }
    ],
    contact: '行政部 · 资产管理员',
    note: '请妥善保管公司资产，离职时需按清单归还全部设备与配件。'
  }
]

export function buildTaskRoster(records = []) {
  const exactMatches = new Map()

  ONBOARDING_TASKS.forEach((definition, index) => {
    const record = records.find((item) => item.taskName === definition.name)
    if (record) exactMatches.set(index, record)
  })

  const usedTaskIds = new Set(
    [...exactMatches.values()].map((record) => String(record.taskId))
  )

  return ONBOARDING_TASKS.map((definition, index) => {
    let record = exactMatches.get(index)

    if (!record) {
      record = records.find((item) => !usedTaskIds.has(String(item.taskId)))
      if (record) usedTaskIds.add(String(record.taskId))
    }

    return {
      ...definition,
      taskId: record?.taskId ?? `demo-${definition.code}`,
      taskStatus: record?.taskStatus ?? 0,
      dueDate: record?.dueDate ?? '',
      overdue: Boolean(record?.overdue),
      canFinish: record?.canFinish ?? true,
      archived: Boolean(record?.archived)
    }
  })
}

export function formatFileSize(bytes) {
  const size = Number(bytes) || 0
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / 1024 / 1024).toFixed(1)} MB`
}

export function getTaskProgress(roster = [], submissions = {}) {
  const total = roster.length
  const completed = roster.filter((task) => (
    task.taskStatus === 1 || submissions[task.taskId]?.submitted
  )).length

  return {
    completed,
    total,
    percent: total ? Math.round((completed / total) * 100) : 0
  }
}

export async function submitTaskMaterials(task, submission, finishTask) {
  if (!task || !submission?.files.length) {
    return false
  }

  await finishTask(task.taskId)
  submission.submitted = true
  submission.submittedAt = new Date().toLocaleString('zh-CN', {
    hour12: false,
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
  return true
}
