package vn.thanhtuanle.submission;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * V16: existing submissions get their problem's slug, a submission may name a problem this database
 * does not know (problems move to problem-service in 2b), and the column stays nullable so the image
 * before 2a can still insert after a rollback.
 */
@Testcontainers(disabledWithoutDocker = true)
class SubmissionsDecoupledFromProblemsMigrationTest {

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

    private static void insertSubmission(Statement s, UUID id, UUID problemId, String slugOrNull) throws Exception {
        s.executeUpdate("INSERT INTO public.t_submissions "
                + "(id, status, \"time\", created_at, updated_at, language_id, problem_id, user_id"
                + (slugOrNull == null ? "" : ", problem_slug") + ") VALUES ('" + id + "', 6, 0, now(), now(), '"
                + C_LANGUAGE_ID + "', '" + problemId + "', '" + UUID.randomUUID() + "'"
                + (slugOrNull == null ? "" : ", '" + slugOrNull + "'") + ")");
    }

    @Test
    void submissionsNoLongerNeedTheirProblemInThisDatabase() throws Exception {
        flyway("15").migrate();

        UUID problemId = UUID.randomUUID();
        UUID before = UUID.randomUUID();
        try (Connection c = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             Statement s = c.createStatement()) {
            s.executeUpdate("INSERT INTO public.t_problems "
                    + "(id, hardness_level, memory_limit, status, time_limit, created_at, updated_at, title, problem_slug) "
                    + "VALUES ('" + problemId + "', 1, 256, 1, 1000, now(), now(), 'IT problem', 'two-sum')");
            insertSubmission(s, before, problemId, null);

            flyway("latest").migrate();

            try (ResultSet rs = s.executeQuery("SELECT problem_slug FROM public.t_submissions WHERE id = '" + before + "'")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString(1)).as("backfilled from t_problems").isEqualTo("two-sum");
            }

            UUID fromProblemService = UUID.randomUUID();   // a problem this database has never seen
            insertSubmission(s, UUID.randomUUID(), fromProblemService, "new-problem");
            insertSubmission(s, UUID.randomUUID(), problemId, null);   // what the pre-2a image inserts

            try (ResultSet rs = s.executeQuery("SELECT count(*) FROM public.t_submissions")) {
                rs.next();
                assertThat(rs.getInt(1)).isEqualTo(3);
            }
        }
    }
}
