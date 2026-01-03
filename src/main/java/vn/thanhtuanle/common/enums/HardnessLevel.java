package vn.thanhtuanle.common.enums;

import lombok.Getter;

@Getter
public enum HardnessLevel {
    EASY(1),
    MEDIUM(2),
    HARD(3),
    VERY_HARD(4);

    private final int value;

    HardnessLevel(int value) {
        this.value = value;
    }

    public static HardnessLevel fromValue(int value) {
        for (HardnessLevel level : HardnessLevel.values()) {
            if (level.getValue() == value) {
                return level;
            }
        }
        throw new IllegalArgumentException("Invalid HardnessLevel value: " + value);
    }
}
