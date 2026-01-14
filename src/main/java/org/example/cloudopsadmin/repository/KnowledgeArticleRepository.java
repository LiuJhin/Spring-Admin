package org.example.cloudopsadmin.repository;

import org.example.cloudopsadmin.entity.KnowledgeArticle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface KnowledgeArticleRepository extends JpaRepository<KnowledgeArticle, Long>, JpaSpecificationExecutor<KnowledgeArticle> {
}
