package com.smartlearning.core.document.dto.request;

import com.smartlearning.core.document.entity.Document;
import com.smartlearning.core.document.entity.enums.DocumentProcessingStatus;
import com.smartlearning.core.document.repository.specification.DocumentSpecifications;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

@Getter
@Setter
public class AdminDocumentFilterRequest
        extends DocumentFilterRequest {

    private UUID courseId;

    private String courseKeyword;

    private DocumentProcessingStatus processingStatus;

    @Override
    public Specification<Document> specification() {
        return Specification.allOf(
                super.specification(),
                DocumentSpecifications.courseId(courseId),
                DocumentSpecifications.courseKeyword(courseKeyword),
                DocumentSpecifications.processingStatus(processingStatus)
        );
    }
}
