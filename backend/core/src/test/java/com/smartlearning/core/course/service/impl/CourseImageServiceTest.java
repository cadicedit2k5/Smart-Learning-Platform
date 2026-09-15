package com.smartlearning.core.course.service.impl;

import com.smartlearning.core.course.dto.request.CourseUpdateRequest;
import com.smartlearning.core.course.dto.response.CourseResponse;
import com.smartlearning.core.course.entity.Course;
import com.smartlearning.core.course.entity.CourseMember;
import com.smartlearning.core.course.entity.enums.CourseMemberRole;
import com.smartlearning.core.course.entity.enums.CourseMemberStatus;
import com.smartlearning.core.course.mapper.CourseMapper;
import com.smartlearning.core.course.repository.CourseMemberRepository;
import com.smartlearning.core.course.repository.CourseRepository;
import com.smartlearning.core.course.sercurity.CourseAccessPolicy;
import com.smartlearning.core.course.utils.CourseUtils;
import com.smartlearning.storage.dto.FileUploadResponse;
import com.smartlearning.storage.service.FileStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import static com.smartlearning.core.support.CoreTestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseImageServiceTest {

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

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private CourseServiceImpl courseService;

    @Test
    void updateCourse_replacesExistingImage() {
        Course existing = course();
        existing.setCoverUrl("old-image");

        CourseMember owner = new CourseMember();
        owner.setCourse(existing);
        owner.setUserId(OWNER_ID);
        owner.setRole(CourseMemberRole.OWNER);
        owner.setStatus(CourseMemberStatus.ACTIVE);

        MockMultipartFile image = new MockMultipartFile(
                "image",
                "course.png",
                "image/png",
                new byte[]{1, 2, 3}
        );

        CourseUpdateRequest request = new CourseUpdateRequest(null, null, null, null, image);

        FileUploadResponse uploaded = new FileUploadResponse(
                "new-image",
                "course.png",
                "image/png",
                3
        );

        CourseResponse response = courseResponse(existing);

        when(courseUtils.requireCourse(COURSE_ID)).thenReturn(existing);
        when(courseAccessPolicy.requireOwner(COURSE_ID, OWNER_ID)).thenReturn(owner);
        when(fileStorageService.upload(image, "core/courses/" + COURSE_ID + "/image")).thenReturn(uploaded);
        when(courseMapper.toResponse(existing)).thenReturn(response);
        when(courseUtils.withRole(response, CourseMemberRole.OWNER)).thenReturn(response);

        courseService.updateCourse(COURSE_ID, request, OWNER_ID);

        assertThat(existing.getCoverUrl()).isEqualTo("new-image");
        verify(fileStorageService).delete("old-image");
    }
}