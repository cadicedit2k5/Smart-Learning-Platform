package com.smartlearning.system.auth.controller.internal;

import com.smartlearning.common.entity.Authorities;
import com.smartlearning.system.auth.dto.request.UserBatchLookupRequest;
import com.smartlearning.system.auth.dto.response.UserSummaryResponse;
import com.smartlearning.system.auth.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final UserService userService;

    @PostMapping("/lookup")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<UserSummaryResponse>> lookupUsers(
            @Valid @RequestBody UserBatchLookupRequest request) {
        return ResponseEntity.ok(userService.handleLookupUsers(request.userIds()));
    }
}
