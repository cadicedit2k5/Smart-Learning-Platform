package com.smartlearning.storage.dto;

import java.io.InputStream;

public record StoredFile(
        InputStream inputStream,
        String contentType,
        long size,
        String fileName
) {
}