package com.agricultural.trace.service;

import com.agricultural.trace.config.FileUploadConfig;
import com.agricultural.trace.entity.EduArticle;
import com.agricultural.trace.mapper.EduArticleMapper;
import com.agricultural.trace.util.FileCleanupUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EduArticleService {

    private final EduArticleMapper eduArticleMapper;
    private final FileUploadConfig fileUploadConfig;

    /**
     * 分页查询文章列表（管理端）
     */
    public Page<EduArticle> getArticleList(Integer current, Integer size, Long enterpriseId, String category, Integer status, String keyword) {
        LambdaQueryWrapper<EduArticle> wrapper = new LambdaQueryWrapper<>();
        if (enterpriseId != null) {
            wrapper.eq(EduArticle::getEnterpriseId, enterpriseId);
        }
        if (StringUtils.hasText(category)) {
            wrapper.eq(EduArticle::getCategory, category);
        }
        if (status != null) {
            wrapper.eq(EduArticle::getStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(EduArticle::getTitle, keyword).or().like(EduArticle::getSummary, keyword));
        }
        wrapper.orderByAsc(EduArticle::getSortOrder).orderByDesc(EduArticle::getId);
        return eduArticleMapper.selectPage(new Page<>(current, size), wrapper);
    }

    /**
     * 获取已发布的文章列表（公开接口，小程序调用）
     * 传enterpriseId则仅返回该企业自己的文章
     * 不传enterpriseId则返回所有文章（平台通用 + 所有企业），供消费者/游客查看
     * 可按category筛选
     */
    public List<EduArticle> getPublishedArticles(Long enterpriseId, String category) {
        LambdaQueryWrapper<EduArticle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EduArticle::getStatus, 1);
        if (enterpriseId != null) {
            wrapper.eq(EduArticle::getEnterpriseId, enterpriseId);
        }
        if (StringUtils.hasText(category)) {
            wrapper.eq(EduArticle::getCategory, category);
        }
        wrapper.orderByAsc(EduArticle::getSortOrder).orderByDesc(EduArticle::getId);
        return eduArticleMapper.selectList(wrapper);
    }

    /**
     * 获取文章详情
     */
    public EduArticle getArticleDetail(Long id) {
        EduArticle article = eduArticleMapper.selectById(id);
        if (article == null) {
            throw new RuntimeException("文章不存在");
        }
        return article;
    }

    /**
     * 增加阅读量
     */
    public void incrementViewCount(Long id) {
        EduArticle article = eduArticleMapper.selectById(id);
        if (article != null) {
            article.setViewCount((article.getViewCount() != null ? article.getViewCount() : 0) + 1);
            eduArticleMapper.updateById(article);
        }
    }

    /**
     * 新增文章
     */
    public void addArticle(EduArticle article) {
        article.setCreateTime(LocalDateTime.now());
        article.setUpdateTime(LocalDateTime.now());
        if (article.getSortOrder() == null) {
            article.setSortOrder(0);
        }
        if (article.getStatus() == null) {
            article.setStatus(0);
        }
        if (article.getViewCount() == null) {
            article.setViewCount(0);
        }
        if (article.getStatus() == 1 && article.getPublishTime() == null) {
            article.setPublishTime(LocalDateTime.now());
        }
        eduArticleMapper.insert(article);
        log.info("新增科普文章, id={}, title={}", article.getId(), article.getTitle());
    }

    /**
     * 更新文章
     */
    public void updateArticle(EduArticle article) {
        EduArticle existing = eduArticleMapper.selectById(article.getId());
        if (existing == null) {
            throw new RuntimeException("文章不存在");
        }
        // 即时删除被替换的旧封面图
        FileCleanupUtil.deleteIfChanged(fileUploadConfig.getUploadDir(), existing.getCoverUrl(), article.getCoverUrl());
        // 如果从草稿变为发布，设置发布时间
        if (article.getStatus() != null && article.getStatus() == 1 && existing.getStatus() == 0) {
            article.setPublishTime(LocalDateTime.now());
        }
        article.setUpdateTime(LocalDateTime.now());
        eduArticleMapper.updateById(article);
        log.info("更新科普文章, id={}, title={}", article.getId(), article.getTitle());
    }

    /**
     * 删除文章
     */
    public void deleteArticle(Long id) {
        EduArticle article = eduArticleMapper.selectById(id);
        if (article == null) {
            throw new RuntimeException("文章不存在");
        }
        // 即时删除封面图文件
        FileCleanupUtil.deleteFile(fileUploadConfig.getUploadDir(), article.getCoverUrl());
        eduArticleMapper.deleteById(id);
        log.info("删除科普文章, id={}, title={}", id, article.getTitle());
    }

    /**
     * 切换文章状态（草稿/发布）
     */
    public void toggleStatus(Long id) {
        EduArticle article = eduArticleMapper.selectById(id);
        if (article == null) {
            throw new RuntimeException("文章不存在");
        }
        if (article.getStatus() == 0) {
            article.setStatus(1);
            if (article.getPublishTime() == null) {
                article.setPublishTime(LocalDateTime.now());
            }
        } else {
            article.setStatus(0);
        }
        article.setUpdateTime(LocalDateTime.now());
        eduArticleMapper.updateById(article);
        log.info("切换文章状态, id={}, status={}", id, article.getStatus());
    }
}
