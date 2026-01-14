package org.example.cloudopsadmin.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.example.cloudopsadmin.entity.KnowledgeArticle;
import org.example.cloudopsadmin.repository.KnowledgeArticleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KnowledgeArticleService {

    private final KnowledgeArticleRepository knowledgeArticleRepository;

    @Transactional
    public KnowledgeArticle createArticle(CreateArticleRequest request) {
        KnowledgeArticle article = new KnowledgeArticle();
        article.setTitle(request.getTitle());
        article.setAuthor(request.getAuthor());
        article.setContent(request.getContent());
        article.setCategories(splitCsv(request.getCategories()));
        article.setTags(splitCsv(request.getTags()));
        article.setKeywords(splitCsv(request.getKeywords()));
        return knowledgeArticleRepository.save(article);
    }

    @Transactional(readOnly = true)
    public KnowledgeArticle getArticle(Long id) {
        return knowledgeArticleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Article not found"));
    }

    @Transactional
    public KnowledgeArticle updateArticle(Long id, CreateArticleRequest request) {
        KnowledgeArticle article = knowledgeArticleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Article not found"));
        article.setTitle(request.getTitle());
        article.setAuthor(request.getAuthor());
        article.setContent(request.getContent());
        article.setCategories(splitCsv(request.getCategories()));
        article.setTags(splitCsv(request.getTags()));
        article.setKeywords(splitCsv(request.getKeywords()));
        return knowledgeArticleRepository.save(article);
    }

    @Transactional(readOnly = true)
    public Page<KnowledgeArticle> listArticles(int page, int pageSize, String search, String sortOrder) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortOrder == null ? "DESC" : sortOrder), "id");
        Pageable pageable = PageRequest.of(page - 1, pageSize, sort);

        Specification<KnowledgeArticle> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(search)) {
                String like = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), like),
                        cb.like(cb.lower(root.get("author")), like),
                        cb.like(cb.lower(root.get("content")), like)
                ));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return knowledgeArticleRepository.findAll(spec, pageable);
    }

    private List<String> splitCsv(String s) {
        if (s == null || s.trim().isEmpty()) {
            return List.of();
        }
        return Arrays.stream(s.split(","))
                .map(String::trim)
                .filter(v -> !v.isEmpty())
                .collect(Collectors.toList());
    }

    @Data
    public static class CreateArticleRequest {
        @Schema(description = "标题")
        @JsonProperty("title")
        private String title;

        @Schema(description = "分类，逗号分隔")
        @JsonProperty("categories")
        private String categories;

        @Schema(description = "标签，逗号分隔")
        @JsonProperty("tags")
        private String tags;

        @Schema(description = "作者")
        @JsonProperty("author")
        private String author;

        @Schema(description = "内容")
        @JsonProperty("content")
        private String content;

        @Schema(description = "关键词，逗号分隔")
        @JsonProperty("keywords")
        private String keywords;
    }
}
