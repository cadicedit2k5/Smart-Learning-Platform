package com.smartlearning.ai.conversation.controller;


import com.smartlearning.ai.conversation.dto.request.ChatMessageRequest;
import com.smartlearning.ai.conversation.dto.response.ChatMessageResponse;
import com.smartlearning.ai.conversation.dto.response.ChatTurnResponse;
import com.smartlearning.ai.conversation.dto.response.ConversationSummaryResponse;
import com.smartlearning.ai.conversation.service.ConversationService;
import com.smartlearning.common.dto.request.PagingRequest;
import com.smartlearning.common.dto.response.ApiResponse;
import com.smartlearning.common.dto.response.ApiResponses;
import com.smartlearning.common.dto.response.pagination.PagingResponse;
import com.smartlearning.common.entity.Authorities;
import com.smartlearning.common.utils.JwtUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/courses/{courseId}/ai/conversations")
@RequiredArgsConstructor
public class AiConversationController {

    private final ConversationService conversationService;

    @PostMapping
    @PreAuthorize(Authorities.COURSE_READ)
    public ResponseEntity<ApiResponse<ChatTurnResponse>> create(
            @PathVariable UUID courseId,
            @Valid @RequestBody ChatMessageRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        return ApiResponses.created(conversationService.startConversation(
                                courseId,
                                JwtUtils.getUserId(jwt),
                                jwt.getTokenValue(),
                                request.content()));
    }

    @PostMapping("/{conversationId}/messages")
    @PreAuthorize(Authorities.COURSE_READ)
    public ResponseEntity<ApiResponse<ChatTurnResponse>> send(
            @PathVariable UUID courseId,
            @PathVariable UUID conversationId,
            @Valid @RequestBody ChatMessageRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        return ApiResponses.ok(conversationService.sendMessage(
                        courseId,
                        conversationId,
                        JwtUtils.getUserId(jwt),
                        jwt.getTokenValue(),
                        request.content()));
    }

    @GetMapping
    @PreAuthorize(Authorities.COURSE_READ)
    public ResponseEntity<ApiResponse<PagingResponse<ConversationSummaryResponse>>> getConversations(
            @PathVariable UUID courseId,
            @ModelAttribute PagingRequest pagingRequest,
            @AuthenticationPrincipal Jwt jwt) {

        return ApiResponses.ok(conversationService.getConversations(
                        courseId,
                        JwtUtils.getUserId(jwt),
                        jwt.getTokenValue(),
                        pagingRequest));
    }

    @GetMapping("/{conversationId}/messages")
    @PreAuthorize(Authorities.COURSE_READ)
    public ResponseEntity<ApiResponse<PagingResponse<ChatMessageResponse>>> getMessages(
            @PathVariable UUID courseId,
            @PathVariable UUID conversationId,
            @ModelAttribute PagingRequest pagingRequest,
            @AuthenticationPrincipal Jwt jwt) {
        return ApiResponses.ok(conversationService.getMessages(
                        courseId,
                        conversationId,
                        JwtUtils.getUserId(jwt),
                        jwt.getTokenValue(),
                        pagingRequest));
    }
}
