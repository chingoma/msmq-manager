package com.enterprise.msmq.repository;

import com.enterprise.msmq.entity.MsmqMessageTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for MSMQ Message Templates.
 */
@Repository
public interface MsmqMessageTemplateRepository extends JpaRepository<MsmqMessageTemplate, Long> {

    /**
     * Find template by name.
     */
    Optional<MsmqMessageTemplate> findByTemplateName(String templateName);

    /**
     * Find all active templates.
     */
    List<MsmqMessageTemplate> findByIsActiveTrue();

    /**
     * Find templates by type.
     */
    List<MsmqMessageTemplate> findByTemplateTypeAndIsActiveTrue(String templateType);

    /**
     * Find templates by name containing (case-insensitive).
     */
    @Query("SELECT t FROM MsmqMessageTemplate t WHERE LOWER(t.templateName) LIKE LOWER(CONCAT('%', :name, '%')) AND t.isActive = true")
    List<MsmqMessageTemplate> findByNameContainingIgnoreCase(@Param("name") String name);

    /**
     * Check if template name exists.
     */
    boolean existsByTemplateName(String templateName);
}
