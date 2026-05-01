# 智能合约使用说明

## 合约概述

`Traceability.sol` 是农产品溯源系统的核心智能合约，用于在以太坊区块链上存储溯源数据的哈希值，实现数据防篡改和可验证。

## 核心功能

### 1. 数据上传（uploadHash）

**功能说明：** 将业务数据的哈希值上传到区块链

**函数签名：**
```solidity
function uploadHash(string memory _traceId, string memory _dataHash) 
    public onlyAuthorized returns (bool success)
```

**参数说明：**
- `_traceId`: 溯源ID/批次号（如：BATCH20260130001）
- `_dataHash`: 业务数据的 SHA-256 哈希值（64位十六进制字符串）

**返回值：**
- `success`: 是否上传成功

**使用示例（Web3.js）：**
```javascript
const traceId = "BATCH20260130001";
const dataHash = "a1b2c3d4e5f6..."; // 64位SHA-256哈希

const tx = await contract.methods.uploadHash(traceId, dataHash).send({
    from: operatorAddress,
    gas: 300000
});

console.log("交易哈希:", tx.transactionHash);
```

**注意事项：**
- 只有授权的操作者才能调用
- 同一个 traceId 只能上传一次，不可覆盖
- dataHash 必须是 64 位字符（SHA-256 标准长度）

---

### 2. 数据查询（getHash）

**功能说明：** 通过溯源ID查询链上存储的哈希值

**函数签名：**
```solidity
function getHash(string memory _traceId) 
    public view returns (
        string memory traceId,
        string memory dataHash,
        address operator,
        uint256 timestamp
    )
```

**参数说明：**
- `_traceId`: 溯源ID/批次号

**返回值：**
- `traceId`: 溯源ID
- `dataHash`: 数据哈希值
- `operator`: 操作人地址
- `timestamp`: 上链时间戳

**使用示例（Web3.js）：**
```javascript
const traceId = "BATCH20260130001";

const result = await contract.methods.getHash(traceId).call();

console.log("溯源ID:", result.traceId);
console.log("数据哈希:", result.dataHash);
console.log("操作人:", result.operator);
console.log("上链时间:", new Date(result.timestamp * 1000));
```

---

### 3. 数据验证（verifyHash）

**功能说明：** 验证提供的哈希值是否与链上存储的一致

**函数签名：**
```solidity
function verifyHash(string memory _traceId, string memory _dataHash) 
    public view returns (bool)
```

**参数说明：**
- `_traceId`: 溯源ID
- `_dataHash`: 待验证的数据哈希值

**返回值：**
- `bool`: 是否匹配（true-匹配，false-不匹配或不存在）

**使用示例（Web3.js）：**
```javascript
const traceId = "BATCH20260130001";
const localHash = "a1b2c3d4e5f6..."; // 本地计算的哈希

const isValid = await contract.methods.verifyHash(traceId, localHash).call();

if (isValid) {
    console.log("✓ 数据验证通过，未被篡改");
} else {
    console.log("✗ 数据验证失败，可能已被篡改");
}
```

---

## 权限管理

### 授权操作者（setAuthorizedOperator）

**功能说明：** 授权或取消授权某个地址的上传权限

**函数签名：**
```solidity
function setAuthorizedOperator(address _operator, bool _authorized) 
    public onlyOwner
```

**使用示例：**
```javascript
// 授权后端服务器地址
await contract.methods.setAuthorizedOperator(
    "0x1234567890abcdef...",
    true
).send({ from: ownerAddress });

// 取消授权
await contract.methods.setAuthorizedOperator(
    "0x1234567890abcdef...",
    false
).send({ from: ownerAddress });
```

---

## 完整使用流程

### 1. 数据上链流程

