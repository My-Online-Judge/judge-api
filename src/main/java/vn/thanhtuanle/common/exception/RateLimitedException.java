package vn.thanhtuanle.common.exception;

import lombok.Getter;

/** An AppException that knows how long the caller must wait — mapped to a dynamic Retry-After. */
@Getter
public class RateLimitedException extends AppException {

    private final long retryAfterSeconds;

    public RateLimitedException(ErrorCode errorCode, long retryAfterSeconds) {
        super(errorCode);
        this.retryAfterSeconds = retryAfterSeconds;
    }
}
