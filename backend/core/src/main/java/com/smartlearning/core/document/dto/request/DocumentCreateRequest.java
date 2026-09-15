package com.smartlearning.core.document.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@NoArgsConstructor
public class DocumentCreateRequest {
    @NotBlank( message = "Tiêu đề không được để trống!!")
    @Size(max = 255,
    message = "Tiêu đề phải ngắn hơn 255 ký tự.")
    private String title;

    @Size(max = 10_000)
    private String description;

    @NotNull(message = "Vui lòng cung cấp tài liệu!!")
    private MultipartFile file;
}
