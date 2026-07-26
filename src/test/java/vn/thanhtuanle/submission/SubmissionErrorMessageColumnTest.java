package vn.thanhtuanle.submission;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards against the entity/Postgres schema drift that made long verdict messages unstorable.
 *
 * The default {@code test} profile runs on H2 with {@code ddl-auto=update}, so the schema is built
 * from the entities — where {@code errorMessage} is {@code @Column(columnDefinition = "TEXT")}. That
 * can never reproduce the production bug: the real Postgres column came from the Flyway baseline as
 * {@code character varying(255)}, so a compiler/runtime error dump longer than 255 chars failed to
 * persist (SQLState 22001), rolling back the verdict transaction and poison-pilling the results
 * consumer. This test runs the real Flyway migrations against a real Postgres and asserts a long
 * message round-trips.
 */
@Testcontainers(disabledWithoutDocker = true)
class SubmissionErrorMessageColumnTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    // Seeded by V2__seed_reference_data.sql; problem_id/language_id are NOT NULL, user_id is not.
    private static final String C_LANGUAGE_ID = "8eb51c84-2d03-4f86-92c3-1f31a671ff12";

    @Test
    void longVerdictErrorMessagePersistsWithoutOverflow() throws Exception {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .baselineOnMigrate(false)
                .load()
                .migrate();

        // A real gcc dump (even with -fmax-errors=3) easily exceeds the old varchar(255) cap.
        String longError = "error: something went wrong on this line; ".repeat(10); // 420 chars
        assertThat(longError.length()).isGreaterThan(255);

        UUID problemId = UUID.randomUUID();
        UUID submissionId = UUID.randomUUID();

        try (Connection c = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             Statement s = c.createStatement()) {

            s.executeUpdate("INSERT INTO public.t_problems "
                    + "(id, hardness_level, memory_limit, status, time_limit, created_at, updated_at, title) "
                    + "VALUES ('" + problemId + "', 1, 268435456, 1, 1000, now(), now(), 'IT problem')");

            // The write that used to blow up with SQLState 22001 "value too long for varchar(255)".
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO public.t_submissions "
                    + "(id, status, \"time\", created_at, updated_at, language_id, problem_id, error_message) "
                    + "VALUES (?, ?, 0, now(), now(), ?, ?, ?)")) {
                ps.setObject(1, submissionId);
                ps.setInt(2, -2); // COMPILE_ERROR
                ps.setObject(3, UUID.fromString(C_LANGUAGE_ID));
                ps.setObject(4, problemId);
                ps.setString(5, longError);
                ps.executeUpdate();
            }

            try (ResultSet rs = s.executeQuery(
                    "SELECT error_message FROM public.t_submissions WHERE id = '" + submissionId + "'")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("error_message")).isEqualTo(longError);
            }
        }
    }
}
