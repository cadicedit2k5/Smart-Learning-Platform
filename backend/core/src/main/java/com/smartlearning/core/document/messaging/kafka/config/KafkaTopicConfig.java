package com.smartlearning.core.document.messaging.kafka.config;

import com.smartlearning.core.document.messaging.kafka.KafkaTopics;
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
        return TopicBuilder
                .name(KafkaTopics.DOCUMENT_INGESTION_REQUESTED)
                .partitions(partitions)
                .replicas(replicationFactor)
                .build();
    }

    @Bean
    NewTopic documentIngestionCompletedTopic() {
        return TopicBuilder
                .name(KafkaTopics.DOCUMENT_INGESTION_COMPLETED)
                .partitions(partitions)
                .replicas(replicationFactor)
                .build();
    }

    @Bean
    NewTopic documentIngestionFailedTopic() {
        return TopicBuilder
                .name(KafkaTopics.DOCUMENT_INGESTION_FAILED)
                .partitions(partitions)
                .replicas(replicationFactor)
                .build();
    }
}