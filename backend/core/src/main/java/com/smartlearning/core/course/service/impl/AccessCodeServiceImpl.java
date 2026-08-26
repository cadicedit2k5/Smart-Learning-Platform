package com.smartlearning.core.course.service.impl;

import com.smartlearning.common.error.ApplicationException;
import com.smartlearning.common.error.CommonErrorCode;
import com.smartlearning.core.course.dto.request.AccessCodeCreateRequest;
import com.smartlearning.core.course.dto.response.AccessCodeCreatedResponse;
import com.smartlearning.core.course.dto.response.AccessCodeResponse;
import com.smartlearning.core.course.entity.AccessCode;
import com.smartlearning.core.course.entity.Course;
import com.smartlearning.core.course.repository.AccessCodeRepository;
import com.smartlearning.core.course.sercurity.CourseAccessPolicy;
import com.smartlearning.core.course.service.AccessCodeService;
import com.smartlearning.core.course.utils.AccessCodeUtils;
import com.smartlearning.core.course.utils.CourseUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class AccessCodeServiceImpl implements AccessCodeService {

    private final CourseUtils courseUtils;
    private final AccessCodeUtils accessCodeUtils;
    private final PasswordEncoder passwordEncoder;
    private final AccessCodeRepository accessCodeRepository;
    private final CourseAccessPolicy courseAccessPolicy;

    @Override
    public AccessCodeCreatedResponse createCode(UUID courseId, AccessCodeCreateRequest request, UUID currentUserId) {
        Course course = courseUtils.requireCourse(courseId);
        courseAccessPolicy.requireOwner(courseId, currentUserId);
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
//        accessCode.setMaxUses(request.maxUses());
        accessCode.setExpiresAt(request.expiresAt());
        accessCode.setCreatedBy(currentUserId);

        AccessCode saved =
                accessCodeRepository.save(accessCode);

        return new AccessCodeCreatedResponse(
                saved.getId(),
                courseId,
                rawCode,
//                saved.getMaxUses(),
//                saved.getUsedCount(),
                saved.getExpiresAt(),
                saved.getActive(),
                saved.getCreatedAt()
        );
    }

    @Override
    public List<AccessCodeResponse> getCodes(UUID courseId, UUID currentUserId) {
        courseUtils.requireCourse(courseId);
        courseAccessPolicy.requireOwner(courseId, currentUserId);

        return accessCodeRepository.findAllByCourseIdOrderByCreatedAtDesc(courseId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public void revokeCode(
            UUID courseId,
            UUID codeId,
            UUID currentUserId
    ) {
        courseUtils.requireCourse(courseId);
        courseAccessPolicy.requireOwner(courseId, currentUserId);

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

    private AccessCodeResponse toResponse(AccessCode accessCode) {
        return new AccessCodeResponse(
                accessCode.getId(),
                accessCode.getCourse().getId(),
                accessCode.getCodeHint(),
                accessCode.getExpiresAt(),
                accessCode.getActive(),
                accessCode.getRevokedAt(),
                accessCode.getCreatedBy(),
                accessCode.getCreatedAt()
        );
    }
}
