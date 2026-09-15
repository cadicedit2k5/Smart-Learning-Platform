package com.smartlearning.core.document.dto.request;

import com.smartlearning.common.dto.request.FilterRequest;
import com.smartlearning.core.document.entity.Document;
import com.smartlearning.core.document.repository.specification.DocumentSpecifications;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.jpa.domain.Specification;

@Getter
@Setter
public class DocumentFilterRequest extends FilterRequest<Document> {
    private String keyword;

    @Override
    public Specification<Document> specification() {
        return Specification.allOf(
                DocumentSpecifications.keyword(this.keyword),
                DocumentSpecifications.notDeleted()
        );
    }
}
