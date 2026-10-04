package vn.thanhtuanle.submission.problem;

import vn.thanhtuanle.oj.common.web.error.AppException;
import vn.thanhtuanle.common.exception.ErrorCode;

/** Problems cannot be reached right now (in 2b: problem-service down, slow, or its circuit open). */
public class ProblemCatalogUnavailableException extends AppException {

    public ProblemCatalogUnavailableException() {
        super(ErrorCode.PROBLEMS_UNAVAILABLE);
    }
}
