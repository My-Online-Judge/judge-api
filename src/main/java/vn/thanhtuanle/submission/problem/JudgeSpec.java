package vn.thanhtuanle.submission.problem;

import java.util.UUID;

/**
 * What the submission side needs from a problem to queue a submission: its identity, its limits and the
 * test-case bundle version to judge against.
 */
public record JudgeSpec(UUID problemId, String problemSlug, int timeLimitMs, long memoryLimitMb,
        String testCaseVersion) {
}
