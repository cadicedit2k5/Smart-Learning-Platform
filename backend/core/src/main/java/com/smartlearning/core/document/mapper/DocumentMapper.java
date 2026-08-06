package com.smartlearning.core.document.mapper;

import com.smartlearning.common.mapper.CreateMapper;
import com.smartlearning.common.mapper.DefaultMapperConfig;
import com.smartlearning.common.mapper.ResponseMapper;
import com.smartlearning.core.document.dto.request.DocumentCreateRequest;
import com.smartlearning.core.document.dto.response.DocumentResponse;
import com.smartlearning.core.document.entity.Document;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = DefaultMapperConfig.class)
public interface DocumentMapper extends CreateMapper<Document, DocumentCreateRequest>,
        ResponseMapper<Document, DocumentResponse> {

    @Override
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "course", ignore = true)
    @Mapping(target = "chapter", ignore = true)
    @Mapping(target = "topic", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "lifecycleStatus", ignore = true)
    @Mapping(target = "uploadedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Document toEntity(DocumentCreateRequest request);

    @Override
    @Mapping(target = "courseId", source = "course.id")
    @Mapping(target = "chapterId", source = "chapter.id")
    @Mapping(target = "topicId", source = "topic.id")
    @Mapping(
            target = "version",
            source = "version"
    )
    DocumentResponse toResponse(Document document);

//    @Override
//    @BeanMapping(
//            nullValuePropertyMappingStrategy =
//                    NullValuePropertyMappingStrategy.IGNORE
//    )
//    @Mapping(target = "id", ignore = true)
//    @Mapping(target = "course", ignore = true)
//    @Mapping(target = "chapter", ignore = true)
//    @Mapping(target = "topic", ignore = true)
//    @Mapping(target = "currentVersion", ignore = true)
//    @Mapping(target = "lifecycleStatus", ignore = true)
//    @Mapping(target = "uploadedBy", ignore = true)
//    @Mapping(target = "deletedAt", ignore = true)
//    @Mapping(target = "createdAt", ignore = true)
//    @Mapping(target = "updatedAt", ignore = true)
//    void partialUpdate(
//            DocumentUpdateRequest request,
//            @MappingTarget Document document
//    );
}
