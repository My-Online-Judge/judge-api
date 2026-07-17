package vn.thanhtuanle.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    USER_NOT_EXISTED("Invalid credentials", HttpStatus.UNAUTHORIZED),
    USER_EXISTED("User existed", HttpStatus.BAD_REQUEST),
    ACCESS_DENIED("Access denied", HttpStatus.FORBIDDEN),
    USER_BLOCKED("User is blocked", HttpStatus.FORBIDDEN),
    INVALID_CREDENTIALS("Invalid credentials", HttpStatus.UNAUTHORIZED),
    ROLE_NOT_EXISTED("Role not found", HttpStatus.NOT_FOUND),
    PERMISSION_NOT_EXISTED("One or more permissions do not exist", HttpStatus.BAD_REQUEST),
    ROLE_IMMUTABLE("This role's permissions cannot be modified", HttpStatus.CONFLICT),
    UNCATEGORIZED_EXCEPTION("Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String message;
    private final HttpStatus statusCode;

    ErrorCode(String message, HttpStatus statusCode) {
        this.message = message;
        this.statusCode = statusCode;
    }
}
