package vn.thanhtuanle.common.enums;

import lombok.Getter;

@Getter
public enum ProblemStatus {
    ACTIVE(1),
    INACTIVE(0);

    private final int value;

    ProblemStatus(int value) {
        this.value = value;
    }

    public static ProblemStatus fromValue(int value) {
        for (ProblemStatus status : ProblemStatus.values()) {
            if (status.getValue() == value) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid ProblemStatus value: " + value);
    }
}
