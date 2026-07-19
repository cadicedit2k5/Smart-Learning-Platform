package com.smartlearning.system.auth.mapper;

import com.smartlearning.common.mapper.CrudMapper;
import com.smartlearning.common.mapper.DefaultMapperConfig;
import com.smartlearning.system.auth.dto.request.UserCreateRequest;
import com.smartlearning.system.auth.dto.request.UserUpdateRequest;
import com.smartlearning.system.auth.dto.response.UserResponse;
import com.smartlearning.system.auth.entity.User;
import org.mapstruct.*;

@Mapper(config = DefaultMapperConfig.class)
public interface UserMapper extends CrudMapper<User, UserCreateRequest, UserUpdateRequest, UserResponse> {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "avatar", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    User toEntity(UserCreateRequest request);

    @Override
    @BeanMapping(
            nullValuePropertyMappingStrategy =
                    NullValuePropertyMappingStrategy.IGNORE
    )
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "avatar", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void partialUpdate(
            UserUpdateRequest request,
            @MappingTarget User entity
    );
}
