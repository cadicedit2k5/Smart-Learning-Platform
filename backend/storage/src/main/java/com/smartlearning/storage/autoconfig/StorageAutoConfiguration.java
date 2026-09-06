package com.smartlearning.storage.autoconfig;

import com.smartlearning.storage.config.MinioProperties;
import com.smartlearning.storage.service.FileStorageService;
import com.smartlearning.storage.service.impl.MinioFileStorageService;
import io.minio.MinioClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(MinioProperties.class)
public class StorageAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    MinioClient minioClient(MinioProperties properties) {
        return MinioClient.builder()
                .endpoint(properties.endpoint())
                .credentials(properties.accessKey(), properties.secretKey())
                .build();
    }

    @Bean
    @ConditionalOnMissingBean(FileStorageService.class)
    FileStorageService fileStorageService(
            MinioClient minioClient,
            MinioProperties properties
    ) {
        return new MinioFileStorageService(minioClient, properties);
    }
}
