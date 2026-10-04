package vn.thanhtuanle.submission.problem;

import vn.thanhtuanle.oj.common.web.error.ResourceNotFoundException;

public class ProblemNotFoundException extends ResourceNotFoundException {

    public ProblemNotFoundException() {
        super("Problem not found");
    }
}
