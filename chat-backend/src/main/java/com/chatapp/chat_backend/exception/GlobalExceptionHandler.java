package com.chatapp.chat_backend.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.reactive.resource.NoResourceFoundException;
import org.springframework.web.server.ServerWebExchange;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException ex,
            ServerWebExchange exchange
    ) {
        HttpStatus status = resolveStatus(ex.getErrorCode());
        ErrorResponse response = buildResponse(
                status,
                ex.getErrorCode(),
                ex.getMessage(),
                exchange
        );
        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(
            NoResourceFoundException ex,
            ServerWebExchange request) {

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(buildResponse(
                        HttpStatus.NOT_FOUND,
                        ErrorCode.ENDPOINT_NOT_FOUND,
                        "The requested endpoint does not exist",
                        request
                ));
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            WebExchangeBindException ex,
            ServerWebExchange exchange
    ) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .findFirst()
                .orElse("Validation failed");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(buildResponse(
                HttpStatus.BAD_REQUEST,
                ErrorCode.VALIDATION_ERROR,
                message,
                exchange
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(
            Exception ex,
            ServerWebExchange exchange
    ) {
        log.error("Unexpected error occurred", ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ErrorCode.INTERNAL_ERROR,
                "An unexpected error occurred",
                exchange
        ));
    }

    private HttpStatus resolveStatus(ErrorCode code) {
        return switch (code) {
            case EMAIL_ALREADY_REGISTERED, USERNAME_ALREADY_REGISTERED, PHONE_ALREADY_REGISTERED,
                 DIRECT_CHAT_ALREADY_EXISTS, MESSAGE_REQUEST_ALREADY_EXISTS, CONTACT_ALREADY_EXISTS,
                 INVITATION_ALREADY_EXISTS, ALREADY_A_MEMBER -> HttpStatus.CONFLICT;
            case USER_NOT_FOUND, ENDPOINT_NOT_FOUND, CHAT_ROOM_NOT_FOUND, MESSAGE_NOT_FOUND,
                 MESSAGE_REQUEST_NOT_FOUND, CONTACT_NOT_FOUND, INVITATION_NOT_FOUND, REPORT_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case UNAUTHENTICATED, INVALID_CREDENTIALS, INVALID_REFRESH_TOKEN, EXPIRED_REFRESH_TOKEN,
                 TOKEN_REUSE_DETECTED -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN, NOT_A_CHAT_MEMBER, INSUFFICIENT_PERMISSIONS, CANNOT_EDIT_OTHERS_MESSAGE,
                 CANNOT_DELETE_OTHERS_MESSAGE, USER_BLOCKED, PRIVACY_RESTRICTION -> HttpStatus.FORBIDDEN;
            default -> HttpStatus.BAD_REQUEST;
        };
    }

    private ErrorResponse buildResponse(
            HttpStatus status,
            ErrorCode code,
            String message,
            ServerWebExchange exchange
    ) {
        return new ErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                code.name(),
                message,
                exchange.getRequest().getPath().value()
        );
    }
}
