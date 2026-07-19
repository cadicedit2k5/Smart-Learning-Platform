package com.smartlearning.system.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class UserUpdateRequest {

    @Email(message = "Email không đúng định dạng")
    private String email;

    @Size(
            min = 8,
            message = "Mật khẩu phải từ ít nhất 8 ký tự"
    )
    @Size(
            max = 36,
            message = "Mật khẩu không được vượt qua 36 ký tự"
    )
    private String password;

    @Size(
            max = 100,
            message = "Họ tên không được vượt quá 100 ký tự"
    )
    private String fullName;

    private MultipartFile avatar;
}