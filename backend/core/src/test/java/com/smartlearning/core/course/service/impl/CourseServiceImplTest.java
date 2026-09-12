package com.smartlearning.core.course.service.impl;

import com.smartlearning.common.error.ApplicationException;
import com.smartlearning.common.error.CommonErrorCode;
import com.smartlearning.core.course.dto.request.MyCourseFilterRequest;
import com.smartlearning.core.course.dto.request.PublicCourseFilterRequest;
import com.smartlearning.common.dto.response.pagination.PagingResponse;
import com.smartlearning.core.course.dto.request.CourseCreateRequest;
import com.smartlearning.core.course.dto.request.CourseUpdateRequest;
import com.smartlearning.core.course.dto.response.CourseResponse;
import com.smartlearning.core.course.dto.response.PublicCourseDetailResponse;
import com.smartlearning.core.course.dto.response.PublicCourseResponse;
import com.smartlearning.core.course.entity.Course;
import com.smartlearning.core.course.entity.CourseChapter;
import com.smartlearning.core.course.entity.CourseMember;
import com.smartlearning.core.course.entity.CourseTopic;
import com.smartlearning.core.course.entity.enums.CourseMemberRole;
import com.smartlearning.core.course.entity.enums.CourseMemberStatus;
import com.smartlearning.core.course.entity.enums.CourseStatus;
import com.smartlearning.core.course.entity.enums.CourseVisibility;
import com.smartlearning.core.course.mapper.CourseMapper;
import com.smartlearning.core.course.repository.CourseChapterRepository;
import com.smartlearning.core.course.repository.CourseMemberRepository;
import com.smartlearning.core.course.repository.CourseRepository;
import com.smartlearning.core.course.repository.CourseTopicRepository;
import com.smartlearning.core.course.sercurity.CourseAccessPolicy;
import com.smartlearning.core.course.utils.CourseUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static com.smartlearning.core.support.CoreTestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
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
    @Mock
    private CourseChapterRepository chapterRepository;
    @Mock
    private CourseTopicRepository topicRepository;

    @BeforeEach
    void mapRoleLikeTheRealUtility() {
        lenient().when(courseUtils.withRole(
                        any(CourseResponse.class),
                        nullable(CourseMemberRole.class)
                ))
                .thenAnswer(invocation -> withRole(
                        invocation.getArgument(0),
                        invocation.getArgument(1)
                ));
    }

    @Test
    void createCourse_persistsCourseThenCreatesActiveOwner() {
        CourseCreateRequest request = new CourseCreateRequest(
                "Smart Learning", "Description", "BEGINNER", null
        );
        Course mappedCourse = course();
        mappedCourse.setVisibility(null);
        mappedCourse.setStatus(null);
        mappedCourse.setCreatedBy(null);

        when(courseMapper.toEntity(request)).thenReturn(mappedCourse);
        when(courseRepository.save(mappedCourse)).thenReturn(mappedCourse);
        when(courseMapper.toResponse(mappedCourse))
                .thenAnswer(invocation -> courseResponse(mappedCourse));

        Instant beforeCall = Instant.now();
        CourseResponse result = courseService.createCourse(request, OWNER_ID);
        Instant afterCall = Instant.now();

        assertThat(result.id()).isEqualTo(COURSE_ID);
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

    @ParameterizedTest(name = "public course with membership {0} returns role {1}")
    @MethodSource("publicMembershipScenarios")
    void getCourse_publicPublishedCourseUsesOptionalMembership(
            CourseMemberStatus membershipStatus,
            CourseMemberRole expectedRole
    ) {
        Course publicCourse = course();
        publicCourse.setVisibility(CourseVisibility.PUBLIC);
        publicCourse.setStatus(CourseStatus.PUBLISHED);
        CourseMember membership = membershipStatus == null
                ? null
                : com.smartlearning.core.support.CoreTestData.member(
                        CourseMemberRole.STUDENT,
                        membershipStatus
                );
        when(courseUtils.requireCourse(COURSE_ID)).thenReturn(publicCourse);
        when(courseMapper.toResponse(publicCourse)).thenReturn(courseResponse(publicCourse));
        when(memberRepository.findByCourseIdAndUserId(COURSE_ID, OWNER_ID))
                .thenReturn(Optional.ofNullable(membership));

        CourseResponse result = courseService.getCourse(COURSE_ID, OWNER_ID);

        assertThat(result.currentUserRole()).isEqualTo(expectedRole);

        verifyNoInteractions(courseAccessPolicy);
        verify(memberRepository).findByCourseIdAndUserId(COURSE_ID, OWNER_ID);
    }

    static Stream<Arguments> publicMembershipScenarios() {
        return Stream.of(
                Arguments.of(null, null),
                Arguments.of(CourseMemberStatus.ACTIVE, CourseMemberRole.STUDENT),
                Arguments.of(CourseMemberStatus.PENDING, null)
        );
    }

    @ParameterizedTest(name = "{0}/{1} requires an active member")
    @MethodSource("restrictedCourseScenarios")
    void getCourse_nonPublicOrUnpublishedCourseRequiresActiveMembership(
            CourseVisibility visibility,
            CourseStatus status
    ) {
        Course restrictedCourse = course();
        restrictedCourse.setVisibility(visibility);
        restrictedCourse.setStatus(status);
        CourseMember activeOwner = com.smartlearning.core.support.CoreTestData.member(
                CourseMemberRole.OWNER,
                CourseMemberStatus.ACTIVE
        );
        when(courseUtils.requireCourse(COURSE_ID)).thenReturn(restrictedCourse);
        when(courseAccessPolicy.requireActiveMember(COURSE_ID, OWNER_ID)).thenReturn(activeOwner);
        when(courseMapper.toResponse(restrictedCourse)).thenReturn(courseResponse(restrictedCourse));

        CourseResponse result = courseService.getCourse(COURSE_ID, OWNER_ID);

        assertThat(result.currentUserRole()).isEqualTo(CourseMemberRole.OWNER);

        InOrder order = inOrder(courseUtils, courseAccessPolicy, courseMapper);
        order.verify(courseUtils).requireCourse(COURSE_ID);
        order.verify(courseAccessPolicy).requireActiveMember(COURSE_ID, OWNER_ID);
        order.verify(courseMapper).toResponse(restrictedCourse);
    }

    static Stream<Arguments> restrictedCourseScenarios() {
        return Stream.of(
                Arguments.of(CourseVisibility.INVITE_ONLY, CourseStatus.DRAFT),
                Arguments.of(CourseVisibility.INVITE_ONLY, CourseStatus.PUBLISHED),
                Arguments.of(CourseVisibility.PUBLIC, CourseStatus.DRAFT)
        );
    }

    @Test
    void getPublicCourseDetail_returnsCourseOutline() {
        Course course = course();
        course.setVisibility(CourseVisibility.PUBLIC);
        course.setStatus(CourseStatus.PUBLISHED);
        course.setPublishedAt(TEST_TIME);
        course.setCoverUrl("course-cover");

        CourseChapter chapter = chapter();
        chapter.setCourse(course);

        CourseTopic topic = topic(chapter);

        CourseMember membership = member(CourseMemberRole.STUDENT, CourseMemberStatus.PENDING);
        membership.setCourse(course);

        when(courseUtils.requireCourse(COURSE_ID)).thenReturn(course);
        when(memberRepository.findByCourseIdAndUserId(COURSE_ID, STUDENT_ID))
                .thenReturn(Optional.of(membership));
        when(chapterRepository.findAllByCourseIdAndDeletedAtIsNullOrderByOrderIndexAsc(COURSE_ID))
                .thenReturn(List.of(chapter));
        when(topicRepository.findAllActiveByCourseIdOrderByPosition(COURSE_ID))
                .thenReturn(List.of(topic));
        when(courseMapper.toImageUrl(course)).thenReturn("/courses/" + COURSE_ID + "/image?v=1");

        PublicCourseDetailResponse response = courseService.getPublicCourseDetail(COURSE_ID, STUDENT_ID);

        assertThat(response.id()).isEqualTo(COURSE_ID);
        assertThat(response.currentUserMembershipStatus()).isEqualTo(CourseMemberStatus.PENDING);
        assertThat(response.chapters()).hasSize(1);
        assertThat(response.chapters().getFirst().title()).isEqualTo("Chapter 1");
        assertThat(response.chapters().getFirst().topics()).hasSize(1);
        assertThat(response.chapters().getFirst().topics().getFirst().title()).isEqualTo("Topic 1");
        assertThat(response.chapters().getFirst().topics().getFirst().estimatedMinutes()).isEqualTo(30);
    }

    @Test
    void getMyCourses_returnsMappedPageWithRequestedPagination() {
        Course first = course();
        Course second = course();
        second.setId(UUID.randomUUID());
        second.setTitle("Second course");
        MyCourseFilterRequest request = new MyCourseFilterRequest();
        request.setPage(2);
        request.getOrders().put("title", "ASC");
        var pageable = request.pageable();
        when(courseRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Course>>any(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(first, second), pageable, 8));
        when(courseMapper.toResponse(first)).thenReturn(courseResponse(first));
        when(courseMapper.toResponse(second)).thenReturn(courseResponse(second));

        PagingResponse<CourseResponse> result = courseService.getMyCourses(OWNER_ID, request);

        assertThat(result.getContent()).extracting(CourseResponse::id)
                .containsExactly(first.getId(), second.getId());
        assertThat(result.getPageable().getPage()).isEqualTo(2);
        assertThat(result.getPageable().getSize()).isEqualTo(6);
        assertThat(result.getPageable().getTotalElements()).isEqualTo(8);
        assertThat(result.getPageable().getTotalPages()).isEqualTo(2);
    }

    @Test
    void getPublicCourses_returnsPageAndCurrentMembershipStatusWithDefaultSort() {
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
                CourseMemberRole.STUDENT, CourseMemberStatus.PENDING);
        pending.setCourse(first);
        PublicCourseFilterRequest request = new PublicCourseFilterRequest();
        request.setPage(2);
        PageRequest pageable = PageRequest.of(1, 6, Sort.by(Sort.Direction.DESC, "publishedAt"));
        when(courseRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Course>>any(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(first, second), pageable, 8));
        when(memberRepository.findAllByCourseIdInAndUserId(
                List.of(first.getId(), second.getId()), OWNER_ID))
                .thenReturn(List.of(pending));

        PagingResponse<PublicCourseResponse> result = courseService.getPublicCourses(request, OWNER_ID);

        assertThat(result.getContent()).extracting(PublicCourseResponse::id)
                .containsExactly(first.getId(), second.getId());
        assertThat(result.getContent()).extracting(PublicCourseResponse::currentUserMembershipStatus)
                .containsExactly(CourseMemberStatus.PENDING, null);
        assertThat(result.getPageable().getPage()).isEqualTo(2);
        assertThat(result.getPageable().getSize()).isEqualTo(6);
        assertThat(result.getPageable().getTotalElements()).isEqualTo(8);
        assertThat(result.getPageable().getTotalPages()).isEqualTo(2);
    }

    @Test
    void getPublicCourses_preservesRequestedSortAndSkipsMembershipLookupForEmptyPage() {
        PublicCourseFilterRequest request = new PublicCourseFilterRequest();
        request.getOrders().put("title", "ASC");
        var pageable = request.pageable();
        when(courseRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Course>>any(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        PagingResponse<PublicCourseResponse> result = courseService.getPublicCourses(request, OWNER_ID);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getPageable().getTotalElements()).isZero();
        verifyNoInteractions(memberRepository);
    }
    @Test
    void updateCourse_appliesPartialUpdateAndTrimsTitle() {
        CourseUpdateRequest request = new CourseUpdateRequest(
                "  Updated title  ", null, null, CourseVisibility.PUBLIC
        );
        Course existing = course();
        when(courseUtils.requireCourse(COURSE_ID)).thenReturn(existing);
        when(courseAccessPolicy.requireOwner(COURSE_ID, OWNER_ID)).thenReturn(
                com.smartlearning.core.support.CoreTestData.member(
                        CourseMemberRole.OWNER,
                        CourseMemberStatus.ACTIVE
                )
        );
        org.mockito.Mockito.doAnswer(invocation -> {
            existing.setTitle(request.title());
            existing.setVisibility(request.visibility());
            return null;
        }).when(courseMapper).partialUpdate(request, existing);
        when(courseMapper.toResponse(existing))
                .thenAnswer(invocation -> courseResponse(existing));

        CourseResponse result = courseService.updateCourse(COURSE_ID, request, OWNER_ID);

        assertThat(result.title()).isEqualTo("Updated title");
        assertThat(result.currentUserRole()).isEqualTo(CourseMemberRole.OWNER);
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
        when(courseUtils.requireCourse(COURSE_ID)).thenReturn(existing);
        when(courseAccessPolicy.requireOwner(COURSE_ID, OWNER_ID)).thenReturn(
                com.smartlearning.core.support.CoreTestData.member(
                        CourseMemberRole.OWNER,
                        CourseMemberStatus.ACTIVE
                )
        );
        when(courseMapper.toResponse(existing))
                .thenAnswer(invocation -> courseResponse(existing));

        Instant beforeCall = Instant.now();
        CourseResponse result = courseService.publishCourse(COURSE_ID, OWNER_ID);
        Instant afterCall = Instant.now();

        assertThat(result.status()).isEqualTo(CourseStatus.PUBLISHED);
        assertThat(result.currentUserRole()).isEqualTo(CourseMemberRole.OWNER);
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

    private static CourseResponse withRole(CourseResponse response, CourseMemberRole role) {
        return new CourseResponse(
                response.id(), response.title(), response.description(), response.level(),
                response.visibility(), response.status(), response.createdBy(), response.publishedAt(),
                response.createdAt(), response.updatedAt(), role
        );
    }
}
