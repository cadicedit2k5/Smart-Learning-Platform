package com.smartlearning.core.course.sercurity;

import com.smartlearning.common.error.ApplicationException;
import com.smartlearning.common.error.CommonErrorCode;
import com.smartlearning.core.course.entity.CourseMember;
import com.smartlearning.core.course.entity.enums.CourseMemberRole;
import com.smartlearning.core.course.entity.enums.CourseMemberStatus;
import com.smartlearning.core.course.repository.CourseMemberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

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

    @Test
    void requireActiveMember_rejectsMissingAndInactiveMemberships() {
        when(memberRepository.findByCourseIdAndUserId(COURSE_ID, STUDENT_ID)).thenReturn(Optional.empty());
        assertForbidden(() -> accessPolicy.requireActiveMember(COURSE_ID, STUDENT_ID));

        CourseMember removed = member(CourseMemberRole.STUDENT, CourseMemberStatus.REMOVED);
        when(memberRepository.findByCourseIdAndUserId(COURSE_ID, STUDENT_ID)).thenReturn(Optional.of(removed));
        assertForbidden(() -> accessPolicy.requireActiveMember(COURSE_ID, STUDENT_ID));
    }

    @Test
    void requireTeachingMember_acceptsOwnerAndLecturer() {
        CourseMember owner = member(CourseMemberRole.OWNER, CourseMemberStatus.ACTIVE);
        when(memberRepository.findByCourseIdAndUserId(COURSE_ID, STUDENT_ID)).thenReturn(Optional.of(owner));
        assertThatCode(() -> accessPolicy.requireTeachingMember(COURSE_ID, STUDENT_ID))
                .doesNotThrowAnyException();

        CourseMember lecturer = member(CourseMemberRole.LECTURER, CourseMemberStatus.ACTIVE);
        when(memberRepository.findByCourseIdAndUserId(COURSE_ID, STUDENT_ID)).thenReturn(Optional.of(lecturer));
        assertThatCode(() -> accessPolicy.requireTeachingMember(COURSE_ID, STUDENT_ID))
                .doesNotThrowAnyException();
    }

    @Test
    void requireTeachingMember_rejectsActiveStudent() {
        CourseMember student = member(CourseMemberRole.STUDENT, CourseMemberStatus.ACTIVE);
        when(memberRepository.findByCourseIdAndUserId(COURSE_ID, STUDENT_ID)).thenReturn(Optional.of(student));

        assertForbidden(() -> accessPolicy.requireTeachingMember(COURSE_ID, STUDENT_ID));
    }

    @Test
    void adminCanAccessAndManageAnyCourseWithoutMembership() {
        when(currentUserAccess.isAdmin()).thenReturn(true);

        assertThatCode(() -> accessPolicy.requireActiveMember(COURSE_ID, STUDENT_ID))
                .doesNotThrowAnyException();
        assertThatCode(() -> accessPolicy.requireTeachingMember(COURSE_ID, STUDENT_ID))
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
