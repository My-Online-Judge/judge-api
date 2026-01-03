package vn.thanhtuanle.common.enums;

import lombok.Getter;

@Getter
public enum SubmissionStatus {
    PENDING(1),
    JUDGED(2),
    ERROR(3);

    private final int value;

    SubmissionStatus(int value) {
        this.value = value;
    }

    public static SubmissionStatus fromValue(int value) {
        for (SubmissionStatus status : SubmissionStatus.values()) {
            if (status.getValue() == value) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid SubmissionStatus value: " + value);
    }
}
