package com.smartlearning.ai.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** Shared, isolated configuration for AI integration tests. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public abstract class AiIntegrationTest {
}
