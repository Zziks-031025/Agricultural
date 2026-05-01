# 基于区块链的农产品溯源系统

## 项目概述

本项目基于 Spring Boot + Vue 2 + 微信小程序 + 以太坊区块链（Ganache 本地测试网）构建的农产品全链路溯源系统。支持养殖、加工、检疫三类企业的全流程溯源管理，通过 Solidity 智能合约实现数据上链存证与防篡改验证。

---

## 一、模块说明

```
ProductTraceability_2026/
├── 01_Database/              # 数据库初始化脚本（建库建表 + 初始数据）
├── 02_Backend/               # Spring Boot 后端服务（API + 区块链交互）
├── 03_Frontend_Web/          # Vue 2 PC 管理端（平台管理员 + 企业用户）
├── 04_Frontend_MiniApp/      # 微信小程序（消费者扫码溯源 + 企业移动端）
├── 05_SmartContract/         # Solidity 智能合约源码 + ABI 文件
└── doc/                      # 接口文档（openapi.yaml）、数据库脚本
```

### 各模块职责

| 模块 | 技术 | 职责 |
|------|------|------|
| 01_Database | MySQL 8.0 | 系统所有表结构及初始化数据 |
| 02_Backend | Spring Boot 2.7 / Web3j | 提供 REST API，负责业务逻辑和区块链上链 |
| 03_Frontend_Web | Vue 2 / Element UI | PC 端管理后台，企业数据录入、审核、统计 |
| 04_Frontend_MiniApp | 微信小程序原生 | 消费者扫码查询溯源链路，企业移动端录入 |
| 05_SmartContract | Solidity ^0.8.0 | 链上存证合约，存储各环节数据哈希，防篡改 |

### 智能合约核心函数

| 函数 | 说明 |
|------|------|
| `uploadHash(traceId, dataHash)` | 上传溯源数据哈希到链上（每个环节调用一次） |
| `getHash(traceId)` | 查询链上溯源记录 |
| `verifyHash(traceId, dataHash)` | 验证数据哈希是否与链上一致 |
| `traceExists(traceId)` | 检查溯源 ID 是否已上链 |
| `getTotalRecords()` | 获取链上总记录数 |
| `setAuthorizedOperator(addr, bool)` | 授权/取消操作者地址 |

---

## 二、环境要求

| 环境 | 版本要求 | 验证命令 |
|------|---------|---------|
| JDK | 1.8（Java 8） | `java -version` |
| Maven | 3.6+ | `mvn -v` |
| Node.js | 14.x 以上（推荐 16.x LTS） | `node -v` |
| MySQL | 8.0 | `mysql --version` |
| Ganache | 2.7+（桌面版） | 打开 Ganache 应用 |
| 微信开发者工具 | 最新稳定版 | 打开应用 |
| 浏览器 | Chrome（用于 Remix） | — |

### 关键端口

| 端口 | 服务 |
|------|------|
| 3306 | MySQL |
| 8545 | Ganache RPC |
| 8888 | Spring Boot 后端 |
| 8080 | Vue 开发服务器 |

---

## 三、部署步骤（按顺序执行）

### 步骤 1：启动 Ganache

Ganache 提供以太坊本地测试网络，用于合约部署和上链操作。

1. 打开 Ganache 桌面应用
2. 点击 **QUICKSTART (ETHEREUM)**
3. 确认 RPC Server 显示为 `HTTP://127.0.0.1:8545`
4. 记录 **ACCOUNTS** 页面第一个账户的地址（格式：`0x...`），后续配置需要

> Ganache 必须在整个系统运行期间保持开启。关闭后链上数据丢失（桌面版可使用 Workspace 持久化）。

---

### 步骤 2：部署智能合约（Remix）

使用 Remix 在线 IDE 将合约部署到 Ganache 本地网络，并获取合约地址和 ABI。

**2.1 编译合约**

