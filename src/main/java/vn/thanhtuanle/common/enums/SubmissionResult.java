package vn.thanhtuanle.common.enums;

import lombok.Getter;

@Getter
public enum SubmissionResult {
    WRONG_ANSWER(-1),
    SUCCESS(0),
    CPU_TIME_LIMIT_EXCEEDED(1),
    REAL_TIME_LIMIT_EXCEEDED(2),
    MEMORY_LIMIT_EXCEEDED(3),
    RUNTIME_ERROR(4),
    SYSTEM_ERROR(5);

    private final int value;

    SubmissionResult(int value) {
        this.value = value;
    }

    public static SubmissionResult fromValue(int value) {
        for (SubmissionResult result : SubmissionResult.values()) {
            if (result.getValue() == value) {
                return result;
            }
        }
        throw new IllegalArgumentException("Invalid SubmissionResult value: " + value);
    }
}
