package com.smartlearning.core.course.service.impl;

import com.smartlearning.common.error.ApplicationException;
import com.smartlearning.common.error.CommonErrorCode;
import com.smartlearning.core.course.dto.request.CourseChapterCreateRequest;
import com.smartlearning.core.course.dto.request.CourseChapterUpdateRequest;
import com.smartlearning.core.course.dto.request.CourseTopicCreateRequest;
import com.smartlearning.core.course.dto.request.CourseTopicUpdateRequest;
import com.smartlearning.core.course.dto.response.CourseChapterResponse;
import com.smartlearning.core.course.dto.response.CourseTopicResponse;
import com.smartlearning.core.course.entity.Course;
import com.smartlearning.core.course.entity.CourseChapter;
import com.smartlearning.core.course.entity.CourseTopic;
import com.smartlearning.core.course.entity.enums.CourseContentStatus;
import com.smartlearning.core.course.mapper.CourseChapterMapper;
import com.smartlearning.core.course.mapper.CourseTopicMapper;
import com.smartlearning.core.course.repository.CourseChapterRepository;
import com.smartlearning.core.course.repository.CourseTopicRepository;
import com.smartlearning.core.course.sercurity.CourseAccessPolicy;
import com.smartlearning.core.course.service.CourseContentService;
import com.smartlearning.core.course.utils.CourseUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CourseContentServiceImpl implements CourseContentService {

    private final CourseUtils courseUtils;
    private final CourseAccessPolicy courseAccessPolicy;
    private final CourseChapterRepository chapterRepository;
    private final CourseTopicRepository topicRepository;
    private final CourseChapterMapper chapterMapper;
    private final CourseTopicMapper topicMapper;

    @Override
    @Transactional(readOnly = true)
    public List<CourseChapterResponse> getChapters(UUID courseId, UUID currentUserId) {
        requireTeachingCourse(courseId, currentUserId);
        return chapterRepository.findAllByCourseIdAndDeletedAtIsNullOrderByOrderIndexAsc(courseId)
                .stream()
                .map(chapterMapper::toResponse)
                .toList();
    }

    @Override
    public CourseChapterResponse createChapter(
            UUID courseId,
            CourseChapterCreateRequest request,
            UUID currentUserId
    ) {
        Course course = requireTeachingCourse(courseId, currentUserId);
        CourseChapter chapter = chapterMapper.toEntity(request);
        chapter.setCourse(course);
        chapter.setTitle(request.title().trim());
        chapter.setStatus(request.status() == null ? CourseContentStatus.DRAFT : request.status());
        chapter.setOrderIndex(request.orderIndex() == null
                ? chapterRepository.findMaxOrderIndex(courseId) + 1
                : request.orderIndex());
        requireChapterOrderAvailable(courseId, chapter.getOrderIndex(), null);
        return chapterMapper.toResponse(chapterRepository.save(chapter));
    }

    @Override
    public CourseChapterResponse updateChapter(
            UUID courseId,
            UUID chapterId,
            CourseChapterUpdateRequest request,
            UUID currentUserId
    ) {
        requireTeachingCourse(courseId, currentUserId);
        CourseChapter chapter = requireChapter(courseId, chapterId);
        if (request.orderIndex() != null) {
            requireChapterOrderAvailable(courseId, request.orderIndex(), chapterId);
        }
        chapterMapper.partialUpdate(request, chapter);
        if (request.title() != null) {
            chapter.setTitle(request.title().trim());
        }
        return chapterMapper.toResponse(chapter);
    }

    @Override
    public void deleteChapter(UUID courseId, UUID chapterId, UUID currentUserId) {
        requireTeachingCourse(courseId, currentUserId);
        CourseChapter chapter = requireChapter(courseId, chapterId);
        Instant deletedAt = Instant.now();
        chapter.setStatus(CourseContentStatus.ARCHIVED);
        chapter.setDeletedAt(deletedAt);
        topicRepository.findAllByChapterIdAndDeletedAtIsNullOrderByOrderIndexAsc(chapterId)
                .forEach(topic -> {
                    topic.setStatus(CourseContentStatus.ARCHIVED);
                    topic.setDeletedAt(deletedAt);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseTopicResponse> getTopics(UUID courseId, UUID chapterId, UUID currentUserId) {
        requireTeachingCourse(courseId, currentUserId);
        requireChapter(courseId, chapterId);
        return topicRepository.findAllByChapterIdAndDeletedAtIsNullOrderByOrderIndexAsc(chapterId)
                .stream()
                .map(topicMapper::toResponse)
                .toList();
    }

    @Override
    public CourseTopicResponse createTopic(
            UUID courseId,
            UUID chapterId,
            CourseTopicCreateRequest request,
            UUID currentUserId
    ) {
        requireTeachingCourse(courseId, currentUserId);
        CourseChapter chapter = requireChapter(courseId, chapterId);
        CourseTopic topic = topicMapper.toEntity(request);
        topic.setChapter(chapter);
        topic.setTitle(request.title().trim());
        topic.setStatus(request.status() == null ? CourseContentStatus.DRAFT : request.status());
        topic.setOrderIndex(request.orderIndex() == null
                ? topicRepository.findMaxOrderIndex(chapterId) + 1
                : request.orderIndex());
        requireTopicOrderAvailable(chapterId, topic.getOrderIndex(), null);
        return topicMapper.toResponse(topicRepository.save(topic));
    }

    @Override
    public CourseTopicResponse updateTopic(
            UUID courseId,
            UUID chapterId,
            UUID topicId,
            CourseTopicUpdateRequest request,
            UUID currentUserId
    ) {
        requireTeachingCourse(courseId, currentUserId);
        requireChapter(courseId, chapterId);
        CourseTopic topic = requireTopic(chapterId, topicId);
        if (request.orderIndex() != null) {
            requireTopicOrderAvailable(chapterId, request.orderIndex(), topicId);
        }
        topicMapper.partialUpdate(request, topic);
        if (request.title() != null) {
            topic.setTitle(request.title().trim());
        }
        return topicMapper.toResponse(topic);
    }

    @Override
    public void deleteTopic(UUID courseId, UUID chapterId, UUID topicId, UUID currentUserId) {
        requireTeachingCourse(courseId, currentUserId);
        requireChapter(courseId, chapterId);
        CourseTopic topic = requireTopic(chapterId, topicId);
        topic.setStatus(CourseContentStatus.ARCHIVED);
        topic.setDeletedAt(Instant.now());
    }

    private Course requireTeachingCourse(UUID courseId, UUID currentUserId) {
        Course course = courseUtils.requireCourse(courseId);
        courseAccessPolicy.requireTeachingMember(courseId, currentUserId);
        return course;
    }

    private CourseChapter requireChapter(UUID courseId, UUID chapterId) {
        return chapterRepository.findByIdAndCourseIdAndDeletedAtIsNull(chapterId, courseId)
                .orElseThrow(() -> new ApplicationException(
                        CommonErrorCode.RESOURCE_NOT_FOUND,
                        "Không tìm thấy chương"
                ));
    }

    private CourseTopic requireTopic(UUID chapterId, UUID topicId) {
        return topicRepository.findByIdAndChapterIdAndDeletedAtIsNull(topicId, chapterId)
                .orElseThrow(() -> new ApplicationException(
                        CommonErrorCode.RESOURCE_NOT_FOUND,
                        "Không tìm thấy chủ đề"
                ));
    }

    private void requireChapterOrderAvailable(UUID courseId, Integer orderIndex, UUID excludedId) {
        boolean exists = excludedId == null
                ? chapterRepository.existsByCourseIdAndOrderIndex(courseId, orderIndex)
                : chapterRepository.existsByCourseIdAndOrderIndexAndIdNot(courseId, orderIndex, excludedId);
        if (exists) {
            throw new ApplicationException(
                    CommonErrorCode.DATA_CONFLICT,
                    "Thứ tự chương đã được sử dụng"
            );
        }
    }

    private void requireTopicOrderAvailable(UUID chapterId, Integer orderIndex, UUID excludedId) {
        boolean exists = excludedId == null
                ? topicRepository.existsByChapterIdAndOrderIndex(chapterId, orderIndex)
                : topicRepository.existsByChapterIdAndOrderIndexAndIdNot(chapterId, orderIndex, excludedId);
        if (exists) {
            throw new ApplicationException(
                    CommonErrorCode.DATA_CONFLICT,
                    "Thứ tự chủ đề đã được sử dụng"
            );
        }
    }
}
