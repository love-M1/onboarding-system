# 项目跟进记录

更新时间：2026-09-21

## 1. 本次会话目标

为现有新员工入职任务协同系统增加真实账号认证，形成完整操作闭环：

1. HR 使用手机号（工号）和密码登录。
2. HR 录入新员工信息并完成建档。
3. 系统根据任务模板自动生成该员工的入职任务。
4. 新员工使用 HR 建档时登记的手机号获取验证码并注册。
5. 注册账号自动绑定对应员工档案。
6. 员工登录后只查看并确认自己的任务。
7. HR 可在档案详情和进度汇总中查看任务完成结果。

## 2. 已确认的认证方案

- 手机号同时作为登录账号和工号。
- 密码使用 BCrypt 哈希保存，不保存明文。
- 公开注册仅允许创建 `EMPLOYEE` 账号。
- 员工注册手机号必须能在 HR 创建的员工档案中找到。
- HR 账号由后端配置初始化，不允许公开自助注册。
- 保留现有 `DEPARTMENT` 后端权限兼容能力，但不再提供该角色的演示登录入口。
- 登录成功后签发随机 Bearer Token，服务端只保存 Token 的 SHA-256 哈希。
- 普通业务接口不再信任 `X-Role`、`X-Department` 和 `X-Operator` 请求头。
- 演示环境可以让验证码接口返回 `devCode`，生产环境必须通过 `AUTH_EXPOSE_CODE=false` 关闭。

## 3. 已确认的任务权限

- `HR`：查看全部任务、维护模板、建档、查看统计和归档，但不能代替员工确认任务。
- `EMPLOYEE`：只能查看和确认自己档案下的任务。
- `DEPARTMENT`：保留历史接口兼容行为，只能处理本部门任务。

## 4. 相关设计文档

- 设计规格：`docs/superpowers/specs/2026-09-21-employee-auth-closed-loop-design.md`
- 实现计划：`docs/superpowers/plans/2026-09-21-employee-auth-closed-loop-implementation.md`
- 测试记录：`docs/TEST-CASES.md`
- 项目说明：`README.md`
- 启动说明：`启动说明.md`

后续修改认证、注册或任务权限时，应同时检查以上文档是否仍然一致。

## 5. Git 与远程仓库

- 本地分支：`main`
- 远程仓库：`https://github.com/love-M1/onboarding-system.git`
- 初始化提交：`e356683 初始化项目`
- 本地 `main` 已设置为跟踪 `origin/main`

常用命令：

```powershell
cd C:\Users\GuoZ\Desktop\onboarding-system-source-with-startup
git status
git add <需要提交的文件>
git commit -m "说明本次修改"
git push
```

## 6. 当前进度记录

- 已完成 Git 仓库初始化，并将初始项目快照推送到 GitHub。
- 已完成认证闭环设计规格和实现计划文档。
- 仓库中已经存在认证数据层、认证接口、前端登录注册页和对应测试的进行中实现。
- 截至本文档创建时，工作区仍有尚未提交的认证相关修改。最终完成状态必须以后端测试、前端构建和端到端验收结果为准，不能仅依据文件已经存在来判断。

## 7. 后续验收步骤

### 7.1 后端测试

```powershell
cd C:\Users\GuoZ\Desktop\onboarding-system-source-with-startup
mvn clean test
```

重点确认：

- HR 初始化账号可以登录。
- 未认证请求不能访问业务接口。
- HR 建档后能够生成任务。
- 未建档手机号不能申请验证码。
- 验证码错误、过期或重复使用时注册失败。
- 员工注册后自动绑定正确的员工档案。
- 员工只能查询和确认自己的任务。
- 注销后原 Token 失效。

### 7.2 前端构建

```powershell
cd C:\Users\GuoZ\Desktop\onboarding-system-source-with-startup\frontend
npm run build
```

### 7.3 端到端验收

1. 使用 HR 账号登录。
2. 创建一名新员工并确认任务自动生成。
3. 退出 HR 账号。
4. 使用新员工手机号申请验证码并完成注册。
5. 确认员工进入“我的任务”，且只看到自己的任务。
6. 确认完成一项任务。
7. 重新登录 HR，在档案详情和进度汇总中检查结果。

## 8. 恢复工作时的建议顺序

1. 执行 `git status`，确认主线程或其他人留下的未提交修改。
2. 阅读本文件、设计规格和实现计划。
3. 执行后端测试和前端构建，获取当前真实基线。
4. 修复失败项后再补充功能，不覆盖工作区中已有的未提交修改。
5. 完成端到端验收后，再按功能拆分提交并推送。

## 9. 安全注意事项

- 部署时通过 `AUTH_HR_PHONE`、`AUTH_HR_PASSWORD` 覆盖演示 HR 账号配置。
- 生产环境设置 `AUTH_EXPOSE_CODE=false`，防止接口直接返回验证码。
- 不要把真实数据库密码、短信服务密钥或 GitHub Token 写入源码。
- 不要恢复基于 `X-Role` 请求头的授权逻辑。
