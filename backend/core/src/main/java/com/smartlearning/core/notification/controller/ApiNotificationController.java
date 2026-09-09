package com.smartlearning.core.notification.controller;

import com.smartlearning.common.dto.response.ApiResponse;
import com.smartlearning.common.dto.response.ApiResponses;
import com.smartlearning.common.utils.JwtUtils;
import com.smartlearning.core.notification.dto.response.NotificationResponse;
import com.smartlearning.core.notification.dto.response.UnreadCountResponse;
import com.smartlearning.core.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ApiNotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getNotifications(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ApiResponses.ok(
                notificationService.getMyNotifications(JwtUtils.getUserId(jwt))
        );
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<UnreadCountResponse>> getUnreadCount(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ApiResponses.ok(
                new UnreadCountResponse(
                        notificationService.getUnreadCount(JwtUtils.getUserId(jwt))
                )
        );
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable UUID notificationId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        notificationService.markAsRead(
                notificationId,
                JwtUtils.getUserId(jwt)
        );

        return ApiResponses.noContent();
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(
            @AuthenticationPrincipal Jwt jwt
    ) {
        notificationService.markAllAsRead(JwtUtils.getUserId(jwt));
        return ApiResponses.noContent();
    }
}
