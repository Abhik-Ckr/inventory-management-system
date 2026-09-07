package com.inventory.system.exception;

import com.inventory.system.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps every domain exception to the HTTP status it represents in our
 * design: 404 for "not found", 409 for conflicts (duplicate SKU, concurrent
 * update), 422 for a business rule violation that isn't a conflict
 * (insufficient stock, selling an inactive product, exceeding the stock
 * cap), 400 for malformed input (bean-validation failures, duplicate sale
 * line items), and 500 for anything that is genuinely an unexpected
 * server-side failure.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({ProductNotFoundException.class, SaleNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleNotFound(RuntimeException ex, HttpServletRequest request) {
        ErrorResponse body = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(), "Not Found", ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler({DuplicateSkuException.class, OptimisticLockingFailureException.class})
    public ResponseEntity<ErrorResponse> handleConflict(RuntimeException ex, HttpServletRequest request) {
        String message = ex instanceof OptimisticLockingFailureException
                ? "This record was modified by another request. Please retry."
                : ex.getMessage();
        ErrorResponse body = new ErrorResponse(
                HttpStatus.CONFLICT.value(), "Conflict", message, request.getRequestURI());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler({InsufficientStockException.class, InactiveProductException.class, StockLimitExceededException.class})
    public ResponseEntity<ErrorResponse> handleBusinessRule(RuntimeException ex, HttpServletRequest request) {
        ErrorResponse body = new ErrorResponse(
                HttpStatus.UNPROCESSABLE_ENTITY.value(), "Unprocessable Entity", ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(body);
    }

    @ExceptionHandler({DuplicateSaleItemException.class, ConstraintViolationException.class})
    public ResponseEntity<ErrorResponse> handleBadRequest(RuntimeException ex, HttpServletRequest request) {
        ErrorResponse body = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(), "Bad Request", ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * A required query parameter is missing, or one can't be converted to the
     * declared type (e.g. a non-ISO date on the sales report). Both are
     * malformed input, so we surface them as 400 rather than letting them fall
     * through to the generic 500 handler.
     */
    @ExceptionHandler({MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorResponse> handleBadRequestParams(Exception ex, HttpServletRequest request) {
        String message = ex instanceof MissingServletRequestParameterException missing
                ? "Required parameter '" + missing.getParameterName() + "' is missing"
                : "Parameter has an invalid value: " + ex.getMessage();
        ErrorResponse body = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(), "Bad Request", message, request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(fe ->
                fieldErrors.put(fe.getField(), fe.getDefaultMessage()));
        ErrorResponse body = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(), "Bad Request", "Validation failed", request.getRequestURI(), fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        ErrorResponse body = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal Server Error",
                "Something went wrong. Please try again later.", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
