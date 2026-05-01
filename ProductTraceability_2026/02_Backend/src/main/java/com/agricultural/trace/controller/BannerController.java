package com.agricultural.trace.controller;

import com.agricultural.trace.annotation.OperationLog;
import com.agricultural.trace.common.Result;
import com.agricultural.trace.entity.SysBanner;
import com.agricultural.trace.service.BannerService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/banner")
@RequiredArgsConstructor
@CrossOrigin
public class BannerController {

    private final BannerService bannerService;

    /**
     * 公开接口：获取启用的Banner列表（小程序首页调用）
     * enterpriseId: 企业ID，传则返回该企业Banner+通用Banner，不传则仅返回通用Banner
     */
    @GetMapping("/active")
    public Result<List<SysBanner>> getActiveBanners(
            @RequestParam(required = false) Long enterpriseId) {
        try {
            return Result.success(bannerService.getActiveBanners(enterpriseId));
        } catch (Exception e) {
            log.error("获取Banner列表失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 管理端：分页查询Banner列表
     * 管理员不传enterpriseId查全部，企业用户传自己的enterpriseId
     */
    @GetMapping("/list")
    public Result<Page<SysBanner>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) Long enterpriseId,
            @RequestParam(required = false) Integer targetType,
            @RequestParam(required = false) Boolean platformOnly,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {
        try {
            return Result.success(bannerService.getBannerList(current, size, enterpriseId, targetType, platformOnly, status, keyword));
        } catch (Exception e) {
            log.error("查询Banner列表失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 管理端：新增Banner
     */
    @OperationLog("新增轮播图")
    @PostMapping("/add")
    public Result<Void> add(@RequestBody SysBanner banner) {
        try {
            bannerService.addBanner(banner);
            return Result.success();
        } catch (Exception e) {
            log.error("新增Banner失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 管理端：更新Banner
     */
    @OperationLog("更新轮播图")
    @PutMapping("/update")
    public Result<Void> update(@RequestBody SysBanner banner) {
        try {
            bannerService.updateBanner(banner);
            return Result.success();
        } catch (Exception e) {
            log.error("更新Banner失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 管理端：删除Banner
     */
    @OperationLog("删除轮播图")
    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        try {
            bannerService.deleteBanner(id);
            return Result.success();
        } catch (Exception e) {
            log.error("删除Banner失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 管理端：切换Banner启用/禁用状态
     */
    @OperationLog("切换轮播图状态")
    @PutMapping("/toggle-status/{id}")
    public Result<Void> toggleStatus(@PathVariable Long id) {
        try {
            bannerService.toggleStatus(id);
            return Result.success();
        } catch (Exception e) {
            log.error("切换Banner状态失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }
}
