package com.smartlearning.core.course.service.impl;

import com.smartlearning.common.error.ApplicationException;
import com.smartlearning.common.error.CommonErrorCode;
import com.smartlearning.common.dto.request.PagingRequest;
import com.smartlearning.common.dto.response.pagination.PagingResponse;
import com.smartlearning.core.course.dto.request.CourseCreateRequest;
import com.smartlearning.core.course.dto.request.CourseUpdateRequest;
import com.smartlearning.core.course.dto.response.CourseResponse;
import com.smartlearning.core.course.dto.response.PublicCourseResponse;
import com.smartlearning.core.course.entity.Course;
import com.smartlearning.core.course.entity.CourseMember;
import com.smartlearning.core.course.entity.enums.CourseMemberRole;
import com.smartlearning.core.course.entity.enums.CourseMemberStatus;
import com.smartlearning.core.course.entity.enums.CourseStatus;
import com.smartlearning.core.course.entity.enums.CourseVisibility;
import com.smartlearning.core.course.mapper.CourseMapper;
import com.smartlearning.core.course.repository.CourseMemberRepository;
import com.smartlearning.core.course.repository.CourseRepository;
import com.smartlearning.core.course.sercurity.CourseAccessPolicy;
import com.smartlearning.core.course.utils.CourseUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.smartlearning.core.support.CoreTestData.COURSE_ID;
import static com.smartlearning.core.support.CoreTestData.OWNER_ID;
import static com.smartlearning.core.support.CoreTestData.course;
import static com.smartlearning.core.support.CoreTestData.courseResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseServiceImplTest {

    @Mock
    private CourseMapper courseMapper;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private CourseMemberRepository memberRepository;
    @Mock
    private CourseAccessPolicy courseAccessPolicy;
    @Mock
    private CourseUtils courseUtils;
    @InjectMocks
    private CourseServiceImpl courseService;

    @Test
    void createCourse_persistsCourseThenCreatesActiveOwner() {
        CourseCreateRequest request = new CourseCreateRequest(
                "Smart Learning", "Description", "BEGINNER", null
        );
        Course mappedCourse = course();
        mappedCourse.setVisibility(null);
        mappedCourse.setStatus(null);
        mappedCourse.setCreatedBy(null);
        CourseResponse expected = courseResponse(course());

        when(courseMapper.toEntity(request)).thenReturn(mappedCourse);
        when(courseRepository.save(mappedCourse)).thenReturn(mappedCourse);
        when(courseMapper.toResponse(mappedCourse)).thenReturn(expected);

        Instant beforeCall = Instant.now();
        CourseResponse result = courseService.createCourse(request, OWNER_ID);
        Instant afterCall = Instant.now();

        assertThat(result)
                .usingRecursiveComparison()
                .ignoringFields("currentUserRole")
                .isEqualTo(expected);
        assertThat(result.currentUserRole()).isEqualTo(CourseMemberRole.OWNER);
        assertThat(mappedCourse.getCreatedBy()).isEqualTo(OWNER_ID);
        assertThat(mappedCourse.getStatus()).isEqualTo(CourseStatus.DRAFT);
        assertThat(mappedCourse.getVisibility()).isEqualTo(CourseVisibility.INVITE_ONLY);

        ArgumentCaptor<CourseMember> ownerCaptor = ArgumentCaptor.forClass(CourseMember.class);
        InOrder order = inOrder(courseRepository, memberRepository, courseMapper);
        order.verify(courseRepository).save(mappedCourse);
        order.verify(memberRepository).save(ownerCaptor.capture());
        order.verify(courseMapper).toResponse(mappedCourse);

        CourseMember owner = ownerCaptor.getValue();
        assertThat(owner.getCourse()).isSameAs(mappedCourse);
        assertThat(owner.getUserId()).isEqualTo(OWNER_ID);
        assertThat(owner.getRole()).isEqualTo(CourseMemberRole.OWNER);
        assertThat(owner.getStatus()).isEqualTo(CourseMemberStatus.ACTIVE);
        assertThat(owner.getJoinedAt()).isBetween(beforeCall, afterCall);
    }

    @Test
    void getCourse_skipsMembershipCheckForPublicCourse() {
        Course publicCourse = course();
        publicCourse.setVisibility(CourseVisibility.PUBLIC);
        publicCourse.setStatus(CourseStatus.PUBLISHED);
        CourseResponse expected = courseResponse(publicCourse);
        when(courseUtils.requireCourse(COURSE_ID)).thenReturn(publicCourse);
        when(courseMapper.toResponse(publicCourse)).thenReturn(expected);

        assertThat(courseService.getCourse(COURSE_ID, OWNER_ID)).isEqualTo(expected);

        verifyNoInteractions(courseAccessPolicy);
        verify(memberRepository).findByCourseIdAndUserId(COURSE_ID, OWNER_ID);
    }

    @Test
    void getCourse_requiresActiveMembershipForInviteOnlyOrUnpublishedCourse() {
        Course inviteOnlyCourse = course();
        CourseResponse expected = courseResponse(inviteOnlyCourse);
        CourseMember activeOwner = com.smartlearning.core.support.CoreTestData.member(
                CourseMemberRole.OWNER,
                CourseMemberStatus.ACTIVE
        );
        when(courseUtils.requireCourse(COURSE_ID)).thenReturn(inviteOnlyCourse);
        when(courseAccessPolicy.requireActiveMember(COURSE_ID, OWNER_ID)).thenReturn(activeOwner);
        when(courseMapper.toResponse(inviteOnlyCourse)).thenReturn(expected);

        CourseResponse result = courseService.getCourse(COURSE_ID, OWNER_ID);
        assertThat(result.currentUserRole()).isEqualTo(CourseMemberRole.OWNER);

        InOrder order = inOrder(courseUtils, courseAccessPolicy, courseMapper);
        order.verify(courseUtils).requireCourse(COURSE_ID);
        order.verify(courseAccessPolicy).requireActiveMember(COURSE_ID, OWNER_ID);
        order.verify(courseMapper).toResponse(inviteOnlyCourse);
    }

    @Test
    void getCourse_requiresMembershipForPublicDraft() {
        Course publicDraft = course();
        publicDraft.setVisibility(CourseVisibility.PUBLIC);
        CourseMember activeOwner = com.smartlearning.core.support.CoreTestData.member(
                CourseMemberRole.OWNER,
                CourseMemberStatus.ACTIVE
        );
        when(courseUtils.requireCourse(COURSE_ID)).thenReturn(publicDraft);
        when(courseAccessPolicy.requireActiveMember(COURSE_ID, OWNER_ID)).thenReturn(activeOwner);
        when(courseMapper.toResponse(publicDraft)).thenReturn(courseResponse(publicDraft));

        assertThat(courseService.getCourse(COURSE_ID, OWNER_ID).currentUserRole())
                .isEqualTo(CourseMemberRole.OWNER);

        verify(courseAccessPolicy).requireActiveMember(COURSE_ID, OWNER_ID);
    }

    @Test
    void getMyCourses_mapsOnlyActiveMembershipCourses() {
        Course first = course();
        Course second = course();
        second.setId(java.util.UUID.randomUUID());
        second.setTitle("Second course");
        CourseResponse firstResponse = courseResponse(first);
        CourseResponse secondResponse = courseResponse(second);
        CourseMember firstMembership = com.smartlearning.core.support.CoreTestData.member(
                CourseMemberRole.OWNER,
                CourseMemberStatus.ACTIVE
        );
        firstMembership.setCourse(first);
        CourseMember secondMembership = com.smartlearning.core.support.CoreTestData.member(
                CourseMemberRole.STUDENT,
                CourseMemberStatus.ACTIVE
        );
        secondMembership.setCourse(second);
        when(memberRepository.findAllByUserIdAndStatusAndCourseDeletedAtIsNullOrderByCourseUpdatedAtDesc(
                OWNER_ID,
                CourseMemberStatus.ACTIVE
        )).thenReturn(List.of(firstMembership, secondMembership));
        when(courseMapper.toResponse(first)).thenReturn(firstResponse);
        when(courseMapper.toResponse(second)).thenReturn(secondResponse);

        List<CourseResponse> result = courseService.getMyCourses(OWNER_ID);
        assertThat(result).extracting(CourseResponse::currentUserRole)
                .containsExactly(CourseMemberRole.OWNER, CourseMemberRole.STUDENT);
        assertThat(result).extracting(CourseResponse::id)
                .containsExactly(firstResponse.id(), secondResponse.id());
    }

    @Test
    void getPublicCourses_returnsOnlyRepositoryPageAndCurrentMembershipStatus() {
        Course first = course();
        first.setVisibility(CourseVisibility.PUBLIC);
        first.setStatus(CourseStatus.PUBLISHED);
        first.setPublishedAt(Instant.parse("2026-08-28T12:00:00Z"));
        Course second = course();
        second.setId(UUID.randomUUID());
        second.setTitle("Second public course");
        second.setVisibility(CourseVisibility.PUBLIC);
        second.setStatus(CourseStatus.PUBLISHED);
        second.setPublishedAt(Instant.parse("2026-08-27T12:00:00Z"));
        CourseMember pending = com.smartlearning.core.support.CoreTestData.member(
                CourseMemberRole.STUDENT,
                CourseMemberStatus.PENDING
        );
        pending.setCourse(first);
        PagingRequest pagingRequest = new PagingRequest();
        PageRequest pageable = PageRequest.of(0, 10);
        when(courseRepository.findAllByVisibilityAndStatusAndDeletedAtIsNullOrderByPublishedAtDesc(
                eq(CourseVisibility.PUBLIC),
                eq(CourseStatus.PUBLISHED),
                any()
        )).thenReturn(new PageImpl<>(List.of(first, second), pageable, 2));
        when(memberRepository.findAllByCourseIdInAndUserId(
                List.of(first.getId(), second.getId()),
                OWNER_ID
        )).thenReturn(List.of(pending));

        PagingResponse<PublicCourseResponse> result = courseService.getPublicCourses(
                pagingRequest,
                OWNER_ID
        );

        assertThat(result.getContent()).extracting(PublicCourseResponse::id)
                .containsExactly(first.getId(), second.getId());
        assertThat(result.getContent()).extracting(PublicCourseResponse::currentUserMembershipStatus)
                .containsExactly(CourseMemberStatus.PENDING, null);
    }

    @Test
    void updateCourse_appliesPartialUpdateAndTrimsTitle() {
        CourseUpdateRequest request = new CourseUpdateRequest(
                "  Updated title  ", null, null, CourseVisibility.PUBLIC
        );
        Course existing = course();
        CourseResponse expected = courseResponse(existing);
        when(courseUtils.requireCourse(COURSE_ID)).thenReturn(existing);
        org.mockito.Mockito.doAnswer(invocation -> {
            existing.setTitle(request.title());
            existing.setVisibility(request.visibility());
            return null;
        }).when(courseMapper).partialUpdate(request, existing);
        when(courseMapper.toResponse(existing)).thenReturn(expected);

        assertThat(courseService.updateCourse(COURSE_ID, request, OWNER_ID)).isEqualTo(expected);

        assertThat(existing.getTitle()).isEqualTo("Updated title");
        assertThat(existing.getVisibility()).isEqualTo(CourseVisibility.PUBLIC);
        InOrder order = inOrder(courseUtils, courseMapper);
        order.verify(courseUtils).requireCourse(COURSE_ID);
        order.verify(courseMapper).partialUpdate(request, existing);
        order.verify(courseMapper).toResponse(existing);
        verify(courseRepository, never()).save(any());
    }

    @Test
    void publishCourse_marksDraftAsPublished() {
        Course existing = course();
        CourseResponse expected = courseResponse(existing);
        when(courseUtils.requireCourse(COURSE_ID)).thenReturn(existing);
        when(courseMapper.toResponse(existing)).thenReturn(expected);

        Instant beforeCall = Instant.now();
        assertThat(courseService.publishCourse(COURSE_ID, OWNER_ID)).isEqualTo(expected);
        Instant afterCall = Instant.now();

        assertThat(existing.getStatus()).isEqualTo(CourseStatus.PUBLISHED);
        assertThat(existing.getPublishedAt()).isBetween(beforeCall, afterCall);
        verify(courseRepository, never()).save(any());
    }

    @Test
    void publishCourse_rejectsAlreadyPublishedCourse() {
        Course existing = course();
        existing.setStatus(CourseStatus.PUBLISHED);
        when(courseUtils.requireCourse(COURSE_ID)).thenReturn(existing);

        assertError(
                () -> courseService.publishCourse(COURSE_ID, OWNER_ID),
                CommonErrorCode.DATA_CONFLICT
        );

        verifyNoInteractions(courseMapper);
    }

    @Test
    void deleteCourse_softDeletesWithoutRepositoryDeleteCall() {
        Course existing = course();
        when(courseUtils.requireCourse(COURSE_ID)).thenReturn(existing);

        Instant beforeCall = Instant.now();
        courseService.deleteCourse(COURSE_ID, OWNER_ID);
        Instant afterCall = Instant.now();

        assertThat(existing.getDeletedAt()).isBetween(beforeCall, afterCall);
        verify(courseRepository, never()).delete(any(Course.class));
    }

    private static void assertError(Runnable invocation, CommonErrorCode expectedCode) {
        assertThatThrownBy(invocation::run)
                .isInstanceOf(ApplicationException.class)
                .extracting(exception -> ((ApplicationException) exception).getErrorCode())
                .isEqualTo(expectedCode);
    }
}
