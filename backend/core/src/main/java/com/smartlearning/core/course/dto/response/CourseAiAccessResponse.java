package com.smartlearning.core.course.dto.response;

import java.util.List;
import java.util.UUID;

public record CourseAiAccessResponse(
        CourseAiAccessLevel accessLevel,
        CoursePreviewContext preview
) {
    public static CourseAiAccessResponse full() {
        return new CourseAiAccessResponse(
                CourseAiAccessLevel.FULL, null);
    }

    public static CourseAiAccessResponse denied() {
        return new CourseAiAccessResponse(CourseAiAccessLevel.DENIED, null);
    }

    public static CourseAiAccessResponse preview(CoursePreviewContext preview) {
        return new CourseAiAccessResponse(CourseAiAccessLevel.PREVIEW, preview);
    }


    public record CoursePreviewContext(
            UUID courseId,
            String title,
            String description,
            String level,
            List<ChapterPreview> chapters
    ) {
    }


    public record ChapterPreview(
            UUID chapterId,
            String title,
            String description,
            String learningObjectives,
            Integer orderIndex,
            List<TopicPreview> topics
    ) {
    }


    public record TopicPreview(
            UUID topicId,
            String title,
            Integer orderIndex,
            Integer estimatedMinutes
    ) {
    }

    public enum CourseAiAccessLevel {
        DENIED,
        PREVIEW,
        FULL
    }
}