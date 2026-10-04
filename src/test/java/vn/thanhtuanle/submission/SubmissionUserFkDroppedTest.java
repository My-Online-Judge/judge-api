package vn.thanhtuanle.submission;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Users live in identity-service's database since sub-project 1b, so a user created there never
 * appears in the t_users copy left behind in oj-db. V15 drops the foreign key from
 * t_submissions.user_id; without that, a new user's first submission fails (SQLState 23503).
 */
@Testcontainers(disabledWithoutDocker = true)
class SubmissionUserFkDroppedTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    // Seeded by V2__seed_reference_data.sql.
    private static final String C_LANGUAGE_ID = "8eb51c84-2d03-4f86-92c3-1f31a671ff12";

    @Test
    void aSubmissionByAUserUnknownToOjDbIsStored() throws Exception {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .baselineOnMigrate(false)
                .load()
                .migrate();

        UUID problemId = UUID.randomUUID();
        UUID submissionId = UUID.randomUUID();
        UUID identityOnlyUser = UUID.randomUUID();

        try (Connection c = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             Statement s = c.createStatement()) {

            s.executeUpdate("INSERT INTO public.t_problems "
                    + "(id, hardness_level, memory_limit, status, time_limit, created_at, updated_at, title) "
                    + "VALUES ('" + problemId + "', 1, 268435456, 1, 1000, now(), now(), 'IT problem')");

            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO public.t_submissions "
                    + "(id, status, \"time\", created_at, updated_at, language_id, problem_id, problem_slug, user_id) "
                    + "VALUES (?, 6, 0, now(), now(), ?, ?, 'it-problem', ?)")) {
                ps.setObject(1, submissionId);
                ps.setObject(2, UUID.fromString(C_LANGUAGE_ID));
                ps.setObject(3, problemId);
                ps.setObject(4, identityOnlyUser);
                ps.executeUpdate();
            }

            try (ResultSet rs = s.executeQuery(
                    "SELECT user_id FROM public.t_submissions WHERE id = '" + submissionId + "'")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getObject("user_id", UUID.class)).isEqualTo(identityOnlyUser);
            }
        }
    }
}
