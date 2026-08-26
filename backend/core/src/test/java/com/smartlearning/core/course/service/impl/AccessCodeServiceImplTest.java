package com.smartlearning.core.course.service.impl;

import com.smartlearning.common.error.ApplicationException;
import com.smartlearning.common.error.CommonErrorCode;
import com.smartlearning.core.course.dto.request.AccessCodeCreateRequest;
import com.smartlearning.core.course.dto.response.AccessCodeCreatedResponse;
import com.smartlearning.core.course.entity.AccessCode;
import com.smartlearning.core.course.entity.Course;
import com.smartlearning.core.course.repository.AccessCodeRepository;
import com.smartlearning.core.course.sercurity.CourseAccessPolicy;
import com.smartlearning.core.course.utils.AccessCodeUtils;
import com.smartlearning.core.course.utils.CourseUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.List;
import java.util.UUID;

import static com.smartlearning.core.support.CoreTestData.ACCESS_CODE_ID;
import static com.smartlearning.core.support.CoreTestData.COURSE_ID;
import static com.smartlearning.core.support.CoreTestData.OWNER_ID;
import static com.smartlearning.core.support.CoreTestData.TEST_TIME;
import static com.smartlearning.core.support.CoreTestData.accessCode;
import static com.smartlearning.core.support.CoreTestData.course;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessCodeServiceImplTest {

    @Mock
    private CourseUtils courseUtils;
    @Mock
    private AccessCodeUtils accessCodeUtils;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AccessCodeRepository accessCodeRepository;
    @Mock
    private CourseAccessPolicy courseAccessPolicy;
    @InjectMocks
    private AccessCodeServiceImpl accessCodeService;

    @Test
    void createCode_generatesHashesAndPersistsCodeButReturnsRawValue() {
        Instant expiresAt = Instant.now().plusSeconds(3600);
        AccessCodeCreateRequest request = new AccessCodeCreateRequest(expiresAt);
        Course course = course();
        String rawCode = "COURSE-ABCD1234";
        when(courseUtils.requireCourse(COURSE_ID)).thenReturn(course);
        when(accessCodeUtils.generateRawCode()).thenReturn(rawCode);
        when(passwordEncoder.encode(rawCode)).thenReturn("encoded-value");
        when(accessCodeRepository.save(any(AccessCode.class))).thenAnswer(invocation -> {
            AccessCode saved = invocation.getArgument(0);
            saved.setId(ACCESS_CODE_ID);
            saved.setCreatedAt(TEST_TIME);
            return saved;
        });

        AccessCodeCreatedResponse result = accessCodeService.createCode(COURSE_ID, request, OWNER_ID);

        assertThat(result.id()).isEqualTo(ACCESS_CODE_ID);
        assertThat(result.courseId()).isEqualTo(COURSE_ID);
        assertThat(result.code()).isEqualTo(rawCode);
        assertThat(result.expiresAt()).isEqualTo(expiresAt);
        assertThat(result.active()).isTrue();
        assertThat(result.createdAt()).isEqualTo(TEST_TIME);

        ArgumentCaptor<AccessCode> captor = ArgumentCaptor.forClass(AccessCode.class);
        verify(accessCodeRepository).save(captor.capture());
        AccessCode persisted = captor.getValue();
        assertThat(persisted.getCourse()).isSameAs(course);
        assertThat(persisted.getCodeHash()).isEqualTo("encoded-value");
        assertThat(persisted.getCodeHash()).doesNotContain(rawCode);
        assertThat(persisted.getCodeHint()).isEqualTo("1234");
        assertThat(persisted.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(persisted.getCreatedBy()).isEqualTo(OWNER_ID);
    }

    @Test
    void revokeCode_marksMatchingCodeInactive() {
        AccessCode code = accessCode();
        when(accessCodeRepository.findById(ACCESS_CODE_ID)).thenReturn(Optional.of(code));

        Instant beforeCall = Instant.now();
        accessCodeService.revokeCode(COURSE_ID, ACCESS_CODE_ID, OWNER_ID);
        Instant afterCall = Instant.now();

        assertThat(code.getActive()).isFalse();
        assertThat(code.getRevokedAt()).isBetween(beforeCall, afterCall);
        verify(accessCodeRepository, never()).save(any());
    }

    @Test
    void revokeCode_rejectsMissingCode() {
        when(accessCodeRepository.findById(ACCESS_CODE_ID)).thenReturn(Optional.empty());

        assertConflict(() -> accessCodeService.revokeCode(COURSE_ID, ACCESS_CODE_ID, OWNER_ID));

        verify(courseUtils).requireCourse(COURSE_ID);
        verify(courseAccessPolicy).requireOwner(COURSE_ID, OWNER_ID);
        verifyNoInteractions(passwordEncoder, accessCodeUtils);
    }

    @Test
    void revokeCode_rejectsCodeOwnedByAnotherCourse() {
        AccessCode code = accessCode();
        code.getCourse().setId(UUID.randomUUID());
        when(accessCodeRepository.findById(ACCESS_CODE_ID)).thenReturn(Optional.of(code));

        assertConflict(() -> accessCodeService.revokeCode(COURSE_ID, ACCESS_CODE_ID, OWNER_ID));

        assertThat(code.getActive()).isTrue();
        assertThat(code.getRevokedAt()).isNull();
    }

    @Test
    void getCodes_returnsOnlyHintsAfterTeachingAccessCheck() {
        AccessCode first = accessCode();
        AccessCode second = accessCode();
        second.setId(UUID.randomUUID());
        second.setCodeHint("5678");
        second.setActive(false);
        when(accessCodeRepository.findAllByCourseIdOrderByCreatedAtDesc(COURSE_ID))
                .thenReturn(List.of(first, second));

        var result = accessCodeService.getCodes(COURSE_ID, OWNER_ID);

        assertThat(result).extracting(value -> value.codeHint())
                .containsExactly("1234", "5678");
        assertThat(result).extracting(value -> value.active())
                .containsExactly(true, false);
        verify(courseUtils).requireCourse(COURSE_ID);
        verify(courseAccessPolicy).requireOwner(COURSE_ID, OWNER_ID);
    }

    private static void assertConflict(Runnable invocation) {
        assertThatThrownBy(invocation::run)
                .isInstanceOf(ApplicationException.class)
                .extracting(exception -> ((ApplicationException) exception).getErrorCode())
                .isEqualTo(CommonErrorCode.DATA_CONFLICT);
    }
}
