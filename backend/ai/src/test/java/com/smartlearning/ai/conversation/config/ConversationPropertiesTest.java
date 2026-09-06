package com.smartlearning.ai.conversation.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class ConversationPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfiguration.class);

    @Test
    void bindsHistoryLimit() {
        contextRunner
                .withPropertyValues("app.conversation.history-limit=20")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(ConversationProperties.class).historyLimit()).isEqualTo(20);
                });
    }

    @ParameterizedTest(name = "rejects history limit {0}")
    @ValueSource(ints = {0, -1, -20})
    void rejectsNonPositiveHistoryLimit(int historyLimit) {
        contextRunner
                .withPropertyValues("app.conversation.history-limit=" + historyLimit)
                .run(context -> assertThat(context).hasFailed());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(ConversationProperties.class)
    static class TestConfiguration {
    }
}
