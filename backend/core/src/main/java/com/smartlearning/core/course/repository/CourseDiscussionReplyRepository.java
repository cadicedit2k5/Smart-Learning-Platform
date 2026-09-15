package com.smartlearning.core.course.repository;

import com.smartlearning.core.course.entity.CourseDiscussionReply;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface CourseDiscussionReplyRepository extends JpaRepository<CourseDiscussionReply, UUID> {

    List<CourseDiscussionReply> findAllByDiscussion_IdInOrderByCreatedAtAsc(
            Collection<UUID> discussionIds
    );
}
