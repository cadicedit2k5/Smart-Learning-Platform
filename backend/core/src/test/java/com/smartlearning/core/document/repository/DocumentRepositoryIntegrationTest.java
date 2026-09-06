package com.smartlearning.core.document.repository;

import com.smartlearning.core.course.entity.Course;
import com.smartlearning.core.course.repository.CourseRepository;
import com.smartlearning.core.document.dto.request.AdminDocumentFilterRequest;
import com.smartlearning.core.document.entity.Document;
import com.smartlearning.core.document.entity.DocumentVersion;
import com.smartlearning.core.document.entity.enums.DocumentProcessingStatus;
import com.smartlearning.core.support.CoreIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static com.smartlearning.core.support.CoreTestData.course;
import static com.smartlearning.core.support.CoreTestData.document;
import static com.smartlearning.core.support.CoreTestData.documentVersion;
import static org.assertj.core.api.Assertions.assertThat;

class DocumentRepositoryIntegrationTest extends CoreIntegrationTest {

    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private DocumentRepository documentRepository;
    @Autowired
    private DocumentVersionRepository versionRepository;

    @Test
    void adminFilter_combinesCourseKeywordAndProcessingStatus() {
        Document indexedDocument = persistDocument(
                "Smart Learning Java",
                "Java handbook",
                DocumentProcessingStatus.INDEXED,
                "indexed-java.pdf"
        );
        persistDocument(
                "Smart Learning Java",
                "Failed handbook",
                DocumentProcessingStatus.FAILED,
                "failed-java.pdf"
        );
        persistDocument(
                "Design Basics",
                "Design handbook",
                DocumentProcessingStatus.INDEXED,
                "indexed-design.pdf"
        );

        AdminDocumentFilterRequest filter = new AdminDocumentFilterRequest();
        filter.setCourseKeyword("  java  ");
        filter.setProcessingStatus(DocumentProcessingStatus.INDEXED);

        var result = documentRepository.findAll(
                filter.specification(),
                filter.pageable()
        );

        assertThat(result.getContent())
                .extracting(Document::getId)
                .containsExactly(indexedDocument.getId());
    }

    private Document persistDocument(
            String courseTitle,
            String documentTitle,
            DocumentProcessingStatus status,
            String storageKey
    ) {
        Course course = course();
        course.setId(null);
        course.setTitle(courseTitle);
        course = courseRepository.saveAndFlush(course);

        Document document = document();
        document.setId(null);
        document.setCourse(course);
        document.setTitle(documentTitle);
        document.setVersion(null);
        document = documentRepository.saveAndFlush(document);

        DocumentVersion version = documentVersion();
        version.setId(null);
        version.setDocument(document);
        version.setProcessingStatus(status);
        version.setStorageKey("test/" + storageKey);
        version = versionRepository.saveAndFlush(version);

        document.setVersion(version);
        return documentRepository.saveAndFlush(document);
    }
}
