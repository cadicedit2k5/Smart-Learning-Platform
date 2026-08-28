package com.smartlearning.core.course.service.impl;

import com.smartlearning.common.error.ApplicationException;
import com.smartlearning.common.error.CommonErrorCode;
import com.smartlearning.core.course.dto.request.CourseMemberCreateRequest;
import com.smartlearning.core.course.dto.response.CourseMemberResponse;
import com.smartlearning.core.course.entity.Course;
import com.smartlearning.core.course.entity.CourseMember;
import com.smartlearning.core.course.entity.enums.CourseMemberRole;
import com.smartlearning.core.course.entity.enums.CourseMemberStatus;
import com.smartlearning.core.course.entity.enums.CourseStatus;
import com.smartlearning.core.course.entity.enums.CourseVisibility;
import com.smartlearning.core.course.mapper.CourseMemberMapper;
import com.smartlearning.core.course.repository.CourseMemberRepository;
import com.smartlearning.core.course.sercurity.CourseAccessPolicy;
import com.smartlearning.core.course.utils.CourseUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.smartlearning.core.support.CoreTestData.COURSE_ID;
import static com.smartlearning.core.support.CoreTestData.MEMBER_ID;
import static com.smartlearning.core.support.CoreTestData.OWNER_ID;
import static com.smartlearning.core.support.CoreTestData.STUDENT_ID;
import static com.smartlearning.core.support.CoreTestData.course;
import static com.smartlearning.core.support.CoreTestData.member;
import static com.smartlearning.core.support.CoreTestData.memberResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseMemberServiceImplTest {

    @Mock
    private CourseMemberRepository memberRepository;
    @Mock
    private CourseMemberMapper memberMapper;
    @Mock
    private CourseUtils courseUtils;
    @Mock
    private CourseAccessPolicy courseAccessPolicy;
    @InjectMocks
    private CourseMemberServiceImpl memberService;

    @Test
    void addMember_invitesStudentIntoPublishedInviteOnlyCourse() {
        Course inviteOnlyCourse = publishedCourse(CourseVisibility.INVITE_ONLY);
        CourseMemberCreateRequest request = new CourseMemberCreateRequest(STUDENT_ID);
        when(courseUtils.requireCourse(COURSE_ID)).thenReturn(inviteOnlyCourse);
        when(memberRepository.findByCourseIdAndUserId(COURSE_ID, STUDENT_ID))
                .thenReturn(Optional.empty());
        stubSavedResponse();

        Instant beforeCall = Instant.now();
        CourseMemberResponse result = memberService.addMember(COURSE_ID, request, OWNER_ID);
        Instant afterCall = Instant.now();

        assertThat(result.status()).isEqualTo(CourseMemberStatus.ACTIVE);
        assertThat(result.role()).isEqualTo(CourseMemberRole.STUDENT);
        assertThat(result.invitedBy()).isEqualTo(OWNER_ID);
        assertThat(result.joinedAt()).isBetween(beforeCall, afterCall);
    }

    @Test
    void addMember_reactivatesRejectedOrRemovedMembershipByInvitation() {
        Course inviteOnlyCourse = publishedCourse(CourseVisibility.INVITE_ONLY);
        CourseMember rejected = member(CourseMemberRole.STUDENT, CourseMemberStatus.REJECTED);
        rejected.setCourse(inviteOnlyCourse);
        when(courseUtils.requireCourse(COURSE_ID)).thenReturn(inviteOnlyCourse);
        when(memberRepository.findByCourseIdAndUserId(COURSE_ID, STUDENT_ID))
                .thenReturn(Optional.of(rejected));
        when(memberRepository.save(rejected)).thenReturn(rejected);
        when(memberMapper.toResponse(rejected)).thenAnswer(invocation -> memberResponse(rejected));

        CourseMemberResponse result = memberService.addMember(
                COURSE_ID,
                new CourseMemberCreateRequest(STUDENT_ID),
                OWNER_ID
        );

        assertThat(result.status()).isEqualTo(CourseMemberStatus.ACTIVE);
        assertThat(result.invitedBy()).isEqualTo(OWNER_ID);
    }

    @Test
    void addMember_rejectsDraftAndPublicCourses() {
        Course draftCourse = course();
        when(courseUtils.requireCourse(COURSE_ID)).thenReturn(draftCourse);

        assertError(
                () -> memberService.addMember(
                        COURSE_ID,
                        new CourseMemberCreateRequest(STUDENT_ID),
                        OWNER_ID
                ),
                CommonErrorCode.DATA_CONFLICT
        );

        Course publicCourse = publishedCourse(CourseVisibility.PUBLIC);
        when(courseUtils.requireCourse(COURSE_ID)).thenReturn(publicCourse);

        assertError(
                () -> memberService.addMember(
                        COURSE_ID,
                        new CourseMemberCreateRequest(STUDENT_ID),
                        OWNER_ID
                ),
                CommonErrorCode.DATA_CONFLICT
        );

        verify(memberRepository, never()).save(any());
    }

    @Test
    void requestToJoin_createsPendingRequestForPublishedPublicCourse() {
        Course publicCourse = publishedCourse(CourseVisibility.PUBLIC);
        when(courseUtils.requireCourse(COURSE_ID)).thenReturn(publicCourse);
        when(memberRepository.findByCourseIdAndUserId(COURSE_ID, STUDENT_ID))
                .thenReturn(Optional.empty());
        stubSavedResponse();

        CourseMemberResponse result = memberService.requestToJoin(COURSE_ID, STUDENT_ID);

        assertThat(result.userId()).isEqualTo(STUDENT_ID);
        assertThat(result.status()).isEqualTo(CourseMemberStatus.PENDING);
        assertThat(result.joinedAt()).isNull();
        assertThat(result.invitedBy()).isNull();
    }

    @Test
    void requestToJoin_reopensRejectedRequest() {
        Course publicCourse = publishedCourse(CourseVisibility.PUBLIC);
        CourseMember rejected = member(CourseMemberRole.STUDENT, CourseMemberStatus.REJECTED);
        rejected.setCourse(publicCourse);
        rejected.setInvitedBy(OWNER_ID);
        when(courseUtils.requireCourse(COURSE_ID)).thenReturn(publicCourse);
        when(memberRepository.findByCourseIdAndUserId(COURSE_ID, STUDENT_ID))
                .thenReturn(Optional.of(rejected));
        when(memberRepository.save(rejected)).thenReturn(rejected);
        when(memberMapper.toResponse(rejected)).thenAnswer(invocation -> memberResponse(rejected));

        CourseMemberResponse result = memberService.requestToJoin(COURSE_ID, STUDENT_ID);

        assertThat(result.status()).isEqualTo(CourseMemberStatus.PENDING);
        assertThat(result.invitedBy()).isNull();
        assertThat(result.joinedAt()).isNull();
    }

    @Test
    void requestToJoin_rejectsDraftInviteOnlyActiveAndDuplicatePendingCases() {
        Course draftCourse = course();
        when(courseUtils.requireCourse(COURSE_ID)).thenReturn(draftCourse);
        assertError(
                () -> memberService.requestToJoin(COURSE_ID, STUDENT_ID),
                CommonErrorCode.DATA_CONFLICT
        );

        Course inviteOnlyCourse = publishedCourse(CourseVisibility.INVITE_ONLY);
        when(courseUtils.requireCourse(COURSE_ID)).thenReturn(inviteOnlyCourse);
        assertError(
                () -> memberService.requestToJoin(COURSE_ID, STUDENT_ID),
                CommonErrorCode.DATA_CONFLICT
        );

        Course publicCourse = publishedCourse(CourseVisibility.PUBLIC);
        when(courseUtils.requireCourse(COURSE_ID)).thenReturn(publicCourse);
        when(memberRepository.findByCourseIdAndUserId(COURSE_ID, STUDENT_ID))
                .thenReturn(Optional.of(member(CourseMemberRole.STUDENT, CourseMemberStatus.ACTIVE)))
                .thenReturn(Optional.of(member(CourseMemberRole.STUDENT, CourseMemberStatus.PENDING)));
        assertError(
                () -> memberService.requestToJoin(COURSE_ID, STUDENT_ID),
                CommonErrorCode.DATA_CONFLICT
        );
        assertError(
                () -> memberService.requestToJoin(COURSE_ID, STUDENT_ID),
                CommonErrorCode.DATA_CONFLICT
        );

        verify(memberRepository, never()).save(any());
    }

    @Test
    void getJoinRequests_returnsOnlyPendingRequests() {
        CourseMember pending = member(CourseMemberRole.STUDENT, CourseMemberStatus.PENDING);
        CourseMemberResponse response = memberResponse(pending);
        when(memberRepository.findAllByCourseIdAndStatus(COURSE_ID, CourseMemberStatus.PENDING))
                .thenReturn(List.of(pending));
        when(memberMapper.toResponse(pending)).thenReturn(response);

        assertThat(memberService.getJoinRequests(COURSE_ID, OWNER_ID))
                .containsExactly(response);

        verify(courseAccessPolicy).requireOwner(COURSE_ID, OWNER_ID);
    }

    @Test
    void approveJoinRequest_activatesPendingStudentForPublishedPublicCourse() {
        Course publicCourse = publishedCourse(CourseVisibility.PUBLIC);
        CourseMember pending = member(CourseMemberRole.STUDENT, CourseMemberStatus.PENDING);
        pending.setCourse(publicCourse);
        pending.setJoinedAt(null);
        when(courseUtils.requireCourse(COURSE_ID)).thenReturn(publicCourse);
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(pending));
        when(memberMapper.toResponse(pending)).thenAnswer(invocation -> memberResponse(pending));

        Instant beforeCall = Instant.now();
        CourseMemberResponse result = memberService.approveJoinRequest(
                COURSE_ID,
                MEMBER_ID,
                OWNER_ID
        );
        Instant afterCall = Instant.now();

        assertThat(result.status()).isEqualTo(CourseMemberStatus.ACTIVE);
        assertThat(result.joinedAt()).isBetween(beforeCall, afterCall);
    }

    @Test
    void approveJoinRequest_rejectsDraftCourse() {
        when(courseUtils.requireCourse(COURSE_ID)).thenReturn(course());

        assertError(
                () -> memberService.approveJoinRequest(COURSE_ID, MEMBER_ID, OWNER_ID),
                CommonErrorCode.DATA_CONFLICT
        );

        verify(memberRepository, never()).findById(any());
    }

    @Test
    void rejectJoinRequest_marksPendingRequestRejected() {
        CourseMember pending = member(CourseMemberRole.STUDENT, CourseMemberStatus.PENDING);
        pending.setJoinedAt(null);
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(pending));
        when(memberMapper.toResponse(pending)).thenAnswer(invocation -> memberResponse(pending));

        CourseMemberResponse result = memberService.rejectJoinRequest(
                COURSE_ID,
                MEMBER_ID,
                OWNER_ID
        );

        assertThat(result.status()).isEqualTo(CourseMemberStatus.REJECTED);
        assertThat(result.joinedAt()).isNull();
    }

    @Test
    void removeMember_marksNonOwnerAsRemoved() {
        CourseMember active = member(CourseMemberRole.STUDENT, CourseMemberStatus.ACTIVE);
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(active));

        Instant beforeCall = Instant.now();
        memberService.removeMember(COURSE_ID, MEMBER_ID, OWNER_ID);
        Instant afterCall = Instant.now();

        assertThat(active.getStatus()).isEqualTo(CourseMemberStatus.REMOVED);
        assertThat(active.getRemovedAt()).isBetween(beforeCall, afterCall);
    }

    private Course publishedCourse(CourseVisibility visibility) {
        Course value = course();
        value.setVisibility(visibility);
        value.setStatus(CourseStatus.PUBLISHED);
        value.setPublishedAt(Instant.now());
        return value;
    }

    private void stubSavedResponse() {
        when(memberRepository.save(any(CourseMember.class))).thenAnswer(invocation -> {
            CourseMember saved = invocation.getArgument(0);
            saved.setId(MEMBER_ID);
            return saved;
        });
        when(memberMapper.toResponse(any(CourseMember.class)))
                .thenAnswer(invocation -> memberResponse(invocation.getArgument(0)));
    }

    private static void assertError(Runnable invocation, CommonErrorCode expectedCode) {
        assertThatThrownBy(invocation::run)
                .isInstanceOf(ApplicationException.class)
                .extracting(exception -> ((ApplicationException) exception).getErrorCode())
                .isEqualTo(expectedCode);
    }
}
