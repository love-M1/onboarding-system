# 新员工入职任务协同系统

本项目按《新员工入职任务协同系统系统设计文档》实现，采用 Spring Boot、MyBatis 和 MySQL 提供后端接口，采用 Vue 3、Vite、Pinia、Axios 和 Element Plus 实现前端页面。

## 已实现功能

- 人事管理员维护任务模板，支持任务名称和部门筛选。
- 新增员工档案时自动读取全部模板，在同一事务中批量生成个人任务。
- 模板为空时仍可建档，并明确提示未生成任务。
- 部门责任人仅查询和确认本部门任务。
- 应完成日期由入职时间和模板偏移天数实时计算。
- 逾期状态由数据库查询实时派生，截止日当天不计入逾期。
- 按员工、按责任部门汇总任务总数、完成数、待完成数和逾期数。
- 全部任务完成后方可归档；存在未完成任务时禁止归档或删除。
- 归档档案保持只读，不再允许确认任务或删除。
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

也可以使用 `-DbName` 指定数据库名。脚本会重新创建三张业务表，并写入 5 条演示任务模板。

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

## 3. 启动前端

再新开一个 PowerShell 窗口：

```powershell
cd <解压后的项目目录>
.\scripts\run-frontend.ps1
```

脚本在缺少 `node_modules` 时自动执行 `npm install`。前端地址为 `http://127.0.0.1:5173`，Vite 已将 `/api` 代理到 `http://127.0.0.1:8080`。

## 4. 角色登录

- 人事管理员：维护模板、登记档案、查看汇总、归档和删除。
- 部门责任人：选择部门后进入系统，只显示并确认该部门任务。

课程设计演示采用角色选择登录，不提交真实账号和密码。写操作仍在后端执行角色校验。

员工进度汇总支持按姓名远程搜索，不限制为前 100 条档案。

## 5. 测试与构建

后端测试使用 H2 的 MySQL 兼容模式，不依赖本机业务数据库：

```powershell
mvn clean test
```

前端生产构建：

```powershell
cd frontend
npm install
npm run build
```

测试覆盖建档事务、模板校验、任务生成、重复确认、未到入职日期确认、归档任务确认、归档限制、删除限制和逾期边界。

当前基线共执行 19 个后端测试，`ApplicationTests` 固定使用 H2 测试配置，不依赖本机 MySQL 密码。

## 接口前缀

所有接口统一使用 `/api`，响应格式如下：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {}
}
```

前端 Axios 会自动携带以下请求头：

- `X-Role`: `HR` 或 `DEPARTMENT`
- `X-Department`: 部门责任人所属部门，使用 URL 编码传输
- `X-Operator`: 操作人显示名称，使用 URL 编码传输

## 常见问题

### 后端提示 Access denied

确认 `DB_USERNAME` 和 `DB_PASSWORD` 与当前 MySQL 实例一致。源码不会保存真实密码。

### 前端显示网络连接失败

确认后端已监听 `8080`，并检查 Vite 所在终端是否正常输出 `http://127.0.0.1:5173`。

### 端口被占用

后端可通过 `SERVER_PORT` 修改端口。前端固定使用 `5173`，Vite 会在该端口被占用时停止启动；请先关闭占用 `5173` 的程序，不要改用其他端口。
