package com.smartlearning.core.course.learningprofile;

import com.smartlearning.core.course.entity.*;
import com.smartlearning.core.course.entity.enums.AssignmentStatus;
import com.smartlearning.core.course.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class LearningProfileEvidenceLoader {

    private final CourseMemberRepository memberRepository;
    private final CourseTopicRepository topicRepository;
    private final LearningProgressRepository progressRepository;
    private final AssessmentAttemptRepository attemptRepository;
    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository submissionRepository;

    public Map<UUID, LearningProfileEvidence> load(
            UUID courseId,
            Collection<UUID> studentIds
    ) {
        if (studentIds.isEmpty()) return Map.of();

        List<UUID> ids = studentIds.stream().distinct().toList();

        Map<UUID, CourseMember> memberships = memberRepository
                .findAllByCourseIdAndUserIdIn(courseId, ids)
                .stream()
                .collect(Collectors.toMap(
                        CourseMember::getUserId,
                        Function.identity()
                ));

        long totalTopics =
                topicRepository.countByChapterCourseIdAndDeletedAtIsNullAndChapterDeletedAtIsNull(courseId);

        Map<UUID, List<LearningProgress>> progressByStudent = progressRepository
                .findCourseProgressForUsers(ids, courseId)
                .stream()
                .collect(Collectors.groupingBy(LearningProgress::getUserId));

        Map<UUID, List<AssessmentAttempt>> attemptsByStudent = attemptRepository
                .findCourseAttemptsForStudents(ids, courseId)
                .stream()
                .collect(Collectors.groupingBy(AssessmentAttempt::getStudentId));

        long totalAssignments = assignmentRepository
                .countByCourseIdAndDeletedAtIsNullAndStatusNot(
                        courseId,
                        AssignmentStatus.DRAFT
                );

        Map<UUID, List<AssignmentSubmission>> submissionsByStudent = submissionRepository
                .findCourseSubmissionsForStudents(
                        ids,
                        courseId,
                        AssignmentStatus.DRAFT
                )
                .stream()
                .collect(Collectors.groupingBy(AssignmentSubmission::getStudentId));

        Map<UUID, LearningProfileEvidence> result = new LinkedHashMap<>();

        for (UUID studentId : ids) {
            result.put(
                    studentId,
                    new LearningProfileEvidence(
                            courseId,
                            studentId,
                            memberships.get(studentId),
                            totalTopics,
                            progressByStudent.getOrDefault(studentId, List.of()),
                            attemptsByStudent.getOrDefault(studentId, List.of()),
                            totalAssignments,
                            submissionsByStudent.getOrDefault(studentId, List.of())
                    )
            );
        }

        return result;
    }
}