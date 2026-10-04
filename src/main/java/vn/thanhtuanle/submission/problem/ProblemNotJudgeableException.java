package vn.thanhtuanle.submission.problem;

/**
 * The problem exists but cannot be judged: it has no published test-case bundle. A server-side fault (500,
 * as before 2b), raised before the cooldown is taken.
 */
public class ProblemNotJudgeableException extends RuntimeException {

    public ProblemNotJudgeableException(String slug) {
        super("No test-case bundle published for problem: " + slug);
    }
}
