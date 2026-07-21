package com.smartlearning.storage.service;

import com.smartlearning.storage.dto.FileUploadResponse;
import com.smartlearning.storage.dto.PresignedUrlResponse;
import com.smartlearning.storage.dto.StoredFile;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    FileUploadResponse upload(
            MultipartFile file,
            String folder
    );

    StoredFile download(String objectName);

    PresignedUrlResponse createDownloadUrl(String objectName);

    void delete(String objectName);
}
