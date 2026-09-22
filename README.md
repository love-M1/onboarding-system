# 新员工入职任务协同系统

本项目按《新员工入职任务协同系统系统设计文档》实现，采用 Spring Boot、MyBatis 和 MySQL 提供后端接口，采用 Vue 3、Vite、Pinia、Axios 和 Element Plus 实现前端页面。

## 已实现功能

- 人事管理员维护任务模板，支持任务名称和部门筛选。
- 人事管理员为每个部门维护最多一个启用责任人账号，支持新增、修改、启停和重置密码。
- 新增员工档案时自动读取全部模板，按模板责任部门分配任务，并保存责任部门和截止日期快照。
- 模板为空时仍可建档，并明确提示未生成任务。
- 手机号（工号）验证码注册、密码登录、Bearer Token 会话和注销。
- 员工注册时自动绑定 HR 已建档的员工记录。
- 员工仅查询本人任务，提交备注和 JPG、PNG、PDF 附件。
- 部门责任人仅处理本部门任务，可查看附件、确认完成，或填写原因和新截止日期后退回。
- 被退回任务可由员工重新提交，提交、退回和确认历史永久保留。
- 任务状态固定为待员工处理、待部门确认、已完成和已退回四种，只有部门确认完成后才计入完成进度。
- 逾期状态由数据库查询实时派生，截止日当天不计入逾期。
- 按员工、按责任部门汇总任务总数、完成数、待完成数和逾期数。
- 全部任务确认完成后才可归档；待员工处理、待部门确认或已退回任务都会阻止归档和删除。
- 归档档案保持只读，不再允许确认任务或删除。
- 附件保存在服务端私有目录，通过带权限校验的下载接口访问，不暴露静态文件地址。
- 统一响应结构、业务错误码、参数校验、角色校验和异常日志。

## 工程结构

```text
onboarding-system/
├─ frontend/                 Vue 3 frontend
├─ scripts/                  Windows PowerShell helpers
├─ sql/onboarding_sys.sql    MySQL schema and seed templates
├─ src/main/java/            Spring Boot source
├─ src/main/resources/       application.yml and MyBatis XML
├─ src/test/java/            service and workflow tests
├─ src/test/resources/       H2 test profile and schema
├─ docs/TEST-CASES.md        test case record
└─ pom.xml                   Maven build
```

## 环境要求

- Windows 10 or Windows 11
- JDK 21
- Maven 3.9 or later
- Node.js 20 or later
- npm 10 or later
- MySQL 8.0

## 1. 初始化数据库

默认连接参数为 `127.0.0.1:3306`、用户 `root`、数据库 `onboarding_sys`。脚本不会在源码中保存密码。

```powershell
cd <解压后的项目目录>
$env:DB_PASSWORD = "你的 MySQL 密码"
.\scripts\init-db.ps1
```

如果使用其他连接参数：

```powershell
.\scripts\init-db.ps1 -DbHost 127.0.0.1 -Port 3306 -User root -Password "你的 MySQL 密码"
```

也可以使用 `-DbName` 指定数据库名。脚本会重新创建五张业务表和三张认证相关表，并写入 5 条演示任务模板。

## 2. 启动后端

新开一个 PowerShell 窗口：

```powershell
cd <解压后的项目目录>
$env:DB_PASSWORD = "你的 MySQL 密码"
.\scripts\run-backend.ps1
```

后端地址为 `http://127.0.0.1:8080`。数据库连接也可直接通过环境变量配置：

```powershell
$env:DB_URL = "jdbc:mysql://127.0.0.1:3306/onboarding_sys?serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true"
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "你的 MySQL 密码"
mvn spring-boot:run
```

首次启动会创建一个 HR 演示账号，默认参数如下：

- 手机号：`13800000000`
- 密码：`Admin@123`

部署时应通过 `AUTH_HR_PHONE`、`AUTH_HR_PASSWORD` 和 `AUTH_HR_NAME` 覆盖默认参数。

