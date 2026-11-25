
package com.eventpro.service;

import com.eventpro.advice.GlobalExceptionHandler;
import com.eventpro.dto.ErrorResponse;
import com.eventpro.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.validation.ValidationException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();

        request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/events");
    }

    private ErrorResponse body(ResponseEntity<ErrorResponse> response) {
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getTimestamp());
        assertEquals("/api/events", response.getBody().getPath());
        return response.getBody();
    }

    @Test
    void handleValidation_shouldReturnBadRequest() {
        ValidationException ex = mock(ValidationException.class);
        when(ex.getMessage()).thenReturn("Validation failed");

        ResponseEntity<ErrorResponse> response =
                handler.handleValidation(ex, request);

        ErrorResponse error = body(response);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, error.getStatus());
        assertEquals("Validation failed", error.getMessage());
        assertEquals("Bad Request", error.getError());
        assertNull(error.getFieldErrors());
    }

    @Test
    void handleBeanValidation_shouldReturnFieldErrors() {
        MethodArgumentNotValidException ex =
                mock(MethodArgumentNotValidException.class);

        BindingResult bindingResult = mock(BindingResult.class);

        when(ex.getBindingResult()).thenReturn(bindingResult);

        when(bindingResult.getFieldErrors()).thenReturn(List.of(
                new FieldError("event", "name", "Name is required"),
                new FieldError("event", "date", "Date is invalid"),
                new FieldError("event", "name", "Another name error")
        ));

        ResponseEntity<ErrorResponse> response =
                handler.handleBeanValidation(ex, request);

        ErrorResponse error = body(response);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Donnees invalides", error.getMessage());

        assertNotNull(error.getFieldErrors());
        assertEquals(2, error.getFieldErrors().size());
        assertEquals("Name is required", error.getFieldErrors().get("name"));
        assertEquals("Date is invalid", error.getFieldErrors().get("date"));
    }

    @Test
    void handleConstraintViolation_shouldReturnFieldErrors() {
        ConstraintViolationException ex =
                mock(ConstraintViolationException.class);

        @SuppressWarnings("unchecked")
        ConstraintViolation<Object> violation =
                mock(ConstraintViolation.class);

        Path path = mock(Path.class);

        when(path.toString()).thenReturn("createEvent.name");
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("Name must not be blank");

        when(ex.getConstraintViolations()).thenReturn(Set.of(violation));

        ResponseEntity<ErrorResponse> response =
                handler.handleConstraintViolation(ex, request);

        ErrorResponse error = body(response);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Donnees invalides", error.getMessage());

        assertNotNull(error.getFieldErrors());
        assertEquals(
                "Name must not be blank",
                error.getFieldErrors().get("createEvent.name")
        );
    }

    @Test
    void handleMalformed_shouldReturnBadRequest() {
        HttpMessageNotReadableException ex =
                mock(HttpMessageNotReadableException.class);

        ResponseEntity<ErrorResponse> response =
                handler.handleMalformed(ex, request);

        ErrorResponse error = body(response);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Requete mal formee", error.getMessage());
        assertNull(error.getFieldErrors());
    }

    @Test
    void handleMalformed_shouldHandleTypeMismatch() {
        MethodArgumentTypeMismatchException ex =
                mock(MethodArgumentTypeMismatchException.class);

        ResponseEntity<ErrorResponse> response =
                handler.handleMalformed(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Requete mal formee", body(response).getMessage());
    }

    @Test
    void handleNotFound_shouldReturnNotFound() {
        ResourceNotFoundException ex =
                mock(ResourceNotFoundException.class);

        when(ex.getMessage()).thenReturn("Event not found");

        ResponseEntity<ErrorResponse> response =
                handler.handleNotFound(ex, request);

        ErrorResponse error = body(response);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, error.getStatus());
        assertEquals("Event not found", error.getMessage());
    }

    @Test
    void handleConflict_shouldReturnConflict() {
        InsufficientStockException ex =
                mock(InsufficientStockException.class);

        when(ex.getMessage()).thenReturn("Insufficient stock");

        ResponseEntity<ErrorResponse> response =
                handler.handleConflict(ex, request);

        ErrorResponse error = body(response);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(409, error.getStatus());
        assertEquals("Insufficient stock", error.getMessage());
    }

    @Test
    void handleResponseStatus_shouldReturnOriginalStatusAndReason() {
        ResponseStatusException ex =
                new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Access denied"
                );

        ResponseEntity<ErrorResponse> response =
                handler.handleResponseStatus(ex, request);

        ErrorResponse error = body(response);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(403, error.getStatus());
        assertEquals("Access denied", error.getMessage());
    }

    @Test
    void handleSystem_shouldReturnGenericInternalServerError() {
        SystemException ex = mock(SystemException.class);

        ResponseEntity<ErrorResponse> response =
                handler.handleSystem(ex, request);

        ErrorResponse error = body(response);

        assertEquals(
                HttpStatus.INTERNAL_SERVER_ERROR,
                response.getStatusCode()
        );

        assertEquals(500, error.getStatus());
        assertEquals(
                "Une erreur interne est survenue",
                error.getMessage()
        );
    }

    @Test
    void handleUnexpected_shouldReturnGenericInternalServerError() {
        Exception ex = new RuntimeException("Unexpected error");

        ResponseEntity<ErrorResponse> response =
                handler.handleUnexpected(ex, request);

        ErrorResponse error = body(response);

        assertEquals(
                HttpStatus.INTERNAL_SERVER_ERROR,
                response.getStatusCode()
        );

        assertEquals(500, error.getStatus());
        assertEquals(
                "Une erreur interne est survenue",
                error.getMessage()
        );
    }
}
