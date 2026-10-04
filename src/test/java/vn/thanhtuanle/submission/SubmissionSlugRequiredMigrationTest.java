package vn.thanhtuanle.submission;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * V18 (2b): every submission names its problem by slug. Rows a rolled-back pre-2a image inserted without one
 * are filled from t_problems (still in oj-db until sub-project 3), then the column becomes NOT NULL.
 */
@Testcontainers(disabledWithoutDocker = true)
class SubmissionSlugRequiredMigrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    // Seeded by V2__seed_reference_data.sql.
    private static final String C_LANGUAGE_ID = "8eb51c84-2d03-4f86-92c3-1f31a671ff12";

    private Flyway flyway(String target) {
        return Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .target(target)
                .load();
    }

    private static void insertSubmission(Statement s, UUID id, UUID problemId, String slugOrNull) throws SQLException {
        s.executeUpdate("INSERT INTO public.t_submissions "
                + "(id, status, \"time\", created_at, updated_at, language_id, problem_id, user_id, problem_slug) "
                + "VALUES ('" + id + "', 0, 0, now(), now(), '" + C_LANGUAGE_ID + "', '" + problemId + "', '"
                + UUID.randomUUID() + "', " + (slugOrNull == null ? "NULL" : "'" + slugOrNull + "'") + ")");
    }

    @Test
    void slugsLeftOutAfterARollbackAreFilledAndTheColumnBecomesRequired() throws Exception {
        flyway("17").migrate();

        UUID problemId = UUID.randomUUID();
        UUID withoutSlug = UUID.randomUUID();
        try (Connection c = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             Statement s = c.createStatement()) {
            s.executeUpdate("INSERT INTO public.t_problems "
                    + "(id, hardness_level, memory_limit, status, time_limit, created_at, updated_at, title, problem_slug) "
                    + "VALUES ('" + problemId + "', 1, 256, 1, 1000, now(), now(), 'IT problem', 'gcd')");
            insertSubmission(s, withoutSlug, problemId, null);
            insertSubmission(s, UUID.randomUUID(), problemId, "gcd");

            flyway("latest").migrate();

            try (ResultSet rs = s.executeQuery("SELECT problem_slug FROM public.t_submissions WHERE id = '" + withoutSlug + "'")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString(1)).isEqualTo("gcd");
            }
            assertThatThrownBy(() -> insertSubmission(s, UUID.randomUUID(), problemId, null))
                    .isInstanceOf(SQLException.class)
                    .hasMessageContaining("problem_slug");
        }
    }
}
