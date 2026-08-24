package com.smartlearning.core.course.service.impl;

import com.smartlearning.core.course.dto.response.CourseAiAccessResponse;
import com.smartlearning.core.course.entity.Course;
import com.smartlearning.core.course.entity.CourseChapter;
import com.smartlearning.core.course.entity.CourseMember;
import com.smartlearning.core.course.entity.enums.CourseContentStatus;
import com.smartlearning.core.course.entity.enums.CourseMemberStatus;
import com.smartlearning.core.course.entity.enums.CourseStatus;
import com.smartlearning.core.course.entity.enums.CourseVisibility;
import com.smartlearning.core.course.repository.CourseChapterRepository;
import com.smartlearning.core.course.repository.CourseMemberRepository;
import com.smartlearning.core.course.repository.CourseTopicRepository;
import com.smartlearning.core.course.service.CourseAiAccessService;
import com.smartlearning.core.course.utils.CourseUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CourseAiAccessServiceImpl implements CourseAiAccessService {

    private final CourseUtils courseUtils;

    private final CourseMemberRepository memberRepository;
    private final CourseChapterRepository chapterRepository;
    private final CourseTopicRepository topicRepository;

    @Transactional(readOnly = true)
    public CourseAiAccessResponse getAccess(UUID courseId, UUID userId) {

        Course course = courseUtils.requireCourse(courseId);

        Optional<CourseMember> member = memberRepository.findByCourseIdAndUserId(courseId, userId);

        boolean activeMember = member.isPresent() && member.get().getStatus() == CourseMemberStatus.ACTIVE;

//      Đã tham gia
        if (activeMember) {
            return CourseAiAccessResponse.full();
        }
        boolean previewAllowed = course.getVisibility() == CourseVisibility.PUBLIC
                && course.getStatus() == CourseStatus.PUBLISHED;

//        Chưa được publish
        if (!previewAllowed) {
            return CourseAiAccessResponse.denied();
        }

        return CourseAiAccessResponse.preview(
                buildPreviewContext(course)
        );
    }

    private CourseAiAccessResponse.CoursePreviewContext buildPreviewContext(Course course) {

        List<CourseChapter> chapters = chapterRepository.findAllByCourseIdAndStatusAndDeletedAtIsNullOrderByOrderIndexAsc(
                                course.getId(),
                                CourseContentStatus.PUBLISHED
                        );

        List<CourseAiAccessResponse.ChapterPreview> chapterResponses = chapters.stream()
                        .map(this::toChapterPreview).toList();

        return new CourseAiAccessResponse.CoursePreviewContext(
                course.getId(),
                course.getTitle(),
                course.getDescription(),
                course.getLevel(),
                chapterResponses
        );
    }


    private CourseAiAccessResponse.ChapterPreview toChapterPreview(CourseChapter chapter) {

        List<CourseAiAccessResponse.TopicPreview> topics = topicRepository.findAllByChapterIdAndStatusAndDeletedAtIsNullOrderByOrderIndexAsc(
                                chapter.getId(),
                                CourseContentStatus.PUBLISHED)
                .stream().map(topic ->
                                new CourseAiAccessResponse.TopicPreview(
                                        topic.getId(),
                                        topic.getTitle(),
                                        topic.getOrderIndex(),
                                        topic.getEstimatedMinutes()
                                )
                        ).toList();

        return new CourseAiAccessResponse.ChapterPreview(
                chapter.getId(),
                chapter.getTitle(),
                chapter.getDescription(),
                chapter.getLearningObjectives(),
                chapter.getOrderIndex(),
                topics
        );
    }
}