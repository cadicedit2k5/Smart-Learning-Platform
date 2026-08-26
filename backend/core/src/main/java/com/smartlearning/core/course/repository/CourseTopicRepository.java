package com.smartlearning.core.course.repository;

import com.smartlearning.core.course.entity.CourseTopic;
import com.smartlearning.core.course.entity.enums.CourseContentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CourseTopicRepository extends JpaRepository<CourseTopic, UUID> {
    List<CourseTopic> findAllByChapterIdAndStatusAndDeletedAtIsNullOrderByOrderIndexAsc(
            UUID chapterId,
            CourseContentStatus status
    );
}
