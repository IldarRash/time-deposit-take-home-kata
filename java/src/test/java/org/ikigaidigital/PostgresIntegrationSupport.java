package org.ikigaidigital;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.math.BigDecimal;

abstract class PostgresIntegrationSupport {
    // One database per test JVM; Testcontainers cleans it up at process exit.
    // Deliberately fail rather than skip if Docker is unavailable.
    static final PostgreSQLContainer<?> DATABASE = new PostgreSQLContainer<>("postgres:16.15-alpine");
    static { DATABASE.start(); }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", DATABASE::getJdbcUrl);
        registry.add("spring.datasource.username", DATABASE::getUsername);
        registry.add("spring.datasource.password", DATABASE::getPassword);
    }

    @Autowired protected JdbcTemplate jdbc;

    @BeforeEach
    void clearDeposits() {
        jdbc.execute("TRUNCATE withdrawals, \"timeDeposits\"");
    }

    protected void insertDeposit(int id, String plan, int days, String balance) {
        jdbc.update("INSERT INTO \"timeDeposits\" (id, \"planType\", days, balance) VALUES (?, ?, ?, ?)",
                id, plan, days, new BigDecimal(balance));
    }

    protected BigDecimal balance(int id) {
        return jdbc.queryForObject("SELECT balance FROM \"timeDeposits\" WHERE id = ?", BigDecimal.class, id);
    }
}
