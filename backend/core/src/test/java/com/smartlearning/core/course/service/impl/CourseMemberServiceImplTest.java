package com.smartlearning.core.course.service.impl;

import com.smartlearning.common.error.ApplicationException;
import com.smartlearning.common.error.CommonErrorCode;
import com.smartlearning.core.course.dto.request.CourseMemberCreateRequest;
import com.smartlearning.core.course.dto.request.JoinCourseRequest;
import com.smartlearning.core.course.dto.response.CourseMemberResponse;
import com.smartlearning.core.course.entity.AccessCode;
import com.smartlearning.core.course.entity.Course;
import com.smartlearning.core.course.entity.CourseMember;
import com.smartlearning.core.course.entity.enums.CourseMemberRole;
import com.smartlearning.core.course.entity.enums.CourseMemberStatus;
import com.smartlearning.core.course.mapper.CourseMemberMapper;
import com.smartlearning.core.course.repository.AccessCodeRepository;
import com.smartlearning.core.course.repository.CourseMemberRepository;
import com.smartlearning.core.course.sercurity.CourseAccessPolicy;
import com.smartlearning.core.course.utils.CourseUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.smartlearning.core.support.CoreTestData.ACCESS_CODE_ID;
import static com.smartlearning.core.support.CoreTestData.COURSE_ID;
import static com.smartlearning.core.support.CoreTestData.MEMBER_ID;
import static com.smartlearning.core.support.CoreTestData.OWNER_ID;
import static com.smartlearning.core.support.CoreTestData.STUDENT_ID;
import static com.smartlearning.core.support.CoreTestData.accessCode;
import static com.smartlearning.core.support.CoreTestData.course;
import static com.smartlearning.core.support.CoreTestData.member;
import static com.smartlearning.core.support.CoreTestData.memberResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
    private AccessCodeRepository accessCodeRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private CourseAccessPolicy courseAccessPolicy;
    @InjectMocks
    private CourseMemberServiceImpl memberService;

    @Test
    void addMember_rejectsOwnerRoleAfterCheckingOwnerAccess() {
        CourseMemberCreateRequest request = new CourseMemberCreateRequest(STUDENT_ID, CourseMemberRole.OWNER);

        assertError(() -> memberService.addMember(COURSE_ID, request, OWNER_ID), CommonErrorCode.FORBIDDEN);

        verify(courseUtils).requireCourse(COURSE_ID);
        verify(courseAccessPolicy).requireOwner(COURSE_ID, OWNER_ID);
        verifyNoInteractions(memberRepository, memberMapper);
    }

    @Test
    void addMember_rejectsExistingNonRemovedMember() {
        CourseMemberCreateRequest request = new CourseMemberCreateRequest(STUDENT_ID, CourseMemberRole.STUDENT);
        CourseMember existing = member(CourseMemberRole.STUDENT, CourseMemberStatus.ACTIVE);
        when(courseUtils.requireCourse(COURSE_ID)).thenReturn(course());
        when(memberRepository.findByCourseIdAndUserId(COURSE_ID, STUDENT_ID))
                .thenReturn(Optional.of(existing));

        assertError(() -> memberService.addMember(COURSE_ID, request, OWNER_ID), CommonErrorCode.DATA_CONFLICT);

        verify(memberRepository, never()).save(any());
        verifyNoInteractions(memberMapper);
    }

    @Test
    void addMember_respectsRequestedLecturerRole() {
        CourseMemberCreateRequest request = new CourseMemberCreateRequest(STUDENT_ID, CourseMemberRole.LECTURER);
        Course course = course();
        when(courseUtils.requireCourse(COURSE_ID)).thenReturn(course);
        when(memberRepository.findByCourseIdAndUserId(COURSE_ID, STUDENT_ID)).thenReturn(Optional.empty());
        when(memberRepository.save(any(CourseMember.class))).thenAnswer(invocation -> {
            CourseMember saved = invocation.getArgument(0);
            saved.setId(MEMBER_ID);
            return saved;
        });
        when(memberMapper.toResponse(any(CourseMember.class)))
                .thenAnswer(invocation -> memberResponse(invocation.getArgument(0)));

        Instant beforeCall = Instant.now();
        CourseMemberResponse result = memberService.addMember(COURSE_ID, request, OWNER_ID);
        Instant afterCall = Instant.now();

        assertThat(result.userId()).isEqualTo(STUDENT_ID);
        assertThat(result.role()).isEqualTo(CourseMemberRole.LECTURER);
        assertThat(result.status()).isEqualTo(CourseMemberStatus.ACTIVE);
        assertThat(result.joinedAt()).isBetween(beforeCall, afterCall);
        assertThat(result.courseId()).isEqualTo(COURSE_ID);
        assertThat(result.invitedBy()).isEqualTo(OWNER_ID);
    }

    @Test
    void addMember_reactivatesRemovedMembership() {
        CourseMemberCreateRequest request = new CourseMemberCreateRequest(STUDENT_ID, CourseMemberRole.STUDENT);
        CourseMember removed = member(CourseMemberRole.LECTURER, CourseMemberStatus.REMOVED);
        removed.setRemovedAt(Instant.parse("2026-01-01T00:00:00Z"));
        removed.setInvitedBy(OWNER_ID);
        when(courseUtils.requireCourse(COURSE_ID)).thenReturn(removed.getCourse());
        when(memberRepository.findByCourseIdAndUserId(COURSE_ID, STUDENT_ID)).thenReturn(Optional.of(removed));
        when(memberRepository.save(removed)).thenReturn(removed);
        when(memberMapper.toResponse(removed)).thenReturn(memberResponse(removed));

        memberService.addMember(COURSE_ID, request, OWNER_ID);

        assertThat(removed.getStatus()).isEqualTo(CourseMemberStatus.ACTIVE);
        assertThat(removed.getRole()).isEqualTo(CourseMemberRole.STUDENT);
        assertThat(removed.getRemovedAt()).isNull();
        assertThat(removed.getInvitedBy()).isEqualTo(OWNER_ID);
    }

    @Test
    void getMembers_checksAccessBeforeQueryAndMapsActiveMembers() {
        CourseMember first = member(CourseMemberRole.OWNER, CourseMemberStatus.ACTIVE);
        CourseMember second = member(CourseMemberRole.STUDENT, CourseMemberStatus.ACTIVE);
        second.setId(UUID.randomUUID());
        CourseMemberResponse firstResponse = memberResponse(first);
        CourseMemberResponse secondResponse = memberResponse(second);
        when(memberRepository.findAllByCourseIdAndStatus(COURSE_ID, CourseMemberStatus.ACTIVE))
                .thenReturn(List.of(first, second));
        when(memberMapper.toResponse(first)).thenReturn(firstResponse);
        when(memberMapper.toResponse(second)).thenReturn(secondResponse);

        assertThat(memberService.getMembers(COURSE_ID, OWNER_ID))
                .containsExactly(firstResponse, secondResponse);

        InOrder order = inOrder(courseUtils, courseAccessPolicy, memberRepository);
        order.verify(courseUtils).requireCourse(COURSE_ID);
        order.verify(courseAccessPolicy).requireTeachingMember(COURSE_ID, OWNER_ID);
        order.verify(memberRepository).findAllByCourseIdAndStatus(COURSE_ID, CourseMemberStatus.ACTIVE);
    }

    @Test
    void removeMember_marksNonOwnerAsRemoved() {
        CourseMember existing = member(CourseMemberRole.STUDENT, CourseMemberStatus.ACTIVE);
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(existing));

        Instant beforeCall = Instant.now();
        memberService.removeMember(COURSE_ID, MEMBER_ID, OWNER_ID);
        Instant afterCall = Instant.now();

        assertThat(existing.getStatus()).isEqualTo(CourseMemberStatus.REMOVED);
        assertThat(existing.getRemovedAt()).isBetween(beforeCall, afterCall);
        verify(memberRepository, never()).save(any());
    }

    @Test
    void removeMember_returnsNotFoundForMissingOrDifferentCourseMember() {
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.empty());
        assertError(
                () -> memberService.removeMember(COURSE_ID, MEMBER_ID, OWNER_ID),
                CommonErrorCode.RESOURCE_NOT_FOUND
        );

        CourseMember memberFromOtherCourse = member(CourseMemberRole.STUDENT, CourseMemberStatus.ACTIVE);
        memberFromOtherCourse.getCourse().setId(UUID.randomUUID());
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(memberFromOtherCourse));
        assertError(
                () -> memberService.removeMember(COURSE_ID, MEMBER_ID, OWNER_ID),
                CommonErrorCode.RESOURCE_NOT_FOUND
        );
    }

    @Test
    void removeMember_rejectsOwner() {
        CourseMember owner = member(CourseMemberRole.OWNER, CourseMemberStatus.ACTIVE);
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(owner));

        assertError(
                () -> memberService.removeMember(COURSE_ID, MEMBER_ID, OWNER_ID),
                CommonErrorCode.FORBIDDEN
        );

        assertThat(owner.getStatus()).isEqualTo(CourseMemberStatus.ACTIVE);
    }

    @Test
    void joinByCode_returnsExistingActiveMembershipWithoutCheckingCode() {
        JoinCourseRequest request = new JoinCourseRequest(COURSE_ID, "ignored-code");
        CourseMember activeMember = member(CourseMemberRole.STUDENT, CourseMemberStatus.ACTIVE);
        CourseMemberResponse expected = memberResponse(activeMember);
        when(courseUtils.requireCourse(COURSE_ID)).thenReturn(activeMember.getCourse());
        when(memberRepository.findByCourseIdAndUserId(COURSE_ID, STUDENT_ID))
                .thenReturn(Optional.of(activeMember));
        when(memberMapper.toResponse(activeMember)).thenReturn(expected);

        assertThat(memberService.joinByCode(request, STUDENT_ID)).isSameAs(expected);

        verifyNoInteractions(accessCodeRepository, passwordEncoder);
        verify(memberRepository, never()).save(any());
    }

    @Test
    void joinByCode_rejectsCodeThatDoesNotMatchAnyActiveHash() {
        JoinCourseRequest request = new JoinCourseRequest(COURSE_ID, "wrong-code");
        AccessCode first = accessCode();
        AccessCode second = accessCode();
        second.setId(UUID.randomUUID());
        second.setCodeHash("other-hash");
        when(courseUtils.requireCourse(COURSE_ID)).thenReturn(course());
        when(memberRepository.findByCourseIdAndUserId(COURSE_ID, STUDENT_ID)).thenReturn(Optional.empty());
        when(accessCodeRepository.findAllByCourseIdAndActiveTrue(COURSE_ID)).thenReturn(List.of(first, second));
        when(passwordEncoder.matches("wrong-code", first.getCodeHash())).thenReturn(false);
        when(passwordEncoder.matches("wrong-code", second.getCodeHash())).thenReturn(false);

        assertError(() -> memberService.joinByCode(request, STUDENT_ID), CommonErrorCode.DATA_CONFLICT);

        verify(memberRepository, never()).save(any());
    }

    @Test
    void joinByCode_rejectsExpiredMatchingCode() {
        JoinCourseRequest request = new JoinCourseRequest(COURSE_ID, "COURSE-1234");
        AccessCode expired = accessCode();
        expired.setExpiresAt(Instant.now().minusSeconds(1));
        when(courseUtils.requireCourse(COURSE_ID)).thenReturn(course());
        when(memberRepository.findByCourseIdAndUserId(COURSE_ID, STUDENT_ID)).thenReturn(Optional.empty());
        when(accessCodeRepository.findAllByCourseIdAndActiveTrue(COURSE_ID)).thenReturn(List.of(expired));
        when(passwordEncoder.matches(request.code(), expired.getCodeHash())).thenReturn(true);

        assertError(() -> memberService.joinByCode(request, STUDENT_ID), CommonErrorCode.DATA_CONFLICT);

        verify(memberRepository, never()).save(any());
    }

    @Test
    void joinByCode_checksHashesInOrderAndCreatesStudentForValidCode() {
        JoinCourseRequest request = new JoinCourseRequest(COURSE_ID, "COURSE-1234");
        AccessCode nonMatching = accessCode();
        AccessCode matching = accessCode();
        nonMatching.setId(ACCESS_CODE_ID);
        matching.setId(UUID.randomUUID());
        matching.setCodeHash("matching-hash");
        matching.setExpiresAt(Instant.now().plusSeconds(600));
        Course course = course();
        when(courseUtils.requireCourse(COURSE_ID)).thenReturn(course);
        when(memberRepository.findByCourseIdAndUserId(COURSE_ID, STUDENT_ID)).thenReturn(Optional.empty());
        when(accessCodeRepository.findAllByCourseIdAndActiveTrue(COURSE_ID))
                .thenReturn(List.of(nonMatching, matching));
        when(passwordEncoder.matches(request.code(), nonMatching.getCodeHash())).thenReturn(false);
        when(passwordEncoder.matches(request.code(), matching.getCodeHash())).thenReturn(true);
        when(memberRepository.save(any(CourseMember.class))).thenAnswer(invocation -> {
            CourseMember saved = invocation.getArgument(0);
            saved.setId(MEMBER_ID);
            return saved;
        });
        when(memberMapper.toResponse(any(CourseMember.class)))
                .thenAnswer(invocation -> memberResponse(invocation.getArgument(0)));

        CourseMemberResponse result = memberService.joinByCode(request, STUDENT_ID);

        assertThat(result.courseId()).isEqualTo(COURSE_ID);
        assertThat(result.userId()).isEqualTo(STUDENT_ID);
        assertThat(result.role()).isEqualTo(CourseMemberRole.STUDENT);
        assertThat(result.status()).isEqualTo(CourseMemberStatus.ACTIVE);
        InOrder hashOrder = inOrder(passwordEncoder);
        hashOrder.verify(passwordEncoder).matches(request.code(), nonMatching.getCodeHash());
        hashOrder.verify(passwordEncoder).matches(request.code(), matching.getCodeHash());
    }

    private static void assertError(Runnable invocation, CommonErrorCode expectedCode) {
        assertThatThrownBy(invocation::run)
                .isInstanceOf(ApplicationException.class)
                .extracting(exception -> ((ApplicationException) exception).getErrorCode())
                .isEqualTo(expectedCode);
    }
}
