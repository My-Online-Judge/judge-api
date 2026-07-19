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
    ROLE_EXISTED("Role name already exists", HttpStatus.CONFLICT),
    ROLE_PROTECTED("This system role cannot be deleted", HttpStatus.CONFLICT),
    ROLE_IN_USE("This role is still assigned to one or more users", HttpStatus.CONFLICT),
    ROLE_NAME_INVALID("Role name must be uppercase letters, digits, or underscore and start with a letter", HttpStatus.BAD_REQUEST),
    USER_NOT_FOUND("User not found", HttpStatus.NOT_FOUND),
    USERNAME_EXISTED("Username already exists", HttpStatus.CONFLICT),
    EMAIL_EXISTED("Email already exists", HttpStatus.CONFLICT),
    USER_PROTECTED("This is a protected system account and cannot be modified", HttpStatus.CONFLICT),
    ROLE_NOT_ASSIGNABLE("This role cannot be assigned to a user", HttpStatus.CONFLICT),
    USER_SELF_MODIFY("You cannot change your own roles, status, or delete your own account", HttpStatus.CONFLICT),
    USERNAME_INVALID("Username must be 3-32 characters: letters, digits, or underscore", HttpStatus.BAD_REQUEST),
    PASSWORD_INVALID("Password must be at least 8 characters and include letters and digits", HttpStatus.BAD_REQUEST),
    EMAIL_INVALID("Email is not valid", HttpStatus.BAD_REQUEST),
    USER_STATUS_INVALID("Status must be ACTIVE or DISABLED", HttpStatus.BAD_REQUEST),
    UNCATEGORIZED_EXCEPTION("Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String message;
    private final HttpStatus statusCode;

    ErrorCode(String message, HttpStatus statusCode) {
        this.message = message;
        this.statusCode = statusCode;
    }
}
