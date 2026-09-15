package com.smartlearning.core.course.service.impl;

import com.smartlearning.common.error.ApplicationException;
import com.smartlearning.common.error.CommonErrorCode;
import com.smartlearning.core.course.dto.request.DiscussionCreateRequest;
import com.smartlearning.core.course.dto.request.DiscussionReplyCreateRequest;
import com.smartlearning.core.course.dto.response.DiscussionAuthorResponse;
import com.smartlearning.core.course.dto.response.DiscussionReplyResponse;
import com.smartlearning.core.course.dto.response.DiscussionResponse;
import com.smartlearning.core.course.entity.Course;
import com.smartlearning.core.course.entity.CourseDiscussion;
import com.smartlearning.core.course.entity.CourseDiscussionReply;
import com.smartlearning.core.course.entity.CourseTopic;
import com.smartlearning.core.course.repository.CourseDiscussionReplyRepository;
import com.smartlearning.core.course.repository.CourseDiscussionRepository;
import com.smartlearning.core.course.repository.CourseTopicRepository;
import com.smartlearning.core.course.sercurity.CourseAccessPolicy;
import com.smartlearning.core.course.service.CourseDiscussionService;
import com.smartlearning.core.course.utils.CourseUtils;
import com.smartlearning.core.infrastructure.dto.SystemUserResponse;
import com.smartlearning.core.infrastructure.http.SystemClient;
import com.smartlearning.core.notification.entity.enums.NotificationType;
import com.smartlearning.core.notification.service.NotificationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class CourseDiscussionServiceImpl implements CourseDiscussionService {

    private final CourseDiscussionRepository discussionRepository;
    private final CourseDiscussionReplyRepository replyRepository;
    private final CourseTopicRepository topicRepository;
    private final CourseUtils courseUtils;
    private final CourseAccessPolicy courseAccessPolicy;
    private final SystemClient systemClient;
    private final NotificationService notificationService;

    @Override
    public List<DiscussionResponse> getDiscussions(
            UUID courseId,
            UUID userId,
            String accessToken
    ) {
        courseUtils.requireCourse(courseId);
        courseAccessPolicy.requireActiveMember(courseId, userId);

        List<CourseDiscussion> discussions =
                discussionRepository.findAllByCourseIdOrderByCreatedAtDesc(courseId);

        return buildResponses(discussions, accessToken);
    }

    @Override
    public DiscussionResponse createDiscussion(
            UUID courseId,
            UUID userId,
            String accessToken,
            DiscussionCreateRequest request
    ) {
        Course course = courseUtils.requireCourse(courseId);
        courseAccessPolicy.requireActiveMember(courseId, userId);
        requireRichTextContent(request.content());

        CourseTopic topic = null;

        if (request.topicId() != null) {
            topic = topicRepository
                    .findByIdAndChapterCourseIdAndDeletedAtIsNullAndChapterDeletedAtIsNull(
                            request.topicId(),
                            courseId
                    )
                    .orElseThrow(() -> new ApplicationException(
                            CommonErrorCode.RESOURCE_NOT_FOUND,
                            "Không tìm thấy bài học này."
                    ));
        }

        CourseDiscussion discussion = new CourseDiscussion();
        discussion.setCourse(course);
        discussion.setTopic(topic);
        discussion.setAuthorId(userId);
        discussion.setTitle(request.title().trim());
        discussion.setContent(request.content());

        CourseDiscussion saved = discussionRepository.save(discussion);

        if (!course.getCreatedBy().equals(userId)) {
            notificationService.create(
                    course.getCreatedBy(),
                    NotificationType.DISCUSSION_CREATED,
                    "Thảo luận mới",
                    "Có câu hỏi mới: " + saved.getTitle(),
                    courseId
            );
        }

        return buildResponses(List.of(saved), accessToken).get(0);
    }

    @Override
    public DiscussionResponse createReply(
            UUID courseId,
            UUID discussionId,
            UUID userId,
            String accessToken,
            DiscussionReplyCreateRequest request
    ) {
        courseUtils.requireCourse(courseId);
        courseAccessPolicy.requireActiveMember(courseId, userId);
        requireRichTextContent(request.content());

        CourseDiscussion discussion = discussionRepository
                .findByIdAndCourseId(discussionId, courseId)
                .orElseThrow(() -> new ApplicationException(
                        CommonErrorCode.RESOURCE_NOT_FOUND,
                        "Không tìm thấy thảo luận này."
                ));

        CourseDiscussionReply reply = new CourseDiscussionReply();
        reply.setDiscussion(discussion);
        reply.setAuthorId(userId);
        reply.setContent(request.content());

        replyRepository.save(reply);

        if (!discussion.getAuthorId().equals(userId)) {
            notificationService.create(
                    discussion.getAuthorId(),
                    NotificationType.DISCUSSION_REPLIED,
                    "Thảo luận có phản hồi mới",
                    "Có phản hồi mới trong: " + discussion.getTitle(),
                    courseId
            );
        }

        return buildResponses(List.of(discussion), accessToken).get(0);
    }

    private List<DiscussionResponse> buildResponses(
            List<CourseDiscussion> discussions,
            String accessToken
    ) {
        if (discussions.isEmpty()) {
            return List.of();
        }

        List<UUID> discussionIds = discussions.stream()
                .map(CourseDiscussion::getId)
                .toList();

        List<CourseDiscussionReply> replies =
                replyRepository.findAllByDiscussion_IdInOrderByCreatedAtAsc(discussionIds);

        Set<UUID> userIds = new HashSet<>();

        discussions.forEach(discussion -> userIds.add(discussion.getAuthorId()));
        replies.forEach(reply -> userIds.add(reply.getAuthorId()));

        Map<UUID, SystemUserResponse> users = systemClient
                .lookupUsers(userIds, accessToken)
                .stream()
                .collect(Collectors.toMap(
                        SystemUserResponse::id,
                        Function.identity()
                ));

        Map<UUID, List<CourseDiscussionReply>> repliesByDiscussion =
                replies.stream().collect(Collectors.groupingBy(
                        reply -> reply.getDiscussion().getId()
                ));

        return discussions.stream()
                .map(discussion -> toResponse(
                        discussion,
                        repliesByDiscussion.getOrDefault(
                                discussion.getId(),
                                List.of()
                        ),
                        users
                ))
                .toList();
    }

    private DiscussionResponse toResponse(
            CourseDiscussion discussion,
            List<CourseDiscussionReply> replies,
            Map<UUID, SystemUserResponse> users
    ) {
        DiscussionAuthorResponse author =
                toAuthor(users.get(discussion.getAuthorId()));

        List<DiscussionReplyResponse> replyResponses = replies.stream()
                .map(reply -> new DiscussionReplyResponse(
                        reply.getId(),
                        reply.getAuthorId(),
                        toAuthor(users.get(reply.getAuthorId())),
                        reply.getContent(),
                        reply.getCreatedAt()
                ))
                .toList();

        return new DiscussionResponse(
                discussion.getId(),
                discussion.getCourse().getId(),
                discussion.getTopic() == null
                        ? null
                        : discussion.getTopic().getId(),
                discussion.getTopic() == null
                        ? null
                        : discussion.getTopic().getTitle(),
                discussion.getAuthorId(),
                author,
                discussion.getTitle(),
                discussion.getContent(),
                replyResponses,
                discussion.getCreatedAt(),
                discussion.getUpdatedAt()
        );
    }

    private DiscussionAuthorResponse toAuthor(SystemUserResponse user) {
        if (user == null) {
            return null;
        }

        return new DiscussionAuthorResponse(
                user.id(),
                user.fullName(),
                user.email()
        );
    }

    private void requireRichTextContent(Map<String, Object> content) {
        if (content == null || !containsText(content)) {
            throw new ApplicationException(
                    CommonErrorCode.VALIDATION_FAILED,
                    "Nội dung không được để trống."
            );
        }
    }

    private boolean containsText(Object value) {
        if (value instanceof Map<?, ?> map) {
            Object text = map.get("text");

            if (text instanceof String string && !string.isBlank()) {
                return true;
            }

            return map.values().stream().anyMatch(this::containsText);
        }

        if (value instanceof Collection<?> collection) {
            return collection.stream().anyMatch(this::containsText);
        }

        return false;
    }
}
