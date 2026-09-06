package com.smartlearning.system.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** Shared, isolated configuration for System integration tests. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public abstract class SystemIntegrationTest {
}
