package com.smartlearning.core.course.mapper;

import com.smartlearning.common.mapper.DefaultMapperConfig;
import com.smartlearning.core.course.dto.request.CourseTopicCreateRequest;
import com.smartlearning.core.course.dto.request.CourseTopicUpdateRequest;
import com.smartlearning.core.course.dto.response.CourseTopicResponse;
import com.smartlearning.core.course.entity.CourseTopic;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(config = DefaultMapperConfig.class)
public interface CourseTopicMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "chapter", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    CourseTopic toEntity(CourseTopicCreateRequest request);

    @Mapping(target = "courseId", source = "chapter.course.id")
    @Mapping(target = "chapterId", source = "chapter.id")
    CourseTopicResponse toResponse(CourseTopic topic);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "chapter", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void partialUpdate(CourseTopicUpdateRequest request, @MappingTarget CourseTopic topic);
}
