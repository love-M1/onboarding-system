# 项目跟进记录

更新时间：2026-09-22

## 1. 最终目标

把原有“员工直接确认任务”的实现升级为真实业务闭环：

1. HR 维护任务模板，并管理每个部门的责任人账号。
2. HR 建立员工档案，系统按模板自动分配责任部门并计算截止日期。
3. 员工上传办理材料，任务进入待部门确认状态。
4. 部门责任人核对材料，确认完成或退回补交。
5. 员工根据退回意见重新提交，全部历史保留。
6. 所有任务确认完成后，HR 才能归档档案。

## 2. 已确认的业务规则

- 任务状态：`0=待员工处理`、`1=待部门确认`、`2=已完成`、`3=已退回`。
- `assignedDept`、`baseDueDate` 和 `currentDueDate` 在建档时按模板快照写入。
- 每个部门最多一个启用责任人；删除责任人采用软停用。
- 员工提交必须包含至少一个 JPG、JPEG、PNG 或 PDF 文件。
- 单文件最大 10 MB，单次最多 10 个附件，备注最多 500 字。
- 部门退回必须填写原因和新的截止日期，新日期不能早于今天。
- 按时退回不会自动逾期，页面显示“已退回 · 待补交”。
- 原截止日已过、员工本次提交迟交，或新截止日已过未重交时才计逾期。
- 截止日当天不计逾期，次日起才计逾期。
- 只有状态 2 计入完成进度；状态 0、1、3 都会阻止归档和删除。
- HR 只读查看任务，不能代替部门责任人确认或退回。

## 3. 已交付的后端能力

- `emp_task` 保存责任部门、原始截止日期、当前截止日期、确认人和乐观锁版本。
- `task_action` 永久保存 `SUBMIT`、`CONFIRM`、`REJECT` 操作历史。
- `task_attachment` 保存附件元数据，文件落在配置的私有目录。
- 任务详情返回状态、逾期标识、确认信息、操作历史和附件下载地址。
- 员工提交：`POST /api/tasks/{taskId}/submissions`。
- 部门确认：`POST /api/tasks/{taskId}/confirm`。
- 部门退回：`POST /api/tasks/{taskId}/reject`。
- 附件下载：`GET /api/tasks/{taskId}/attachments/{attachmentId}`。
- 部门责任人管理：`/api/department-owners` 的增改、停用和密码重置。
- 旧的 `/api/tasks/{taskId}/finish` 旁路已删除，避免跳过员工提交和部门审核。

## 4. 已交付的前端能力

- HR 导航新增“部门责任人”管理页。
- 员工任务工作台保留原有布局，改为完全由后端任务记录驱动。
- 员工可上传真实附件、重新提交，并查看退回原因、截止日期和处理历史。
- 部门责任人可按部门查询任务，在详情抽屉查看附件并确认或退回。
- HR 在任务清单、档案详情和进度汇总中只读查看最终状态。
- 逾期任务可通过状态筛选查询。
- 接口错误由统一拦截器显示，页面反馈与后端返回保持一致。

## 5. 关键文件

- 设计规格：`docs/superpowers/specs/2026-09-22-hr-employee-department-confirmation-design.md`
- 实施计划：`docs/superpowers/plans/2026-09-22-hr-employee-department-confirmation-implementation.md`
- 测试记录：`docs/TEST-CASES.md`
- 项目说明：`README.md`
- 启动说明：`启动说明.md`
- 数据库脚本：`sql/onboarding_sys.sql`
- 附件目录配置：`ONBOARDING_UPLOAD_DIR`，默认 `uploads/onboarding`

## 6. 当前验证基线

```powershell
mvn clean test
cd frontend
npm test
npm run build
```

自动化验证结果以 `docs/TEST-CASES.md` 为准。最终推送前应重新执行以上三条命令，并完成 HR 建档、员工提交、部门退回、员工重提、部门确认和 HR 归档的浏览器验收。

## 7. Git 与远程仓库

- 远程仓库：`https://github.com/love-M1/onboarding-system.git`
- 本次实现分支：`codex/qa-fixes-and-auth-flow`
- 提交时必须包含后端、前端、SQL、启动说明、测试记录和本跟进文档。

常用命令：

```powershell
git status
git add <需要提交的文件>
git commit -m "说明本次修改"
git push
```

## 8. 部署注意事项

- 通过 `AUTH_HR_PHONE`、`AUTH_HR_PASSWORD` 和 `AUTH_HR_NAME` 覆盖演示 HR 账号。
- 生产环境设置 `AUTH_EXPOSE_CODE=false`。
- 通过 `ONBOARDING_UPLOAD_DIR` 指向持久化且不可公开访问的附件目录。
- 不要把真实数据库密码、短信服务密钥或 GitHub Token 写入源码。
- 初始化脚本会重建业务表；已有生产数据时不得直接执行。
