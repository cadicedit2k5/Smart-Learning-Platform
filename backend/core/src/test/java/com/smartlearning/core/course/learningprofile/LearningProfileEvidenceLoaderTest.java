package com.smartlearning.core.course.learningprofile;

import com.smartlearning.core.course.entity.CourseMember;
import com.smartlearning.core.course.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LearningProfileEvidenceLoaderTest {

    @Mock
    private CourseMemberRepository memberRepository;

    @Mock
    private CourseTopicRepository topicRepository;

    @Mock
    private LearningProgressRepository progressRepository;

    @Mock
    private AssessmentAttemptRepository attemptRepository;

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private AssignmentSubmissionRepository submissionRepository;

    @InjectMocks
    private LearningProfileEvidenceLoader loader;

    @Test
    void load_loadsMultipleStudentsWithBulkQueries() {
        UUID courseId = UUID.randomUUID();
        UUID student1 = UUID.randomUUID();
        UUID student2 = UUID.randomUUID();

        CourseMember member1 = member(student1);
        CourseMember member2 = member(student2);

        List<UUID> studentIds =
                List.of(student1, student2);

        when(memberRepository.findAllByCourseIdAndUserIdIn(
                courseId,
                studentIds
        )).thenReturn(List.of(member1, member2));

        when(topicRepository
                .countByChapterCourseIdAndDeletedAtIsNullAndChapterDeletedAtIsNull(
                        courseId
                ))
                .thenReturn(5L);

        when(progressRepository.findCourseProgressForUsers(
                studentIds,
                courseId
        )).thenReturn(List.of());

        when(attemptRepository.findCourseAttemptsForStudents(
                studentIds,
                courseId
        )).thenReturn(List.of());

        when(assignmentRepository
                .countByCourseIdAndDeletedAtIsNullAndStatusNot(
                        courseId,
                        com.smartlearning.core.course.entity.enums.AssignmentStatus.DRAFT
                ))
                .thenReturn(3L);

        when(submissionRepository.findCourseSubmissionsForStudents(
                studentIds,
                courseId,
                com.smartlearning.core.course.entity.enums.AssignmentStatus.DRAFT
        )).thenReturn(List.of());

        Map<UUID, LearningProfileEvidence> result =
                loader.load(courseId, studentIds);

        assertThat(result).containsOnlyKeys(
                student1,
                student2
        );

        assertThat(result.get(student1).totalTopics())
                .isEqualTo(5);

        assertThat(result.get(student1).totalAssignments())
                .isEqualTo(3);

        verify(memberRepository, times(1))
                .findAllByCourseIdAndUserIdIn(
                        courseId,
                        studentIds
                );

        verify(progressRepository, times(1))
                .findCourseProgressForUsers(
                        studentIds,
                        courseId
                );

        verify(attemptRepository, times(1))
                .findCourseAttemptsForStudents(
                        studentIds,
                        courseId
                );

        verify(submissionRepository, times(1))
                .findCourseSubmissionsForStudents(
                        studentIds,
                        courseId,
                        com.smartlearning.core.course.entity.enums.AssignmentStatus.DRAFT
                );
    }

    private CourseMember member(UUID studentId) {
        CourseMember member = new CourseMember();
        member.setUserId(studentId);
        return member;
    }
}