package com.smartlearning.storage;

import com.smartlearning.storage.autoconfig.StorageAutoConfiguration;
import com.smartlearning.storage.service.FileStorageService;
import io.minio.MinioClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class StorageApplicationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(StorageAutoConfiguration.class))
            .withPropertyValues(
                    "app.storage.minio.endpoint=http://localhost:9000",
                    "app.storage.minio.access-key=test-access-key",
                    "app.storage.minio.secret-key=test-secret-key",
                    "app.storage.minio.bucket=test-bucket",
                    "app.storage.minio.presigned-url-expiry=15m"
            );

    @Test
    void contextLoads() {
        contextRunner.run(context -> {
            org.assertj.core.api.Assertions.assertThat(context).hasNotFailed();
            org.assertj.core.api.Assertions.assertThat(context).hasSingleBean(MinioClient.class);
            org.assertj.core.api.Assertions.assertThat(context).hasSingleBean(FileStorageService.class);
        });
    }
}
