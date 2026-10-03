package vn.thanhtuanle.submission.problem;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import vn.thanhtuanle.common.exception.GlobalExceptionHandler;

import static org.assertj.core.api.Assertions.assertThat;

/** The two failures a caller of ProblemCatalog can see map to today's 404 and a new 503. */
class ProblemCatalogErrorsTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void anUnknownProblemIsTheSame404AsBefore() {
        var body = handler.handleResourceNotFoundException(new ProblemNotFoundException());

        assertThat(body.getStatus()).isEqualTo(404);
        assertThat(body.getMessage()).isEqualTo("Problem not found");
    }

    @Test
    void unreachableProblemsAre503() {
        var response = handler.handleAppException(new ProblemCatalogUnavailableException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody().getStatus()).isEqualTo(503);
    }
}
