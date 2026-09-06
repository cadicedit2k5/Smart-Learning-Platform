package com.smartlearning.core.course.mapper;

import com.smartlearning.common.mapper.CrudMapper;
import com.smartlearning.common.mapper.DefaultMapperConfig;
import com.smartlearning.core.course.dto.request.CourseCreateRequest;
import com.smartlearning.core.course.dto.request.CourseUpdateRequest;
import com.smartlearning.core.course.dto.response.CourseResponse;
import com.smartlearning.core.course.entity.Course;
import org.mapstruct.*;

@Mapper(config = DefaultMapperConfig.class)
public interface CourseMapper extends CrudMapper<Course, CourseCreateRequest,
        CourseUpdateRequest, CourseResponse> {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "publishedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Course toEntity(CourseCreateRequest request);

    @Override
    @Mapping(target = "currentUserRole", ignore = true)
    CourseResponse toResponse(Course course);

    @BeanMapping(
            nullValuePropertyMappingStrategy =
                    NullValuePropertyMappingStrategy.IGNORE
    )
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "publishedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void partialUpdate(
            CourseUpdateRequest request,
            @MappingTarget Course course
    );
}
