package com.smartlearning.core.course.mapper;

import com.smartlearning.common.mapper.DefaultMapperConfig;
import com.smartlearning.common.mapper.ResponseMapper;
import com.smartlearning.core.course.dto.response.CourseMemberResponse;
import com.smartlearning.core.course.entity.CourseMember;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = DefaultMapperConfig.class)
public interface CourseMemberMapper extends ResponseMapper<CourseMember, CourseMemberResponse> {
    @Mapping(target = "courseId", source = "course.id")
    CourseMemberResponse toResponse(CourseMember entity);
}
