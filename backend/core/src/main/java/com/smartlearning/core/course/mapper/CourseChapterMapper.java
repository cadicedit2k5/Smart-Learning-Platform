package com.smartlearning.core.course.mapper;

import com.smartlearning.common.mapper.DefaultMapperConfig;
import com.smartlearning.core.course.dto.request.CourseChapterCreateRequest;
import com.smartlearning.core.course.dto.request.CourseChapterUpdateRequest;
import com.smartlearning.core.course.dto.response.CourseChapterResponse;
import com.smartlearning.core.course.entity.CourseChapter;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(config = DefaultMapperConfig.class)
public interface CourseChapterMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "course", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    CourseChapter toEntity(CourseChapterCreateRequest request);

    @Mapping(target = "courseId", source = "course.id")
    CourseChapterResponse toResponse(CourseChapter chapter);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "course", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void partialUpdate(CourseChapterUpdateRequest request, @MappingTarget CourseChapter chapter);
}
