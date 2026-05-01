package com.agricultural.trace.service;

import com.agricultural.trace.entity.SysConfig;
import com.agricultural.trace.mapper.SysConfigMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemConfigService {

    private final SysConfigMapper sysConfigMapper;

    public Page<SysConfig> getConfigList(Integer current, Integer size, String configType, String keyword) {
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(configType)) {
            wrapper.eq(SysConfig::getConfigType, configType);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(SysConfig::getConfigKey, keyword).or().like(SysConfig::getDescription, keyword));
        }
        wrapper.orderByAsc(SysConfig::getId);
        return sysConfigMapper.selectPage(new Page<>(current, size), wrapper);
    }

    public void addConfig(SysConfig config) {
        LambdaQueryWrapper<SysConfig> check = new LambdaQueryWrapper<>();
        check.eq(SysConfig::getConfigKey, config.getConfigKey());
        if (sysConfigMapper.selectCount(check) > 0) {
            throw new RuntimeException("参数键已存在");
        }
        config.setCreateTime(LocalDateTime.now());
        config.setUpdateTime(LocalDateTime.now());
        sysConfigMapper.insert(config);
        log.info("新增系统配置, key={}", config.getConfigKey());
    }

    public void updateConfig(SysConfig config) {
        SysConfig existing = sysConfigMapper.selectById(config.getId());
        if (existing == null) {
            throw new RuntimeException("配置不存在");
        }
        existing.setConfigValue(config.getConfigValue());
        existing.setConfigType(config.getConfigType());
        existing.setDescription(config.getDescription());
        existing.setUpdateTime(LocalDateTime.now());
        sysConfigMapper.updateById(existing);
        log.info("更新系统配置, id={}, key={}", config.getId(), existing.getConfigKey());
    }

    public void deleteConfig(Long id) {
        SysConfig config = sysConfigMapper.selectById(id);
        if (config == null) {
            throw new RuntimeException("配置不存在");
        }
        sysConfigMapper.deleteById(id);
        log.info("删除系统配置, id={}, key={}", id, config.getConfigKey());
    }

    /**
     * 根据配置键获取配置值
     */
    public String getConfigValue(String configKey) {
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysConfig::getConfigKey, configKey);
        SysConfig config = sysConfigMapper.selectOne(wrapper);
        return config != null ? config.getConfigValue() : null;
    }

    /**
     * 根据配置键获取配置值，如果不存在则返回默认值
     */
    public String getConfigValue(String configKey, String defaultValue) {
        String value = getConfigValue(configKey);
        return value != null ? value : defaultValue;
    }
}
