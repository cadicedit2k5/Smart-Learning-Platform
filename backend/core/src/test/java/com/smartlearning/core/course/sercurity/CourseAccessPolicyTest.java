package com.smartlearning.core.course.sercurity;

import com.smartlearning.common.error.ApplicationException;
import com.smartlearning.common.error.CommonErrorCode;
import com.smartlearning.core.course.entity.CourseMember;
import com.smartlearning.core.course.entity.enums.CourseMemberRole;
import com.smartlearning.core.course.entity.enums.CourseMemberStatus;
import com.smartlearning.core.course.repository.CourseMemberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.stream.Stream;

import static com.smartlearning.core.support.CoreTestData.COURSE_ID;
import static com.smartlearning.core.support.CoreTestData.STUDENT_ID;
import static com.smartlearning.core.support.CoreTestData.member;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseAccessPolicyTest {

    @Mock
    private CourseMemberRepository memberRepository;
    @Mock
    private CurrentUserAccess currentUserAccess;
    @InjectMocks
    private CourseAccessPolicy accessPolicy;

    @Test
    void requireActiveMember_returnsActiveMember() {
        CourseMember active = member(CourseMemberRole.STUDENT, CourseMemberStatus.ACTIVE);
        when(memberRepository.findByCourseIdAndUserId(COURSE_ID, STUDENT_ID)).thenReturn(Optional.of(active));

        assertThatCode(() -> accessPolicy.requireActiveMember(COURSE_ID, STUDENT_ID))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "rejects membership status {0}")
    @MethodSource("inactiveMembershipStatuses")
    void requireActiveMember_rejectsMissingAndInactiveMemberships(
            CourseMemberStatus status
    ) {
        Optional<CourseMember> membership = status == null
                ? Optional.empty()
                : Optional.of(member(CourseMemberRole.STUDENT, status));
        when(memberRepository.findByCourseIdAndUserId(COURSE_ID, STUDENT_ID))
                .thenReturn(membership);

        assertForbidden(() -> accessPolicy.requireActiveMember(COURSE_ID, STUDENT_ID));
    }

    static Stream<Arguments> inactiveMembershipStatuses() {
        return Stream.of(
                Arguments.of((Object) null),
                Arguments.of(CourseMemberStatus.PENDING),
                Arguments.of(CourseMemberStatus.REJECTED),
                Arguments.of(CourseMemberStatus.REMOVED)
        );
    }

    @Test
    void requireOwner_acceptsActiveOwner() {
        CourseMember owner = member(CourseMemberRole.OWNER, CourseMemberStatus.ACTIVE);
        when(memberRepository.findByCourseIdAndUserId(COURSE_ID, STUDENT_ID))
                .thenReturn(Optional.of(owner));

        assertThatCode(() -> accessPolicy.requireOwner(COURSE_ID, STUDENT_ID))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "rejects owner access for {0}/{1}")
    @MethodSource("nonOwnerMemberships")
    void requireOwner_rejectsWrongRoleOrStatus(
            CourseMemberRole role,
            CourseMemberStatus status
    ) {
        CourseMember student = member(role, status);
        when(memberRepository.findByCourseIdAndUserId(COURSE_ID, STUDENT_ID))
                .thenReturn(Optional.of(student));

        assertForbidden(() -> accessPolicy.requireOwner(COURSE_ID, STUDENT_ID));
    }

    static Stream<Arguments> nonOwnerMemberships() {
        return Stream.of(
                Arguments.of(CourseMemberRole.STUDENT, CourseMemberStatus.ACTIVE),
                Arguments.of(CourseMemberRole.OWNER, CourseMemberStatus.PENDING),
                Arguments.of(CourseMemberRole.OWNER, CourseMemberStatus.REMOVED)
        );
    }

    @Test
    void adminCanAccessAndManageAnyCourseWithoutMembership() {
        when(currentUserAccess.isAdmin()).thenReturn(true);

        assertThatCode(() -> accessPolicy.requireActiveMember(COURSE_ID, STUDENT_ID))
                .doesNotThrowAnyException();
        assertThatCode(() -> accessPolicy.requireOwner(COURSE_ID, STUDENT_ID))
                .doesNotThrowAnyException();

        verifyNoInteractions(memberRepository);
    }

    private static void assertForbidden(Runnable invocation) {
        assertThatThrownBy(invocation::run)
                .isInstanceOf(ApplicationException.class)
                .extracting(exception -> ((ApplicationException) exception).getErrorCode())
                .isEqualTo(CommonErrorCode.FORBIDDEN);
    }
}
