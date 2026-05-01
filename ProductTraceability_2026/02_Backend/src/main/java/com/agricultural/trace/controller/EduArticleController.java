package com.agricultural.trace.controller;

import com.agricultural.trace.annotation.OperationLog;
import com.agricultural.trace.common.Result;
import com.agricultural.trace.entity.EduArticle;
import com.agricultural.trace.service.EduArticleService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/edu/article")
@RequiredArgsConstructor
@CrossOrigin
public class EduArticleController {

    private final EduArticleService eduArticleService;

    /**
     * 公开接口：获取已发布的文章列表（小程序调用）
     */
    @GetMapping("/published")
    public Result<List<EduArticle>> getPublishedArticles(
            @RequestParam(required = false) Long enterpriseId,
            @RequestParam(required = false) String category) {
        try {
            return Result.success(eduArticleService.getPublishedArticles(enterpriseId, category));
        } catch (Exception e) {
            log.error("获取文章列表失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 公开接口：获取文章详情
     */
    @GetMapping("/detail/{id}")
    public Result<EduArticle> getDetail(@PathVariable Long id) {
        try {
            return Result.success(eduArticleService.getArticleDetail(id));
        } catch (Exception e) {
            log.error("获取文章详情失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 公开接口：增加阅读量
     */
    @PutMapping("/view/{id}")
    public Result<Void> incrementView(@PathVariable Long id) {
        try {
            eduArticleService.incrementViewCount(id);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 管理端：分页查询文章列表
     */
    @GetMapping("/list")
    public Result<Page<EduArticle>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) Long enterpriseId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {
        try {
            return Result.success(eduArticleService.getArticleList(current, size, enterpriseId, category, status, keyword));
        } catch (Exception e) {
            log.error("查询文章列表失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 管理端：新增文章
     */
    @OperationLog(module = "科普文章", operation = "新增文章")
    @PostMapping("/add")
    public Result<Void> add(@RequestBody EduArticle article) {
        try {
            eduArticleService.addArticle(article);
            return Result.success();
        } catch (Exception e) {
            log.error("新增文章失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 管理端：更新文章
     */
    @OperationLog(module = "科普文章", operation = "更新文章")
    @PutMapping("/update")
    public Result<Void> update(@RequestBody EduArticle article) {
        try {
            eduArticleService.updateArticle(article);
            return Result.success();
        } catch (Exception e) {
            log.error("更新文章失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 管理端：删除文章
     */
    @OperationLog(module = "科普文章", operation = "删除文章")
    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        try {
            eduArticleService.deleteArticle(id);
            return Result.success();
        } catch (Exception e) {
            log.error("删除文章失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 管理端：切换文章发布/草稿状态
     */
    @OperationLog(module = "科普文章", operation = "切换文章状态")
    @PutMapping("/toggle-status/{id}")
    public Result<Void> toggleStatus(@PathVariable Long id) {
        try {
            eduArticleService.toggleStatus(id);
            return Result.success();
        } catch (Exception e) {
            log.error("切换文章状态失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }
}
