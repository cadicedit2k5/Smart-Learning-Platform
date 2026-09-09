package com.smartlearning.core.course.service.impl;

import com.smartlearning.core.course.dto.request.MyCourseFilterRequest;
import com.smartlearning.core.course.dto.request.PublicCourseFilterRequest;
import com.smartlearning.core.course.dto.response.CourseResponse;
import com.smartlearning.core.course.dto.response.PublicCourseResponse;
import com.smartlearning.core.course.entity.Course;
import com.smartlearning.core.course.entity.CourseMember;
import com.smartlearning.core.course.entity.enums.CourseMemberRole;
import com.smartlearning.core.course.entity.enums.CourseMemberStatus;
import com.smartlearning.core.course.entity.enums.CourseStatus;
import com.smartlearning.core.course.entity.enums.CourseVisibility;
import com.smartlearning.core.course.repository.CourseMemberRepository;
import com.smartlearning.core.course.repository.CourseRepository;
import com.smartlearning.core.course.service.CourseService;
import com.smartlearning.core.support.CoreIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.UUID;

import static com.smartlearning.core.support.CoreTestData.OWNER_ID;
import static com.smartlearning.core.support.CoreTestData.STUDENT_ID;
import static com.smartlearning.core.support.CoreTestData.course;
import static org.assertj.core.api.Assertions.assertThat;

class CourseServiceIntegrationTest extends CoreIntegrationTest {

    @Autowired
    private CourseService courseService;
    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private CourseMemberRepository memberRepository;

    @Test
    void getMyCourses_combinesActiveMembershipKeywordStatusAndSoftDeleteFilters() {
        Course matching = persistCourse("Java basics", CourseStatus.PUBLISHED, CourseVisibility.INVITE_ONLY);
        addMember(matching, STUDENT_ID, CourseMemberStatus.ACTIVE);
        addMember(matching, OWNER_ID, CourseMemberStatus.ACTIVE);
        addMember(persistCourse("Java pending", CourseStatus.PUBLISHED, CourseVisibility.PUBLIC),
                STUDENT_ID, CourseMemberStatus.PENDING);
        addMember(persistCourse("Java removed", CourseStatus.PUBLISHED, CourseVisibility.PUBLIC),
                STUDENT_ID, CourseMemberStatus.REMOVED);
        addMember(persistCourse("Java other user", CourseStatus.PUBLISHED, CourseVisibility.PUBLIC),
                OWNER_ID, CourseMemberStatus.ACTIVE);
        addMember(persistCourse("Java draft", CourseStatus.DRAFT, CourseVisibility.PUBLIC),
                STUDENT_ID, CourseMemberStatus.ACTIVE);
        addMember(persistCourse("Design basics", CourseStatus.PUBLISHED, CourseVisibility.PUBLIC),
                STUDENT_ID, CourseMemberStatus.ACTIVE);
        Course deleted = persistCourse("Java deleted", CourseStatus.PUBLISHED, CourseVisibility.PUBLIC);
        deleted.setDeletedAt(Instant.now());
        addMember(deleted, STUDENT_ID, CourseMemberStatus.ACTIVE);

        MyCourseFilterRequest request = new MyCourseFilterRequest();
        request.setKeyword("  JAVA  ");
        request.setStatus(CourseStatus.PUBLISHED);

        var result = courseService.getMyCourses(STUDENT_ID, request);

        assertThat(result.getContent()).extracting(CourseResponse::id).containsExactly(matching.getId());
        assertThat(result.getPageable().getTotalElements()).isEqualTo(1);
    }

    @Test
    void getPublicCourses_filtersAndSortsPublishedCoursesWithMembershipStatus() {
        Course older = persistCourse("Java basics", CourseStatus.PUBLISHED, CourseVisibility.PUBLIC);
        older.setPublishedAt(Instant.parse("2026-09-01T00:00:00Z"));
        Course newer = persistCourse("Advanced topics", CourseStatus.PUBLISHED, CourseVisibility.PUBLIC);
        newer.setDescription("Java techniques");
        newer.setPublishedAt(Instant.parse("2026-09-02T00:00:00Z"));
        addMember(newer, STUDENT_ID, CourseMemberStatus.PENDING);
        addMember(older, OWNER_ID, CourseMemberStatus.ACTIVE);
        persistCourse("Java private", CourseStatus.PUBLISHED, CourseVisibility.INVITE_ONLY);
        persistCourse("Java draft", CourseStatus.DRAFT, CourseVisibility.PUBLIC);
        persistCourse("Java archived", CourseStatus.ARCHIVED, CourseVisibility.PUBLIC);
        persistCourse("Design basics", CourseStatus.PUBLISHED, CourseVisibility.PUBLIC);
        Course deleted = persistCourse("Java deleted", CourseStatus.PUBLISHED, CourseVisibility.PUBLIC);
        deleted.setDeletedAt(Instant.now());

        PublicCourseFilterRequest request = new PublicCourseFilterRequest();
        request.setKeyword("  JAVA  ");

        var result = courseService.getPublicCourses(request, STUDENT_ID);

        assertThat(result.getContent()).extracting(PublicCourseResponse::id)
                .containsExactly(newer.getId(), older.getId());
        assertThat(result.getContent()).extracting(PublicCourseResponse::currentUserMembershipStatus)
                .containsExactly(CourseMemberStatus.PENDING, null);
        assertThat(result.getPageable().getTotalElements()).isEqualTo(2);
    }

    private Course persistCourse(String title, CourseStatus status, CourseVisibility visibility) {
        Course value = course();
        value.setId(null);
        value.setTitle(title);
        value.setStatus(status);
        value.setVisibility(visibility);
        return courseRepository.saveAndFlush(value);
    }

    private void addMember(Course course, UUID userId, CourseMemberStatus status) {
        CourseMember member = new CourseMember();
        member.setCourse(course);
        member.setUserId(userId);
        member.setRole(CourseMemberRole.STUDENT);
        member.setStatus(status);
        memberRepository.saveAndFlush(member);
    }
}
