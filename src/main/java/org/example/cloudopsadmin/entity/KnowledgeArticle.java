package org.example.cloudopsadmin.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "knowledge_articles", indexes = {
        @Index(name = "idx_ka_title", columnList = "title")
})
@Data
public class KnowledgeArticle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "author")
    private String author;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @ElementCollection
    @CollectionTable(name = "knowledge_article_categories", joinColumns = @JoinColumn(name = "article_id"))
    @Column(name = "category")
    private List<String> categories = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "knowledge_article_tags", joinColumns = @JoinColumn(name = "article_id"))
    @Column(name = "tag")
    private List<String> tags = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "knowledge_article_keywords", joinColumns = @JoinColumn(name = "article_id"))
    @Column(name = "keyword")
    private List<String> keywords = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
