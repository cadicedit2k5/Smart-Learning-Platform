package com.smartlearning.core.document.repository.specification;

import com.smartlearning.core.document.entity.Document;
import com.smartlearning.core.document.entity.enums.DocumentLifecycleStatus;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.UUID;

@NoArgsConstructor
public final class DocumentSpecifications {

    public static Specification<Document> keyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return Specification.unrestricted();
        }

        String pattern = "%" + keyword.trim().toLowerCase() + "%";

        return (root, query, criteriaBuilder) ->
                criteriaBuilder.or(
                        criteriaBuilder.like(
                                criteriaBuilder.lower(root.get("title")),
                                pattern
                        ),
                        criteriaBuilder.like(
                                criteriaBuilder.lower(root.get("description")),
                                pattern
                        )
                );
    }

    public static Specification<Document> lifecycleStatus(DocumentLifecycleStatus lifecycleStatus) {
        if (lifecycleStatus == null) {
            return Specification.unrestricted();
        }
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("lifecycleStatus"), lifecycleStatus);
    }

    public static Specification<Document> courseId(UUID courseId) {
        if (courseId == null) {
            return Specification.unrestricted();
        }
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("course").get("id"), courseId);
    }

    public static Specification<Document> chapterId(UUID chapterId) {
        if (chapterId == null) {
            return Specification.unrestricted();
        }
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("chapter").get("id"), chapterId);
    }

    public static Specification<Document> topicId(UUID topicId) {
        if (topicId == null) {
            return Specification.unrestricted();
        }
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("topic").get("id"), topicId);
    }

    public static Specification<Document> notDeleted() {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.isNull(root.get("deletedAt"));
    }
}
