package com.smartlearning.core.config;

import com.smartlearning.core.course.messaging.kafka.CourseKafkaTopics;
import com.smartlearning.core.document.messaging.kafka.DocumentKafkaTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration(proxyBeanMethods = false)
public class KafkaTopicConfig {

    @Value("${app.kafka.topic.partitions:3}")
    private int partitions;

    @Value("${app.kafka.topic.replication-factor:1}")
    private short replicationFactor;

    @Bean
    NewTopic documentIngestionRequestedTopic() {
        return topic(DocumentKafkaTopics.DOCUMENT_INGESTION_REQUESTED);
    }

    @Bean
    NewTopic documentIngestionCompletedTopic() {
        return topic(DocumentKafkaTopics.DOCUMENT_INGESTION_COMPLETED);
    }

    @Bean
    NewTopic documentIngestionFailedTopic() {
        return topic(DocumentKafkaTopics.DOCUMENT_INGESTION_FAILED);
    }

    @Bean
    NewTopic documentDeletionRequestedTopic() {
        return topic(DocumentKafkaTopics.DOCUMENT_DELETION_REQUESTED);
    }

    @Bean
    NewTopic topicKnowledgeIndexRequestedTopic() {
        return topic(CourseKafkaTopics.TOPIC_KNOWLEDGE_INDEX_REQUESTED);
    }

    private NewTopic topic(String topicName) {
        return TopicBuilder
                .name(topicName)
                .partitions(partitions)
                .replicas(replicationFactor)
                .build();
    }
}