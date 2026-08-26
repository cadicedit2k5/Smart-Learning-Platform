package com.smartlearning.ai;

import com.smartlearning.ai.conversation.config.ConversationProperties;
import com.smartlearning.common.config.CommonSercurityConfigs;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@EnableConfigurationProperties(ConversationProperties.class)
@Import({
        CommonSercurityConfigs.class,
})
public class AiApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiApplication.class, args);
    }

}
