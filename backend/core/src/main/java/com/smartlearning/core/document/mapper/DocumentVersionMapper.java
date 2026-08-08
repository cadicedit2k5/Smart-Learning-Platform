package com.smartlearning.core.document.mapper;

import com.smartlearning.common.mapper.DefaultMapperConfig;
import com.smartlearning.common.mapper.ResponseMapper;
import com.smartlearning.core.document.dto.response.DocumentVersionResponse;
import com.smartlearning.core.document.entity.DocumentVersion;
import org.mapstruct.Mapper;

@Mapper(config = DefaultMapperConfig.class)
public interface DocumentVersionMapper extends ResponseMapper<DocumentVersion, DocumentVersionResponse> {
}