任务附件默认保存在项目下的 `uploads/onboarding`。部署时可通过环境变量指定其他非公开目录：

```powershell
$env:ONBOARDING_UPLOAD_DIR = "D:\onboarding-data\uploads"
```

单个附件最大 10 MB，单次最多 10 个，仅支持 JPG、JPEG、PNG 和 PDF。

## 3. 启动前端

再新开一个 PowerShell 窗口：

```powershell
cd <解压后的项目目录>
.\scripts\run-frontend.ps1
```

脚本在缺少 `node_modules` 时自动执行 `npm install`。前端地址为 `http://127.0.0.1:5173`，Vite 已将 `/api` 代理到 `http://127.0.0.1:8080`。

## 4. 注册和登录

### HR 登录

- 使用初始化账号登录。
- 维护模板、登记档案、查看汇总、归档和删除。

### 员工注册

1. HR 先创建员工档案，手机号必须唯一且格式正确。
2. 员工在登录页切换到注册，输入该手机号并获取验证码。
3. 演示环境默认返回验证码并自动回填；生产环境应设置 `AUTH_EXPOSE_CODE=false`。
4. 设置符合要求的密码后完成注册，系统自动绑定员工档案并登录。

### 员工登录

员工使用手机号和密码登录后，只能查看自己的任务并提交办理材料。材料提交后状态变为“待部门确认”，由模板指定的责任部门处理。

员工进度汇总支持按姓名远程搜索，不限制为前 100 条档案。

### 部门责任人登录

1. HR 在“部门责任人”页面新增账号，每个部门最多一个启用账号。
2. 部门责任人使用 HR 设置的手机号和初始密码登录。
3. 部门责任人只能查看分配给本部门的任务，并确认完成或退回补交。

### 最终操作闭环

1. HR 维护任务模板，确定任务责任部门和相对入职日的截止天数。
2. HR 建立员工档案，系统自动生成任务并保存责任部门和截止日期快照。
3. 员工登录后上传材料，任务进入“待部门确认”。
4. 部门责任人核对材料，确认完成后任务进入“已完成”；材料不符时退回并设置新的补交截止日期。
5. 员工按退回意见重新提交，部门再次处理。
6. 员工全部任务确认完成后，HR 才能归档档案。

## 5. 测试与构建

后端测试使用 H2 的 MySQL 兼容模式，不依赖本机业务数据库：

```powershell
mvn clean test
```

前端生产构建：

```powershell
cd frontend
npm install
npm test
npm run build
```

前端测试覆盖任务状态映射、真实文件提交参数、部门责任人请求参数、确认框取消处理和日期格式化。后端测试覆盖认证持久化、会话、验证码、注册登录、令牌授权、部门责任人管理、任务快照、材料上传、退回重交、部门确认、并发状态保护、归档限制和逾期边界。

当前基线共执行 59 个后端测试和 17 个前端测试，全部通过；`mvn clean test` 固定使用 H2 测试配置，不依赖本机 MySQL 密码。

## 接口前缀

所有接口统一使用 `/api`，响应格式如下：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {}
}
```

除验证码申请、注册和登录外，业务接口都必须携带：

```text
Authorization: Bearer <token>
```

账号、验证码和会话分别存储在 `user_account`、`phone_verification` 和 `auth_session` 表中。密码仅保存 BCrypt 哈希。

## 常见问题

### 后端提示 Access denied

确认 `DB_USERNAME` 和 `DB_PASSWORD` 与当前 MySQL 实例一致。源码不会保存真实密码。

### 前端显示网络连接失败

确认后端已监听 `8080`，并检查 Vite 所在终端是否正常输出 `http://127.0.0.1:5173`。

### 端口被占用

后端可通过 `SERVER_PORT` 修改端口。前端固定使用 `5173`，Vite 会在该端口被占用时停止启动；请先关闭占用 `5173` 的程序，不要改用其他端口。
