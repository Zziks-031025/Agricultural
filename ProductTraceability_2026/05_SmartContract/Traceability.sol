// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

/**
 * @title Traceability
 * @dev 农产品溯源存证智能合约
 * @notice 用于存储农产品溯源数据的哈希值，实现数据防篡改和可验证
 * @author Agricultural Trace Team
 */
contract Traceability {
    
    // ============================================
    // 数据结构定义
    // ============================================
    
    /**
     * @dev 溯源记录结构体
     * @notice 存储每条溯源记录的核心信息
     */
    struct TraceRecord {
        string traceId;        // 溯源ID/批次号（业务唯一标识）
        string dataHash;       // 业务数据哈希值（SHA-256）
        address operator;      // 操作人地址（上传者）
        uint256 timestamp;     // 上链时间戳（区块时间）
        bool exists;           // 记录是否存在的标志位
    }
    
    // ============================================
    // 状态变量
    // ============================================
    
    // 溯源记录映射：traceId => TraceRecord
    // 通过溯源ID快速查询对应的记录
    mapping(string => TraceRecord) private traceRecords;
    
    // 所有溯源ID列表（用于遍历和统计）
    string[] private traceIds;
    
    // 合约所有者地址
    address public owner;
    
    // 授权操作者映射：address => bool
    // 只有授权的地址才能上传数据
    mapping(address => bool) public authorizedOperators;
    
    // ============================================
    // 事件定义
    // ============================================
    
    /**
     * @dev 数据上传事件
     * @notice 当新的溯源记录上传时触发
     * @param traceId 溯源ID
     * @param dataHash 数据哈希值
     * @param operator 操作人地址
     * @param timestamp 上链时间戳
     */
    event HashUploaded(
        string indexed traceId,
        string dataHash,
        address indexed operator,
        uint256 timestamp
    );
    
    /**
     * @dev 操作者授权事件
     * @notice 当操作者权限变更时触发
     * @param operator 操作者地址
     * @param authorized 是否授权
     */
    event OperatorAuthorized(
        address indexed operator,
        bool authorized
    );
    
    // ============================================
    // 修饰符
    // ============================================
    
    /**
     * @dev 仅合约所有者可调用
     */
    modifier onlyOwner() {
        require(msg.sender == owner, "Only owner can call this function");
        _;
    }
    
    /**
     * @dev 仅授权操作者可调用
     */
    modifier onlyAuthorized() {
        require(
            msg.sender == owner || authorizedOperators[msg.sender],
            "Not authorized to upload"
        );
        _;
    }
    
    // ============================================
    // 构造函数
    // ============================================
    
    /**
     * @dev 构造函数
     * @notice 部署合约时自动执行，设置合约所有者并授权
     */
    constructor() {
        owner = msg.sender;
        authorizedOperators[msg.sender] = true;
    }
    
    // ============================================
    // 核心业务函数
    // ============================================
    
    /**
     * @dev 上传溯源数据哈希
     * @notice 后端调用此函数将业务数据的哈希值存储到区块链
     * @param _traceId 溯源ID/批次号（如：BATCH20260130001）
     * @param _dataHash 业务数据的SHA-256哈希值（64位十六进制字符串）
     * @return success 是否上传成功
     * 
     * 使用场景：
     * 1. 创建新批次时上传批次信息哈希
     * 2. 记录生产过程时上传过程数据哈希
     * 3. 加工、检疫、运输等环节上传相应数据哈希
     * 
     * 注意事项：
     * - 只有授权的操作者才能调用
     * - 同一个traceId只能上传一次，不可覆盖
     * - dataHash应为后端计算的完整业务数据的SHA-256值
     */
    function uploadHash(
        string memory _traceId,
        string memory _dataHash
    ) public onlyAuthorized returns (bool success) {
        // 参数校验
        require(bytes(_traceId).length > 0, "Trace ID cannot be empty");
        require(bytes(_dataHash).length > 0, "Data hash cannot be empty");
        require(bytes(_dataHash).length == 64, "Data hash must be 64 characters (SHA-256)");
        require(!traceRecords[_traceId].exists, "Trace ID already exists");
        
        // 创建溯源记录
        traceRecords[_traceId] = TraceRecord({
            traceId: _traceId,
            dataHash: _dataHash,
            operator: msg.sender,
            timestamp: block.timestamp,
            exists: true
        });
        
        // 添加到ID列表
        traceIds.push(_traceId);
        
        // 触发事件
        emit HashUploaded(_traceId, _dataHash, msg.sender, block.timestamp);
        
        return true;
    }
    
    /**
     * @dev 查询溯源数据哈希
     * @notice 通过溯源ID查询链上存储的哈希值，用于数据验真
     * @param _traceId 溯源ID/批次号
     * @return traceId 溯源ID
     * @return dataHash 数据哈希值
     * @return operator 操作人地址
     * @return timestamp 上链时间戳
     * 
     * 使用场景：
     * 1. 用户扫码查询时，获取链上哈希值
     * 2. 后端验证数据完整性时，对比链上哈希与本地计算哈希
     * 3. 审计追溯时，查看历史记录
     * 
     * 验证流程：
     * 1. 调用此函数获取链上的dataHash
     * 2. 后端根据数据库数据重新计算SHA-256哈希
     * 3. 对比两个哈希值是否一致
     * 4. 一致则数据未被篡改，不一致则数据可能被修改
     */
    function getHash(string memory _traceId) 
        public 
        view 
        returns (
            string memory traceId,
            string memory dataHash,
            address operator,
            uint256 timestamp
        ) 
    {
        require(traceRecords[_traceId].exists, "Trace ID does not exist");
        
        TraceRecord memory record = traceRecords[_traceId];
        return (
            record.traceId,
            record.dataHash,
            record.operator,
            record.timestamp
        );
    }

    
    // ============================================
    // 辅助查询函数
    // ============================================
    
    /**
     * @dev 验证数据哈希是否匹配
     * @notice 对比链上存储的哈希与提供的哈希是否一致
     * @param _traceId 溯源ID
     * @param _dataHash 待验证的数据哈希值
     * @return 是否匹配
     * 
     * 使用场景：
     * 快速验证数据完整性，无需获取完整记录
     */
    function verifyHash(string memory _traceId, string memory _dataHash) 
        public 
        view 
        returns (bool) 
    {
        if (!traceRecords[_traceId].exists) {
            return false;
        }
        return keccak256(bytes(traceRecords[_traceId].dataHash)) == keccak256(bytes(_dataHash));
    }
    
    /**
     * @dev 检查溯源ID是否存在
     * @param _traceId 溯源ID
     * @return 是否存在
     */
    function traceExists(string memory _traceId) public view returns (bool) {
        return traceRecords[_traceId].exists;
    }
    
    /**
     * @dev 获取总记录数
     * @return 链上存储的溯源记录总数
     */
    function getTotalRecords() public view returns (uint256) {
        return traceIds.length;
    }
    
    /**
     * @dev 批量查询溯源记录
     * @notice 一次性查询多个溯源ID的哈希值
     * @param _traceIds 溯源ID数组
     * @return dataHashes 对应的数据哈希值数组
     * @return timestamps 对应的时间戳数组
     * 
     * 使用场景：
     * 批量验证多个批次的数据完整性
     */
    function batchGetHash(string[] memory _traceIds) 
        public 
        view 
        returns (
            string[] memory dataHashes,
            uint256[] memory timestamps
        ) 
    {
        uint256 length = _traceIds.length;
        dataHashes = new string[](length);
        timestamps = new uint256[](length);
        
        for (uint256 i = 0; i < length; i++) {
            if (traceRecords[_traceIds[i]].exists) {
                dataHashes[i] = traceRecords[_traceIds[i]].dataHash;
                timestamps[i] = traceRecords[_traceIds[i]].timestamp;
            }
        }
        
        return (dataHashes, timestamps);
    }
    
    // ============================================
    // 权限管理函数
    // ============================================
    
    /**
     * @dev 授权操作者
     * @notice 只有合约所有者可以授权其他地址上传数据
     * @param _operator 操作者地址
     * @param _authorized 是否授权（true-授权，false-取消授权）
     * 
     * 使用场景：
     * 1. 授权后端服务器地址
     * 2. 授权企业账户地址
     * 3. 取消已授权地址的权限
     */
    function setAuthorizedOperator(address _operator, bool _authorized) public onlyOwner {
        require(_operator != address(0), "Invalid address");
        authorizedOperators[_operator] = _authorized;
        emit OperatorAuthorized(_operator, _authorized);
    }
    
    /**
     * @dev 批量授权操作者
     * @notice 一次性授权多个地址
     * @param _operators 操作者地址数组
     * @param _authorized 是否授权
     */
    function batchSetAuthorizedOperators(address[] memory _operators, bool _authorized) public onlyOwner {
        for (uint256 i = 0; i < _operators.length; i++) {
            if (_operators[i] != address(0)) {
                authorizedOperators[_operators[i]] = _authorized;
                emit OperatorAuthorized(_operators[i], _authorized);
            }
        }
    }
    
    /**
     * @dev 检查地址是否已授权
     * @param _operator 操作者地址
     * @return 是否已授权
     */
    function isAuthorized(address _operator) public view returns (bool) {
        return _operator == owner || authorizedOperators[_operator];
    }
    
    /**
     * @dev 转移合约所有权
     * @notice 将合约所有权转移给新地址，并自动授权新所有者
     * @param _newOwner 新所有者地址
     */
    function transferOwnership(address _newOwner) public onlyOwner {
        require(_newOwner != address(0), "Invalid address");
        require(_newOwner != owner, "New owner is the same as current owner");
        
        // 授权新所有者
        authorizedOperators[_newOwner] = true;
        
        // 转移所有权
        owner = _newOwner;
        
        emit OperatorAuthorized(_newOwner, true);
    }
    
    // ============================================
    // 统计查询函数
    // ============================================
    
    /**
     * @dev 获取指定操作者上传的记录数
     * @param _operator 操作者地址
     * @return 该操作者上传的记录总数
     */
    function getOperatorRecordCount(address _operator) public view returns (uint256) {
        uint256 count = 0;
        for (uint256 i = 0; i < traceIds.length; i++) {
            if (traceRecords[traceIds[i]].operator == _operator) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * @dev 获取指定时间范围内的记录数
     * @param _startTime 开始时间戳
     * @param _endTime 结束时间戳
     * @return 该时间范围内的记录总数
     */
    function getRecordCountByTimeRange(uint256 _startTime, uint256 _endTime) 
        public 
        view 
        returns (uint256) 
    {
        require(_endTime >= _startTime, "Invalid time range");
        
        uint256 count = 0;
        for (uint256 i = 0; i < traceIds.length; i++) {
            uint256 timestamp = traceRecords[traceIds[i]].timestamp;
            if (timestamp >= _startTime && timestamp <= _endTime) {
                count++;
            }
        }
        return count;
    }
}

