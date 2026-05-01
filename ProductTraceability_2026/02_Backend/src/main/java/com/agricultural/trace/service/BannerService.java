package com.agricultural.trace.service;

import com.agricultural.trace.config.FileUploadConfig;
import com.agricultural.trace.entity.SysBanner;
import com.agricultural.trace.mapper.SysBannerMapper;
import com.agricultural.trace.util.FileCleanupUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class BannerService {

    private final SysBannerMapper sysBannerMapper;
    private final FileUploadConfig fileUploadConfig;

    public Page<SysBanner> getBannerList(Integer current, Integer size, Long enterpriseId, Integer targetType,
                                         Boolean platformOnly, Integer status, String keyword) {
        LambdaQueryWrapper<SysBanner> wrapper = new LambdaQueryWrapper<>();
        if (enterpriseId != null) {
            wrapper.eq(SysBanner::getEnterpriseId, enterpriseId);
        } else if (Boolean.TRUE.equals(platformOnly)) {
            wrapper.isNull(SysBanner::getEnterpriseId);
        }
        if (targetType != null) {
            wrapper.eq(SysBanner::getTargetType, targetType);
        }
        if (status != null) {
            wrapper.eq(SysBanner::getStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(SysBanner::getTitle, keyword).or().like(SysBanner::getDescription, keyword));
        }
        wrapper.orderByAsc(SysBanner::getSortOrder).orderByDesc(SysBanner::getId);
        return sysBannerMapper.selectPage(new Page<>(current, size), wrapper);
    }

    public List<SysBanner> getActiveBanners(Long enterpriseId) {
        LambdaQueryWrapper<SysBanner> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysBanner::getStatus, 1);
        if (enterpriseId != null) {
            wrapper.eq(SysBanner::getEnterpriseId, enterpriseId);
        } else {
            wrapper.isNull(SysBanner::getEnterpriseId);
        }
        wrapper.orderByAsc(SysBanner::getSortOrder).orderByDesc(SysBanner::getId);
        return sysBannerMapper.selectList(wrapper);
    }

    public void addBanner(SysBanner banner) {
        int desiredSortOrder = normalizeRequestedSort(listScopeBanners(banner.getEnterpriseId(), null), banner.getSortOrder());
        shiftScopeForInsert(banner.getEnterpriseId(), null, desiredSortOrder);
        banner.setSortOrder(desiredSortOrder);
        banner.setCreateTime(LocalDateTime.now());
        banner.setUpdateTime(LocalDateTime.now());
        if (banner.getStatus() == null) {
            banner.setStatus(1);
        }
        sysBannerMapper.insert(banner);
        log.info("新增Banner, id={}, title={}, sortOrder={}", banner.getId(), banner.getTitle(), banner.getSortOrder());
    }

    public void updateBanner(SysBanner banner) {
        SysBanner existing = sysBannerMapper.selectById(banner.getId());
        if (existing == null) {
            throw new RuntimeException("Banner不存在");
        }

        FileCleanupUtil.deleteIfChanged(fileUploadConfig.getUploadDir(), existing.getImageUrl(), banner.getImageUrl());

        Long originalEnterpriseId = existing.getEnterpriseId();
        Long targetEnterpriseId = banner.getEnterpriseId();
        if (targetEnterpriseId == null && existing.getEnterpriseId() != null && banner.getEnterpriseId() == null) {
            targetEnterpriseId = null;
        }

        if (!Objects.equals(originalEnterpriseId, targetEnterpriseId)) {
            normalizeScopeSortOrders(originalEnterpriseId, existing.getId());
            int desiredSortOrder = normalizeRequestedSort(listScopeBanners(targetEnterpriseId, null), banner.getSortOrder());
            shiftScopeForInsert(targetEnterpriseId, null, desiredSortOrder);
            banner.setSortOrder(desiredSortOrder);
        } else {
            int desiredSortOrder = normalizeRequestedSort(listScopeBanners(targetEnterpriseId, existing.getId()), banner.getSortOrder());
            reorderWithinScope(targetEnterpriseId, existing.getId(), desiredSortOrder);
            banner.setSortOrder(desiredSortOrder);
        }

        banner.setUpdateTime(LocalDateTime.now());
        sysBannerMapper.updateById(banner);
        log.info("更新Banner, id={}, title={}, sortOrder={}", banner.getId(), banner.getTitle(), banner.getSortOrder());
    }

    public void deleteBanner(Long id) {
        SysBanner banner = sysBannerMapper.selectById(id);
        if (banner == null) {
            throw new RuntimeException("Banner不存在");
        }
        FileCleanupUtil.deleteFile(fileUploadConfig.getUploadDir(), banner.getImageUrl());
        sysBannerMapper.deleteById(id);
        normalizeScopeSortOrders(banner.getEnterpriseId(), null);
        log.info("删除Banner, id={}, title={}", id, banner.getTitle());
    }

    public void toggleStatus(Long id) {
        SysBanner banner = sysBannerMapper.selectById(id);
        if (banner == null) {
            throw new RuntimeException("Banner不存在");
        }
        banner.setStatus(banner.getStatus() == 1 ? 0 : 1);
        banner.setUpdateTime(LocalDateTime.now());
        sysBannerMapper.updateById(banner);
        log.info("切换Banner状态 id={}, status={}", id, banner.getStatus());
    }

    private void shiftScopeForInsert(Long enterpriseId, Long excludeId, int desiredSortOrder) {
        List<SysBanner> scopeBanners = listScopeBanners(enterpriseId, excludeId);
        for (SysBanner scopeBanner : scopeBanners) {
            if (scopeBanner.getSortOrder() != null && scopeBanner.getSortOrder() >= desiredSortOrder) {
                scopeBanner.setSortOrder(scopeBanner.getSortOrder() + 1);
                scopeBanner.setUpdateTime(LocalDateTime.now());
                sysBannerMapper.updateById(scopeBanner);
            }
        }
    }

    private void reorderWithinScope(Long enterpriseId, Long excludeId, int desiredSortOrder) {
        List<SysBanner> scopeBanners = listScopeBanners(enterpriseId, excludeId);
        int nextOrder = 1;
        for (SysBanner scopeBanner : scopeBanners) {
            if (nextOrder == desiredSortOrder) {
                nextOrder++;
            }
            if (!Objects.equals(scopeBanner.getSortOrder(), nextOrder)) {
                scopeBanner.setSortOrder(nextOrder);
                scopeBanner.setUpdateTime(LocalDateTime.now());
                sysBannerMapper.updateById(scopeBanner);
            }
            nextOrder++;
        }
    }

    private void normalizeScopeSortOrders(Long enterpriseId, Long excludeId) {
        List<SysBanner> scopeBanners = listScopeBanners(enterpriseId, excludeId);
        int expectedSortOrder = 1;
        for (SysBanner scopeBanner : scopeBanners) {
            if (!Objects.equals(scopeBanner.getSortOrder(), expectedSortOrder)) {
                scopeBanner.setSortOrder(expectedSortOrder);
                scopeBanner.setUpdateTime(LocalDateTime.now());
                sysBannerMapper.updateById(scopeBanner);
            }
            expectedSortOrder++;
        }
    }

    private int normalizeRequestedSort(List<SysBanner> scopeBanners, Integer requestedSortOrder) {
        int maxSortOrder = scopeBanners.size() + 1;
        if (requestedSortOrder == null || requestedSortOrder <= 0) {
            return maxSortOrder;
        }
        return Math.min(requestedSortOrder, maxSortOrder);
    }

    private List<SysBanner> listScopeBanners(Long enterpriseId, Long excludeId) {
        LambdaQueryWrapper<SysBanner> wrapper = new LambdaQueryWrapper<>();
        if (enterpriseId == null) {
            wrapper.isNull(SysBanner::getEnterpriseId);
        } else {
            wrapper.eq(SysBanner::getEnterpriseId, enterpriseId);
        }
        if (excludeId != null) {
            wrapper.ne(SysBanner::getId, excludeId);
        }
        wrapper.orderByAsc(SysBanner::getSortOrder).orderByAsc(SysBanner::getId);
        return sysBannerMapper.selectList(wrapper);
    }
}
