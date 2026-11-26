package com.neo4flix.recommendationservice.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail handleResponseStatus(
            ResponseStatusException ex,
            HttpServletRequest request) {

        HttpStatusCode status = ex.getStatusCode();

        String detail = ex.getReason() != null
                ? ex.getReason()
                : "Request could not be processed";

        return createProblem(status, detail, request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        String detail = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("Request validation failed");

        return createProblem(HttpStatus.BAD_REQUEST, detail, request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {

        String detail = "Invalid value for parameter: " + ex.getName();

        return createProblem(HttpStatus.BAD_REQUEST, detail, request);
    }

    @ExceptionHandler({
            IllegalArgumentException.class,
            ConstraintViolationException.class,
            HttpMessageNotReadableException.class,
            MissingRequestHeaderException.class,
            MissingServletRequestParameterException.class
    })
    public ProblemDetail handleBadRequest(
            Exception ex,
            HttpServletRequest request) {

        String detail;

        if (ex instanceof MissingRequestHeaderException missing) {
            detail = "Required header is missing: " + missing.getHeaderName();
        } else if (ex instanceof MissingServletRequestParameterException missing) {
            detail = "Required parameter is missing: " + missing.getParameterName();
        } else if (ex instanceof HttpMessageNotReadableException) {
            detail = "Request body is missing or malformed";
        } else if (ex instanceof ConstraintViolationException) {
            detail = "Request validation failed";
        } else {
            detail = ex.getMessage() != null
                    ? ex.getMessage()
                    : "Invalid request";
        }

        return createProblem(HttpStatus.BAD_REQUEST, detail, request);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(
            Exception ex,
            HttpServletRequest request) {

        log.error("Unexpected recommendation-service error", ex);

        return createProblem(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred",
                request);
    }

    private ProblemDetail createProblem(
            HttpStatusCode status,
            String detail,
            HttpServletRequest request) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);

        problem.setTitle(resolveTitle(status));
        problem.setProperty("path", request.getRequestURI());

        return problem;
    }

    private String resolveTitle(HttpStatusCode status) {
        if (status.value() == 400) return "Invalid Request";
        if (status.value() == 401) return "Unauthorized";
        if (status.value() == 403) return "Forbidden";
        if (status.value() == 404) return "Not Found";
        if (status.value() == 409) return "Conflict";
        if (status.value() == 500) return "Internal Server Error";
        if (status.value() == 503) return "Service Unavailable";

        return "Request Error";
    }
}