1. 浏览器打开 https://remix.ethereum.org/
2. 左侧 **File Explorer** 面板，点击新建文件图标，创建 `Traceability.sol`
3. 将 `05_SmartContract/Traceability.sol` 的全部内容粘贴进去
4. 左侧切换到 **Solidity Compiler** 标签（图标为 S 形）
5. Compiler 版本选择 `0.8.x`（任意 0.8 版本均可）
6. 点击 **Compile Traceability.sol**，编译成功后图标变为绿色

**2.2 连接 Ganache**

1. 左侧切换到 **Deploy & Run Transactions** 标签（图标为以太坊菱形）
2. **ENVIRONMENT** 下拉框选择 `Custom - External Http Provider`
3. 弹窗中填入 `http://127.0.0.1:8545`，点击 **OK**
4. 连接成功后 **ACCOUNT** 下拉框会自动加载 Ganache 中的账户列表
5. 选择第一个账户（与步骤 1 记录的地址一致）

**2.3 部署合约**

1. **CONTRACT** 下拉框确认选中 `Traceability`
2. 点击橙色 **Deploy** 按钮
3. 部署成功后，页面下方 **Deployed Contracts** 区域出现合约条目
4. 点击合约条目左侧的复制图标，复制合约地址（格式：`0x...`）

> 记录下：
> - 合约地址（contract-address）
> - 部署时使用的账户地址（account-address，即步骤 1 记录的地址）

**2.4 获取合约 ABI**

ABI 是后端与合约交互的接口描述文件，项目已提供 `05_SmartContract/contract_abi.json`，无需重新获取。

如需自行获取：
1. 在 Remix **Solidity Compiler** 标签页，编译完成后
2. 点击 **ABI** 按钮（位于 Compile 按钮下方）即可复制
3. 将内容保存为 `contract_abi.json` 替换 `05_SmartContract/` 下的文件

---

### 步骤 3：初始化数据库

```bash
# 登录 MySQL
mysql -u root -p

# 创建数据库
CREATE DATABASE agricultural DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# 退出 mysql 后执行导入
mysql -u root -p agricultural < 01_Database/agricultural_database.sql
```

默认数据库连接信息：

| 项目 | 值 |
|------|---|
| 地址 | `localhost:3306` |
| 数据库名 | `agricultural` |
| 用户名 | `root` |
| 密码 | `123456` |

---

### 步骤 4：配置并启动后端

**4.1 修改配置文件**

编辑 `02_Backend/src/main/resources/application.yml`，填入步骤 2 获得的合约地址和账户地址：

```yaml
spring:
  datasource:
    password: ${DB_PASSWORD:123456}       # 改为你的 MySQL 密码

blockchain:
  rpc-url: http://127.0.0.1:8545
  contract-address: "0x你的合约地址"        # 步骤 2.3 复制的合约地址
  account-address: "0x你的账户地址"         # 步骤 1 记录的 Ganache 账户地址
```

**4.2 启动后端**

```bash
cd 02_Backend

# 首次运行需下载依赖，耗时较长
mvn clean compile

# 启动
mvn spring-boot:run
```

启动成功标志：控制台输出 `Tomcat started on port(s): 8888`

---

### 步骤 5：启动 PC 前端

```bash
cd 03_Frontend_Web

# 首次运行安装依赖
npm install

# 启动开发服务器
npm run dev
```

启动后访问 http://localhost:8080

PC 前端通过 `vue.config.js` 中的 devServer proxy 将 `/api`、`/auth`、`/uploads` 请求代理到后端 `http://localhost:8888`，开发环境无需 Nginx。

**默认账号：**

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 平台管理员 | `admin` | `admin123` |

---

### 步骤 6：启动微信小程序

**6.1 配置后端地址**

编辑 `04_Frontend_MiniApp/config.js`：

```javascript
const ENV_CONFIG = {
  dev: {
    // 修改为当前电脑局域网 IP（cmd 运行 ipconfig 查看 IPv4 地址）
    // 小程序真机调试不能使用 localhost
    API_BASE_URL: 'http://你的局域网IP:8888',
  },
}
```

**6.2 导入项目**

1. 打开微信开发者工具 → 导入项目
2. 目录选择 `04_Frontend_MiniApp`
3. AppID 填入 `wxf24401df8316ca97` 或使用测试号

**6.3 关闭域名校验（开发调试）**

