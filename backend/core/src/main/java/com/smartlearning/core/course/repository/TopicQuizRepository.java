package com.smartlearning.core.course.repository;

import com.smartlearning.core.course.entity.TopicQuiz;
import com.smartlearning.core.course.entity.enums.TopicQuizStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TopicQuizRepository extends JpaRepository<TopicQuiz, UUID> {

    List<TopicQuiz> findAllByTopicIdOrderByVersionDesc(UUID topicId);

    Optional<TopicQuiz> findByIdAndTopicId(UUID quizId, UUID topicId);

    Optional<TopicQuiz> findByTopicIdAndStatus(UUID topicId, TopicQuizStatus status);

    @Query("select coalesce(max(q.version), 0) from TopicQuiz q where q.topic.id = :topicId")
    int findMaxVersion(UUID topicId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select q from TopicQuiz q where q.id = :quizId and q.topic.id = :topicId")
    Optional<TopicQuiz> findForUpdate(UUID topicId, UUID quizId);

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select q from TopicQuiz q where q.id = :quizId and q.topic.id = :topicId")
    Optional<TopicQuiz> findForSubmission(UUID topicId, UUID quizId);
}