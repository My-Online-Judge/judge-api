package vn.thanhtuanle.common.exception;

import org.springframework.web.bind.annotation.RestControllerAdvice;
import vn.thanhtuanle.oj.common.web.error.OjExceptionHandler;

/**
 * The submission side answers errors with the handlers every OJ service shares — the submission cooldown's
 * dynamic Retry-After included (RateLimitedException); it adds none of its own.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends OjExceptionHandler {
}
