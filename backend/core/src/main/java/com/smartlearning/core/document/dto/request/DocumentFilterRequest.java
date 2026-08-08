package com.smartlearning.core.document.dto.request;

import com.smartlearning.common.dto.request.FilterRequest;
import com.smartlearning.core.document.entity.Document;
import com.smartlearning.core.document.entity.enums.DocumentLifecycleStatus;
import com.smartlearning.core.document.repository.specification.DocumentSpecifications;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public class DocumentFilterRequest extends FilterRequest<Document> {
    private String keyword;
    private DocumentLifecycleStatus lifecycleStatus = DocumentLifecycleStatus.ACTIVE;
    private UUID chapterId;
    private UUID topicId;

    @Override
    public Specification<Document> specification() {
        return Specification.allOf(
                DocumentSpecifications.keyword(this.keyword),
                DocumentSpecifications.lifecycleStatus(this.lifecycleStatus),
                DocumentSpecifications.chapterId(this.chapterId),
                DocumentSpecifications.topicId(this.topicId)
        );
    }
}
