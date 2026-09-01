package com.smartlearning.core.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Shared configuration for Core integration tests.
 *
 * <p>The {@code test} profile points to an isolated in-memory H2 database. A
 * transaction is opened for each test and rolled back afterwards, so tests can
 * share the Spring context and database session factory without leaking data.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public abstract class CoreIntegrationTest {
}
