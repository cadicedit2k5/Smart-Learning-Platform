package com.smartlearning.core.support;

import com.smartlearning.core.course.dto.response.CourseMemberResponse;
import com.smartlearning.core.course.dto.response.CourseResponse;
import com.smartlearning.core.course.entity.AccessCode;
import com.smartlearning.core.course.entity.Course;
import com.smartlearning.core.course.entity.CourseMember;
import com.smartlearning.core.course.entity.enums.CourseMemberRole;
import com.smartlearning.core.course.entity.enums.CourseMemberStatus;
import com.smartlearning.core.course.entity.enums.CourseStatus;
import com.smartlearning.core.course.entity.enums.CourseVisibility;
import com.smartlearning.core.document.dto.response.DocumentResponse;
import com.smartlearning.core.document.entity.Document;
import com.smartlearning.core.document.entity.DocumentProcessingJob;
import com.smartlearning.core.document.entity.DocumentVersion;
import com.smartlearning.core.document.entity.enums.DocumentLifecycleStatus;
import com.smartlearning.core.document.entity.enums.DocumentProcessingStatus;
import com.smartlearning.core.document.entity.enums.ProcessingJobStatus;
import com.smartlearning.core.document.messaging.event.DocumentIngestionCompletedEvent;
import com.smartlearning.core.document.messaging.event.DocumentIngestionFailedEvent;

import java.time.Instant;
import java.util.UUID;

public final class CoreTestData {

    public static final UUID COURSE_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    public static final UUID OWNER_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    public static final UUID STUDENT_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");
    public static final UUID MEMBER_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
    public static final UUID ACCESS_CODE_ID = UUID.fromString("40000000-0000-0000-0000-000000000001");
    public static final UUID DOCUMENT_ID = UUID.fromString("50000000-0000-0000-0000-000000000001");
    public static final UUID VERSION_ID = UUID.fromString("60000000-0000-0000-0000-000000000001");
    public static final UUID PROCESSING_JOB_ID = UUID.fromString("70000000-0000-0000-0000-000000000001");
    public static final UUID EVENT_ID = UUID.fromString("80000000-0000-0000-0000-000000000001");
    public static final Instant TEST_TIME = Instant.parse("2026-08-22T12:00:00Z");

    private CoreTestData() {
    }

    public static Course course() {
        Course course = new Course();
        course.setId(COURSE_ID);
        course.setTitle("Smart Learning");
        course.setDescription("Core course test data");
        course.setLevel("BEGINNER");
        course.setVisibility(CourseVisibility.PRIVATE);
        course.setStatus(CourseStatus.DRAFT);
        course.setCreatedBy(OWNER_ID);
        course.setCreatedAt(TEST_TIME);
        course.setUpdatedAt(TEST_TIME);
        return course;
    }

    public static CourseResponse courseResponse(Course course) {
        return new CourseResponse(
                course.getId(), course.getTitle(), course.getDescription(), course.getLevel(),
                course.getVisibility(), course.getStatus(), course.getCreatedBy(), course.getPublishedAt(),
                course.getCreatedAt(), course.getUpdatedAt(), null
        );
    }

    public static CourseMember member(CourseMemberRole role, CourseMemberStatus status) {
        CourseMember member = new CourseMember();
        member.setId(MEMBER_ID);
        member.setCourse(course());
        member.setUserId(STUDENT_ID);
        member.setRole(role);
        member.setStatus(status);
        member.setJoinedAt(TEST_TIME);
        member.setCreatedAt(TEST_TIME);
        member.setUpdatedAt(TEST_TIME);
        return member;
    }

    public static CourseMemberResponse memberResponse(CourseMember member) {
        return new CourseMemberResponse(
                member.getId(), member.getCourse().getId(), member.getUserId(), member.getRole(),
                member.getStatus(), member.getJoinedAt(), member.getInvitedBy(), member.getRemovedAt(),
                member.getCreatedAt()
        );
    }

    public static AccessCode accessCode() {
        AccessCode code = new AccessCode();
        code.setId(ACCESS_CODE_ID);
        code.setCourse(course());
        code.setCodeHash("encoded-code");
        code.setCodeHint("1234");
        code.setActive(true);
        code.setCreatedBy(OWNER_ID);
        code.setCreatedAt(TEST_TIME);
        code.setUpdatedAt(TEST_TIME);
        return code;
    }

    public static Document document() {
        Document document = new Document();
        document.setId(DOCUMENT_ID);
        document.setCourse(course());
        document.setTitle("Document title");
        document.setDescription("Document description");
        document.setLifecycleStatus(DocumentLifecycleStatus.ACTIVE);
        document.setUploadedBy(OWNER_ID);
        document.setCreatedAt(TEST_TIME);
        document.setUpdatedAt(TEST_TIME);
        return document;
    }

    public static DocumentVersion documentVersion() {
        DocumentVersion version = new DocumentVersion();
        version.setId(VERSION_ID);
        version.setDocument(document());
        version.setVersionNumber(1);
        version.setFileName("lesson.pdf");
        version.setFileSize(1024L);
        version.setMimeType("application/pdf");
        version.setStorageBucket("smart-learning-test");
        version.setStorageKey("core/courses/course/documents/lesson.pdf");
        version.setProcessingStatus(DocumentProcessingStatus.QUEUED);
        version.setUploadedBy(OWNER_ID);
        version.setCreatedAt(TEST_TIME);
        version.setUpdatedAt(TEST_TIME);
        return version;
    }

    public static DocumentProcessingJob processingJob(ProcessingJobStatus status) {
        DocumentProcessingJob job = new DocumentProcessingJob();
        job.setId(PROCESSING_JOB_ID);
        job.setDocumentVersion(documentVersion());
        job.setStatus(status);
        job.setCreatedAt(TEST_TIME);
        job.setUpdatedAt(TEST_TIME);
        return job;
    }

    public static DocumentResponse documentResponse() {
        return new DocumentResponse(
                DOCUMENT_ID, COURSE_ID, null, null, "Document title", "Document description",
                DocumentLifecycleStatus.ACTIVE, OWNER_ID, null, TEST_TIME, TEST_TIME
        );
    }

    public static DocumentIngestionCompletedEvent completedEvent(UUID jobId, UUID versionId) {
        return new DocumentIngestionCompletedEvent(
                EVENT_ID, 1, TEST_TIME, EVENT_ID, jobId, versionId, 3, "embedding-model"
        );
    }

    public static DocumentIngestionFailedEvent failedEvent(UUID jobId, UUID versionId) {
        return new DocumentIngestionFailedEvent(
                EVENT_ID, 1, TEST_TIME, EVENT_ID, jobId, versionId, "EXTRACTION_ERROR", "Cannot parse file"
        );
    }
}