工具栏 → 详情 → 本地设置 → 勾选 **不校验合法域名、web-view（业务域名）、TLS 版本以及 HTTPS 证书**

---

## 四、技术栈

### 后端

| 技术 | 版本 |
|------|------|
| Java | 1.8 |
| Spring Boot | 2.7.18 |
| MyBatis Plus | 3.5.3.1 |
| MySQL Connector | 8.0.33 |
| Druid | 1.2.20 |
| Web3j | 4.9.8 |
| JJWT | 0.11.5 |

### PC 前端

| 技术 | 版本 |
|------|------|
| Vue | 2.6.14 |
| Element UI | 2.15.13 |
| Axios | 1.6.2 |
| ECharts | 5.4.3 |
| Web3 | 1.10.3 |

### 区块链

| 技术 | 说明 |
|------|------|
| Solidity | ^0.8.0 |
| Ganache | 2.7+ 本地以太坊测试网 |
| Remix IDE | https://remix.ethereum.org/ |

---

## 五、数据上链机制

每个溯源环节在创建记录时自动上链，各环节拥有独立的 `tx_hash`。

**上链流程：**

1. 后端根据业务数据计算 SHA-256 哈希值（`data_hash`）
2. 通过 Web3j 调用合约 `uploadHash()` 将哈希写入区块链
3. 获取交易哈希（`tx_hash`）和区块高度（`block_number`）
4. 回写到对应业务表的 `tx_hash`、`block_number`、`chain_time`、`data_hash` 字段

**各环节上链触发点：**

| 企业类型 | 操作环节 | 上链时机 |
|---------|---------|---------|
| 养殖企业 | 批次初始化 | 创建批次时 |
| 养殖企业 | 生长记录 | 创建记录时 |
| 检疫企业 | 检疫录入 | 提交检疫证书时 |
| 养殖/加工 | 仓储入库 | 创建仓储记录时 |
| 养殖/加工 | 运输记录 | 创建运输记录时 |
| 加工企业 | 加工记录 | 创建加工记录时 |
| 加工企业 | 销售记录 | 创建销售记录时 |

---

## 六、常见问题

### 后端启动失败

| 现象 | 解决方案 |
|------|---------|
| `Access denied for user 'root'` | MySQL 密码不正确，修改 `application.yml` 中 `datasource.password` |
| `Unknown database 'agricultural'` | 数据库未创建，执行步骤 3 |
| `Connection refused: 127.0.0.1:8545` | Ganache 未启动，执行步骤 1 |
| `Contract call reverted` | 合约地址配置错误，确认 `blockchain.contract-address` 与步骤 2 部署地址一致 |
| `Not authorized to upload` | `account-address` 不是合约部署者，确认与步骤 1 记录的 Ganache 账户地址一致 |

### 前端问题

| 现象 | 解决方案 |
|------|---------|
| 页面接口 404/502 | 后端未启动或端口不是 8888 |
| 登录后菜单为空 | 数据库未导入初始化脚本，缺少角色/菜单数据 |
| 小程序 `request:fail` | 未勾选"不校验合法域名"，或 `config.js` 中 IP 地址不正确 |

### 区块链问题

| 现象 | 解决方案 |
|------|---------|
| `Trace ID already exists` | 同一 traceId 只能上链一次，不可重复提交 |
| 所有上链操作失败 | Ganache 已关闭或重启，需重新部署合约并更新配置 |

---

## 七、生产环境打包

### 后端打包

```bash
cd 02_Backend
mvn clean package -DskipTests
java -jar target/trace-system-1.0.0.jar
```

### PC 前端构建

```bash
cd 03_Frontend_Web
npm run build
# 将 dist/ 目录部署到 Nginx
```

修改 `.env.production` 中的 `VUE_APP_BASE_API` 和 `VUE_APP_BACKEND_URL` 为实际后端域名。

### 小程序发布

1. 修改 `config.js` 中 `ENV` 为 `'prod'`，配置生产域名（需 HTTPS）
2. 微信开发者工具点击上传
3. 登录微信公众平台提交审核

---

版本：v1.0.0
