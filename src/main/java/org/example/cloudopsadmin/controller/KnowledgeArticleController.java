package org.example.cloudopsadmin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.cloudopsadmin.common.ApiResponse;
import org.example.cloudopsadmin.entity.KnowledgeArticle;
import org.example.cloudopsadmin.service.KnowledgeArticleService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/knowledge/articles")
@RequiredArgsConstructor
@Tag(name = "Knowledge Articles", description = "Knowledge base articles management")
public class KnowledgeArticleController {

    private final KnowledgeArticleService knowledgeArticleService;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(KnowledgeArticleController.class);

    @GetMapping
    @Operation(summary = "获取知识库文章列表", description = "分页获取知识库文章列表，支持搜索")
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize,
            @RequestParam(required = false) String search,
            @RequestParam(name = "sort_order", defaultValue = "desc") String sortOrder
    ) {
        try {
            var articlePage = knowledgeArticleService.listArticles(page, pageSize, search, sortOrder);
            List<Map<String, Object>> list = articlePage.getContent().stream().map(a -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", a.getId());
                m.put("title", a.getTitle());
                m.put("author", a.getAuthor());
                m.put("categories", a.getCategories());
                m.put("tags", a.getTags());
                m.put("keywords", a.getKeywords());
                m.put("created_at", a.getCreatedAt());
                return m;
            }).toList();
            Map<String, Object> data = new HashMap<>();
            data.put("total", articlePage.getTotalElements());
            data.put("page", articlePage.getNumber() + 1);
            data.put("page_size", articlePage.getSize());
            data.put("list", list);
            return ApiResponse.success("success", data);
        } catch (Exception e) {
            log.error("List knowledge articles failed. page={}, pageSize={}, search={}, sortOrder={}", page, pageSize, search, sortOrder, e);
            return ApiResponse.error(500, "Internal Server Error: " + e.getMessage());
        }
    }

    @PostMapping
    @Operation(summary = "创建知识库文章", description = "创建文章，后端生成ID")
    public ApiResponse<Map<String, Object>> create(@RequestBody KnowledgeArticleService.CreateArticleRequest request) {
        try {
            KnowledgeArticle saved = knowledgeArticleService.createArticle(request);
            Map<String, Object> data = toResponse(saved);
            return ApiResponse.success("success", data);
        } catch (Exception e) {
            log.error("Create knowledge article failed", e);
            return ApiResponse.error(500, "Internal Server Error: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "编辑知识库文章", description = "根据ID编辑知识库文章内容")
    public ApiResponse<Map<String, Object>> update(@PathVariable Long id,
                                                   @RequestBody KnowledgeArticleService.CreateArticleRequest request) {
        try {
            KnowledgeArticle updated = knowledgeArticleService.updateArticle(id, request);
            return ApiResponse.success("success", toResponse(updated));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(404, e.getMessage());
        } catch (Exception e) {
            log.error("Update knowledge article failed. id={}", id, e);
            return ApiResponse.error(500, "Internal Server Error: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取知识库文章详情", description = "根据ID获取知识库文章详情")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        try {
            KnowledgeArticle article = knowledgeArticleService.getArticle(id);
            return ApiResponse.success("success", toResponse(article));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(404, e.getMessage());
        } catch (Exception e) {
            log.error("Get knowledge article detail failed. id={}", id, e);
            return ApiResponse.error(500, "Internal Server Error: " + e.getMessage());
        }
    }

    private Map<String, Object> toResponse(KnowledgeArticle article) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", article.getId());
        map.put("title", article.getTitle());
        map.put("author", article.getAuthor());
        map.put("content", article.getContent());
        map.put("categories", article.getCategories());
        map.put("tags", article.getTags());
        map.put("keywords", article.getKeywords());
        map.put("created_at", article.getCreatedAt());
        return map;
    }
}
