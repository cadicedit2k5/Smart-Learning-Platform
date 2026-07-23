package com.smartlearning.core.course.service.impl;

import com.smartlearning.common.error.ApplicationException;
import com.smartlearning.common.error.CommonErrorCode;
import com.smartlearning.core.course.dto.request.AccessCodeCreateRequest;
import com.smartlearning.core.course.dto.response.AccessCodeCreatedResponse;
import com.smartlearning.core.course.entity.AccessCode;
import com.smartlearning.core.course.entity.Course;
import com.smartlearning.core.course.repository.AccessCodeRepository;
import com.smartlearning.core.course.service.AccessCodeService;
import com.smartlearning.core.course.utils.AccessCodeUtils;
import com.smartlearning.core.course.utils.CourseUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class AccessCodeServiceImpl implements AccessCodeService {

    private final CourseUtils courseUtils;
    private final AccessCodeUtils accessCodeUtils;
    private final PasswordEncoder passwordEncoder;
    private final AccessCodeRepository accessCodeRepository;

    @Override
    public AccessCodeCreatedResponse createCode(UUID courseId, AccessCodeCreateRequest request, UUID currentUserId) {
//        permissionService.requireTeachingMember(
//                courseId,
//                currentUserId
//        );

        Course course = courseUtils.requireCourse(courseId);
        String rawCode = accessCodeUtils.generateRawCode();

        AccessCode accessCode =
                new AccessCode();

        accessCode.setCourse(course);
        accessCode.setCodeHash(
                passwordEncoder.encode(rawCode)
        );
        accessCode.setCodeHint(
                rawCode.substring(rawCode.length() - 4)
        );
        accessCode.setMaxUses(request.maxUses());
        accessCode.setExpiresAt(request.expiresAt());
        accessCode.setCreatedBy(currentUserId);

        AccessCode saved =
                accessCodeRepository.save(accessCode);

        return new AccessCodeCreatedResponse(
                saved.getId(),
                courseId,
                rawCode,
                saved.getMaxUses(),
                saved.getUsedCount(),
                saved.getExpiresAt(),
                saved.getActive(),
                saved.getCreatedAt()
        );
    }

    public void revokeCode(
            UUID courseId,
            UUID codeId,
            UUID currentUserId
    ) {
//        permissionService.requireTeachingMember(
//                courseId,
//                currentUserId
//        );

        AccessCode accessCode =
                accessCodeRepository.findById(codeId)
                        .orElseThrow(() -> new ApplicationException(
                                CommonErrorCode.DATA_CONFLICT,
                                "AccessCode không hợp lệ!"
                        ));

        if (!accessCode.getCourse().getId().equals(courseId)) {
            throw new ApplicationException(
                    CommonErrorCode.DATA_CONFLICT,
                    "AccessCode không hợp lệ!"
            );
        }

        accessCode.setActive(false);
        accessCode.setRevokedAt(Instant.now());
    }
}