```javascript
// 步骤1：后端计算业务数据的SHA-256哈希
const crypto = require('crypto');
const businessData = JSON.stringify({
    batchCode: "BATCH20260130001",
    productName: "有机西红柿",
    quantity: 1000,
    // ... 其他业务数据
});
const dataHash = crypto.createHash('sha256').update(businessData).digest('hex');

// 步骤2：调用智能合约上传哈希
const tx = await contract.methods.uploadHash(
    "BATCH20260130001",
    dataHash
).send({
    from: operatorAddress,
    gas: 300000
});

// 步骤3：保存交易信息到数据库
await saveToDatabase({
    traceId: "BATCH20260130001",
    dataHash: dataHash,
    txHash: tx.transactionHash,
    blockNumber: tx.blockNumber,
    timestamp: new Date()
});
```

### 2. 数据验证流程

```javascript
// 步骤1：从区块链查询哈希值
const chainResult = await contract.methods.getHash("BATCH20260130001").call();
const chainHash = chainResult.dataHash;

// 步骤2：从数据库获取业务数据并重新计算哈希
const dbData = await getFromDatabase("BATCH20260130001");
const localHash = crypto.createHash('sha256')
    .update(JSON.stringify(dbData))
    .digest('hex');

// 步骤3：对比哈希值
if (chainHash === localHash) {
    console.log("✓ 数据完整性验证通过");
    return { valid: true, message: "数据未被篡改" };
} else {
    console.log("✗ 数据完整性验证失败");
    return { valid: false, message: "数据可能已被篡改" };
}
```

---

## 合约部署

### 使用 Remix IDE 部署

1. 访问 https://remix.ethereum.org/
2. 创建新文件 `Traceability.sol`，复制合约代码
3. 编译合约（Solidity 0.8.0+）
4. 连接 MetaMask 钱包
5. 选择 Sepolia 测试网
6. 部署合约
7. 复制合约地址

### 使用 Hardhat 部署

```javascript
// scripts/deploy.js
const hre = require("hardhat");

async function main() {
    const Traceability = await hre.ethers.getContractFactory("Traceability");
    const traceability = await Traceability.deploy();
    await traceability.deployed();
    
    console.log("合约部署成功！");
    console.log("合约地址:", traceability.address);
}

main().catch((error) => {
    console.error(error);
    process.exitCode = 1;
});
```

运行部署：
```bash
npx hardhat run scripts/deploy.js --network sepolia
```

---

## Gas 费用估算

| 操作 | 预估 Gas | 说明 |
|------|----------|------|
| uploadHash | ~100,000 | 首次上传（存储新数据） |
| getHash | 0 | 查询操作（view函数，不消耗Gas） |
| verifyHash | 0 | 验证操作（view函数，不消耗Gas） |
| setAuthorizedOperator | ~50,000 | 授权操作 |

**注意：** 实际 Gas 消耗会根据网络状况和数据大小有所变化。

---

## 安全建议

1. **私钥管理**：妥善保管合约所有者的私钥，建议使用硬件钱包
2. **权限控制**：只授权可信的后端服务器地址
3. **数据验证**：上传前验证 traceId 和 dataHash 的格式
4. **错误处理**：捕获并记录所有区块链交易错误
5. **Gas 限制**：设置合理的 Gas Limit，避免交易失败

---

## 常见问题

### Q1: 为什么上传失败提示 "Not authorized to upload"？
**A:** 当前地址未被授权。需要合约所有者调用 `setAuthorizedOperator` 授权该地址。

### Q2: 可以修改已上传的数据吗？
**A:** 不可以。区块链数据不可篡改，同一个 traceId 只能上传一次。

### Q3: 如何降低 Gas 费用？
**A:** 
- 选择网络拥堵较少的时段
- 批量操作使用 `batchGetHash` 等批量函数
- 使用 Layer 2 解决方案（如 Polygon）

### Q4: 数据哈希应该包含哪些内容？
**A:** 建议包含所有关键业务数据，如批次号、产品信息、时间戳等，确保数据完整性。

---

## 技术支持

- 合约源码：`Traceability.sol`
- ABI 文件：`contract_abi.json`
- 测试网络：Sepolia Testnet
- 区块浏览器：https://sepolia.etherscan.io/
