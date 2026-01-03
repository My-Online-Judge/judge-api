package vn.thanhtuanle.common.enums;

import lombok.Getter;

@Getter
public enum SubmissionResult {
    OK("OK"),
    ERR("ERR");

    private final String value;

    SubmissionResult(String value) {
        this.value = value;
    }

    public static SubmissionResult fromValue(String value) {
        for (SubmissionResult result : SubmissionResult.values()) {
            if (result.getValue().equals(value)) {
                return result;
            }
        }
        throw new IllegalArgumentException("Invalid SubmissionResult value: " + value);
    }
}
