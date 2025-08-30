package com.marketplace.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Helper method to determine if request expects JSON response
    private boolean isRestRequest(HttpServletRequest request) {
        String acceptHeader = request.getHeader("Accept");
        String contentType = request.getHeader("Content-Type");
        String requestURI = request.getRequestURI();
        
        return (acceptHeader != null && acceptHeader.contains("application/json")) ||
               (contentType != null && contentType.contains("application/json")) ||
               requestURI.startsWith("/api/");
    }

    // ==================== BOOKING-RELATED EXCEPTIONS ====================
    
    @ExceptionHandler(BookingException.class)
    public Object handleBookingException(BookingException ex, HttpServletRequest request, 
                                       RedirectAttributes redirectAttributes) {
        logger.error("Booking exception: ", ex);
        
        if (isRestRequest(request)) {
            Map<String, Object> response = createErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        } else {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/client/bookings";
        }
    }

    @ExceptionHandler(SlotNotAvailableException.class)
    public Object handleSlotNotAvailable(SlotNotAvailableException ex, HttpServletRequest request, 
                                       RedirectAttributes redirectAttributes) {
        logger.warn("Slot not available: ", ex);
        
        if (isRestRequest(request)) {
            Map<String, Object> response = createErrorResponse(ex.getMessage(), HttpStatus.CONFLICT);
            return new ResponseEntity<>(response, HttpStatus.CONFLICT);
        } else {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/client/bookings";
        }
    }

    @ExceptionHandler(ProfessionalNotFoundException.class)
    public Object handleProfessionalNotFound(ProfessionalNotFoundException ex, HttpServletRequest request, 
                                           RedirectAttributes redirectAttributes) {
        logger.warn("Professional not found: ", ex);
        
        if (isRestRequest(request)) {
            Map<String, Object> response = createErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND);
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        } else {
            redirectAttributes.addFlashAttribute("error", "Professional not found");
            return "redirect:/client/search";
        }
    }

    @ExceptionHandler(AvailabilityNotFoundException.class)
    public Object handleAvailabilityNotFound(AvailabilityNotFoundException ex, HttpServletRequest request, 
                                           RedirectAttributes redirectAttributes) {
        logger.warn("Availability not found: ", ex);
        
        if (isRestRequest(request)) {
            Map<String, Object> response = createErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND);
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        } else {
            redirectAttributes.addFlashAttribute("error", "The selected time slot is no longer available");
            return "redirect:/client/bookings";
        }
    }

    @ExceptionHandler(UnauthorizedAccessException.class)
    public Object handleUnauthorizedAccess(UnauthorizedAccessException ex, HttpServletRequest request) {
        logger.warn("Unauthorized access attempt: ", ex);
        
        if (isRestRequest(request)) {
            Map<String, Object> response = createErrorResponse("Access denied", HttpStatus.FORBIDDEN);
            return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
        } else {
            return "redirect:/login";
        }
    }

    // ==================== EXISTING USER-RELATED EXCEPTIONS ====================
    
    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleUserAlreadyExists(UserAlreadyExistsException ex) {
        logger.warn("User already exists: ", ex);
        Map<String, Object> response = createErrorResponse(ex.getMessage(), HttpStatus.CONFLICT);
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(UsernameTakenException.class)
    public ResponseEntity<Map<String, Object>> handleUsernameTaken(UsernameTakenException ex) {
        logger.warn("Username taken: ", ex);
        Map<String, Object> response = createErrorResponse(ex.getMessage(), HttpStatus.CONFLICT);
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    // ==================== VALIDATION EXCEPTIONS ====================
    
    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public Object handleValidationException(org.springframework.web.bind.MethodArgumentNotValidException ex, 
                                          HttpServletRequest request, RedirectAttributes redirectAttributes) {
        logger.warn("Validation error: ", ex);
        
        StringBuilder errorMessage = new StringBuilder("Validation failed: ");
        ex.getBindingResult().getFieldErrors().forEach(error -> 
            errorMessage.append(error.getField()).append(" ").append(error.getDefaultMessage()).append("; "));
        
        if (isRestRequest(request)) {
            Map<String, Object> response = createErrorResponse(errorMessage.toString(), HttpStatus.BAD_REQUEST);
            
            // Add field-specific errors for API responses
            Map<String, String> fieldErrors = new HashMap<>();
            ex.getBindingResult().getFieldErrors().forEach(error -> 
                fieldErrors.put(error.getField(), error.getDefaultMessage()));
            response.put("fieldErrors", fieldErrors);
            
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        } else {
            redirectAttributes.addFlashAttribute("error", errorMessage.toString());
            return "redirect:/client/bookings";
        }
    }

    // ==================== SECURITY EXCEPTIONS ====================
    
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public Object handleAccessDenied(org.springframework.security.access.AccessDeniedException ex, 
                                   HttpServletRequest request) {
        logger.warn("Access denied: ", ex);
        
        if (isRestRequest(request)) {
            Map<String, Object> response = createErrorResponse("Access denied", HttpStatus.FORBIDDEN);
            return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
        } else {
            return "redirect:/access-denied";
        }
    }

    @ExceptionHandler(org.springframework.security.authentication.AuthenticationCredentialsNotFoundException.class)
    public Object handleAuthenticationRequired(
            org.springframework.security.authentication.AuthenticationCredentialsNotFoundException ex, 
            HttpServletRequest request) {
        logger.warn("Authentication required: ", ex);
        
        if (isRestRequest(request)) {
            Map<String, Object> response = createErrorResponse("Authentication required", HttpStatus.UNAUTHORIZED);
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        } else {
            return "redirect:/login";
        }
    }

    // ==================== GENERIC EXCEPTION HANDLER ====================
    
    @ExceptionHandler(Exception.class)
    public Object handleGenericException(Exception ex, HttpServletRequest request, 
                                       RedirectAttributes redirectAttributes) {
        logger.error("Unexpected error occurred: ", ex);
        
        if (isRestRequest(request)) {
            Map<String, Object> response = createErrorResponse("Internal server error", HttpStatus.INTERNAL_SERVER_ERROR);
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        } else {
            redirectAttributes.addFlashAttribute("error", 
                "An unexpected error occurred. Please try again or contact support if the problem persists.");
            return "redirect:/error";
        }
    }

    // ==================== HELPER METHODS ====================
    
    private Map<String, Object> createErrorResponse(String message, HttpStatus status) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("message", message);
        response.put("status", status.value());
        response.put("error", status.getReasonPhrase());
        return response;
    }
}