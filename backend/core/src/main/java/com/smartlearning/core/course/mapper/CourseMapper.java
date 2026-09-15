package com.smartlearning.core.course.mapper;

import com.smartlearning.common.mapper.CrudMapper;
import com.smartlearning.common.mapper.DefaultMapperConfig;
import com.smartlearning.core.course.dto.request.CourseCreateRequest;
import com.smartlearning.core.course.dto.request.CourseUpdateRequest;
import com.smartlearning.core.course.dto.response.CourseResponse;
import com.smartlearning.core.course.entity.Course;
import org.mapstruct.*;

@Mapper(config = DefaultMapperConfig.class)
public interface CourseMapper extends CrudMapper<Course, CourseCreateRequest, CourseUpdateRequest, CourseResponse> {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "coverUrl", ignore = true)
    @Mapping(target = "publishedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "members", ignore = true)
    Course toEntity(CourseCreateRequest request);

    @Override
    @Mapping(target = "currentUserRole", ignore = true)
    @Mapping(target = "imageUrl", expression = "java(toImageUrl(course))")
    CourseResponse toResponse(Course course);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "coverUrl", ignore = true)
    @Mapping(target = "publishedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "members", ignore = true)
    void partialUpdate(CourseUpdateRequest request, @MappingTarget Course course);

    default String toImageUrl(Course course) {
        if (course == null || course.getId() == null || course.getCoverUrl() == null
                || course.getCoverUrl().isBlank()) {
            return null;
        }

        String version = Integer.toUnsignedString(course.getCoverUrl().hashCode());
        return "/courses/" + course.getId() + "/image?v=" + version;
    }
}