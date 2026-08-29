package com.smartlearning.core.course.service.impl;

import com.smartlearning.core.course.dto.response.CourseAiAccessResponse;
import com.smartlearning.core.course.entity.Course;
import com.smartlearning.core.course.entity.CourseChapter;
import com.smartlearning.core.course.entity.CourseTopic;
import com.smartlearning.core.course.entity.enums.CourseStatus;
import com.smartlearning.core.course.entity.enums.CourseVisibility;
import com.smartlearning.core.course.repository.CourseChapterRepository;
import com.smartlearning.core.course.repository.CourseMemberRepository;
import com.smartlearning.core.course.repository.CourseTopicRepository;
import com.smartlearning.core.course.utils.CourseUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.smartlearning.core.support.CoreTestData.COURSE_ID;
import static com.smartlearning.core.support.CoreTestData.STUDENT_ID;
import static com.smartlearning.core.support.CoreTestData.course;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseAiAccessServiceImplTest {

    private static final UUID CHAPTER_ID = UUID.fromString("90000000-0000-0000-0000-000000000001");
    private static final UUID TOPIC_ID = UUID.fromString("91000000-0000-0000-0000-000000000001");

    @Mock
    private CourseUtils courseUtils;
    @Mock
    private CourseMemberRepository memberRepository;
    @Mock
    private CourseChapterRepository chapterRepository;
    @Mock
    private CourseTopicRepository topicRepository;
    @InjectMocks
    private CourseAiAccessServiceImpl accessService;

    @Test
    void getAccess_returnsAllNonDeletedContentForPublishedPublicCourse() {
        Course publishedCourse = course();
        publishedCourse.setVisibility(CourseVisibility.PUBLIC);
        publishedCourse.setStatus(CourseStatus.PUBLISHED);
        CourseChapter chapter = chapter(publishedCourse);
        CourseTopic topic = topic(chapter);

        when(courseUtils.requireCourse(COURSE_ID)).thenReturn(publishedCourse);
        when(memberRepository.findByCourseIdAndUserId(COURSE_ID, STUDENT_ID))
                .thenReturn(Optional.empty());
        when(chapterRepository.findAllByCourseIdAndDeletedAtIsNullOrderByOrderIndexAsc(COURSE_ID))
                .thenReturn(List.of(chapter));
        when(topicRepository.findAllByChapterIdAndDeletedAtIsNullOrderByOrderIndexAsc(CHAPTER_ID))
                .thenReturn(List.of(topic));

        CourseAiAccessResponse result = accessService.getAccess(COURSE_ID, STUDENT_ID);

        assertThat(result.accessLevel()).isEqualTo(CourseAiAccessResponse.CourseAiAccessLevel.PREVIEW);
        assertThat(result.preview().chapters()).singleElement().satisfies(chapterPreview -> {
            assertThat(chapterPreview.chapterId()).isEqualTo(CHAPTER_ID);
            assertThat(chapterPreview.topics()).singleElement()
                    .extracting(CourseAiAccessResponse.TopicPreview::topicId)
                    .isEqualTo(TOPIC_ID);
        });
        verify(chapterRepository).findAllByCourseIdAndDeletedAtIsNullOrderByOrderIndexAsc(COURSE_ID);
        verify(topicRepository).findAllByChapterIdAndDeletedAtIsNullOrderByOrderIndexAsc(CHAPTER_ID);
    }

    @Test
    void getAccess_deniesPreviewWhenCourseIsDraft() {
        Course draftCourse = course();
        draftCourse.setVisibility(CourseVisibility.PUBLIC);
        draftCourse.setStatus(CourseStatus.DRAFT);
        when(courseUtils.requireCourse(COURSE_ID)).thenReturn(draftCourse);
        when(memberRepository.findByCourseIdAndUserId(COURSE_ID, STUDENT_ID))
                .thenReturn(Optional.empty());

        CourseAiAccessResponse result = accessService.getAccess(COURSE_ID, STUDENT_ID);

        assertThat(result.accessLevel()).isEqualTo(CourseAiAccessResponse.CourseAiAccessLevel.DENIED);
        assertThat(result.preview()).isNull();
        verifyNoInteractions(chapterRepository, topicRepository);
    }

    private static CourseChapter chapter(Course course) {
        CourseChapter chapter = new CourseChapter();
        chapter.setId(CHAPTER_ID);
        chapter.setCourse(course);
        chapter.setTitle("Chương 1");
        chapter.setOrderIndex(0);
        return chapter;
    }

    private static CourseTopic topic(CourseChapter chapter) {
        CourseTopic topic = new CourseTopic();
        topic.setId(TOPIC_ID);
        topic.setChapter(chapter);
        topic.setTitle("Chủ đề 1");
        topic.setOrderIndex(0);
        return topic;
    }
}
