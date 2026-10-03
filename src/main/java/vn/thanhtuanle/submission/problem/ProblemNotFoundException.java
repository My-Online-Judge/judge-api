package vn.thanhtuanle.submission.problem;

import vn.thanhtuanle.common.exception.ResourceNotFoundException;

public class ProblemNotFoundException extends ResourceNotFoundException {

    public ProblemNotFoundException() {
        super("Problem not found");
    }
}
