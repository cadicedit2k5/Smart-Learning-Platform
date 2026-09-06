package com.smartlearning.core.document.utils;

import com.smartlearning.common.error.ApplicationException;
import com.smartlearning.common.error.CommonErrorCode;
import com.smartlearning.core.document.entity.Document;
import com.smartlearning.core.document.repository.DocumentRepository;
import com.smartlearning.storage.dto.StoredFile;
import com.smartlearning.storage.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DocumentUtils {

    private final DocumentRepository documentRepository;
    private final FileStorageService fileStorageService;

    public Document requireDocument(UUID documentId) {
        return documentRepository.findByIdAndDeletedAtIsNull(documentId)
                .orElseThrow(() -> new ApplicationException(
                        CommonErrorCode.RESOURCE_NOT_FOUND,
                                "Không tìm thấy tài liệu"));
    }

    public StoredFile downloadDocument(Document document) {
        if (document.getVersion() == null) {
            throw new ApplicationException(CommonErrorCode.RESOURCE_NOT_FOUND,
                    "Không tìm thấy phiên bản tài liệu");
        }

        return fileStorageService.download(document.getVersion().getStorageKey());
    }
}
