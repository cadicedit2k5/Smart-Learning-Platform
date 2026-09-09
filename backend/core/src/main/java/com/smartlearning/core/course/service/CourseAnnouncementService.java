package com.smartlearning.core.course.service;


import com.smartlearning.core.course.dto.request.AnnouncementCreateRequest;
import com.smartlearning.core.course.dto.request.AnnouncementUpdateRequest;
import com.smartlearning.core.course.dto.response.AnnouncementResponse;

import java.util.List;
import java.util.UUID;

public interface CourseAnnouncementService {

    List<AnnouncementResponse> getAnnouncements(UUID courseId, UUID userId);

    AnnouncementResponse createAnnouncement(
            UUID courseId,
            UUID userId,
            AnnouncementCreateRequest request
    );

    AnnouncementResponse updateAnnouncement(
            UUID courseId,
            UUID announcementId,
            UUID userId,
            AnnouncementUpdateRequest request
    );

    void deleteAnnouncement(
            UUID courseId,
            UUID announcementId,
            UUID userId
    );
}
