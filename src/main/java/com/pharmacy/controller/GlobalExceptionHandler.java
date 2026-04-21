package com.pharmacy.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Global exception handler for the application.
 * Catches exceptions from all controllers and returns user-friendly error messages.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public String handleIllegalArgument(IllegalArgumentException ex, RedirectAttributes redirectAttributes, HttpServletRequest request) {
        logger.warn("Illegal argument: {}", ex.getMessage());
        redirectAttributes.addFlashAttribute("errorMessage", "Invalid input: " + ex.getMessage());
        return resolveRedirectTarget(request);
    }

    @ExceptionHandler(IllegalStateException.class)
    public String handleIllegalState(IllegalStateException ex, RedirectAttributes redirectAttributes, HttpServletRequest request) {
        logger.warn("Illegal state: {}", ex.getMessage());
        redirectAttributes.addFlashAttribute("errorMessage", "Operation failed: " + ex.getMessage());
        return resolveRedirectTarget(request);
    }

    @ExceptionHandler(Exception.class)
    public String handleGenericException(Exception ex, RedirectAttributes redirectAttributes, HttpServletRequest request) {
        logger.error("Unexpected error occurred", ex);
        redirectAttributes.addFlashAttribute("errorMessage", "An unexpected error occurred. Please try again.");
        return resolveRedirectTarget(request);
    }

    private String resolveRedirectTarget(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isBlank()) {
            int idx = referer.indexOf("//");
            String path = referer;
            if (idx > -1) {
                int slashAfterHost = referer.indexOf('/', idx + 2);
                path = slashAfterHost > -1 ? referer.substring(slashAfterHost) : "/";
            }
            return "redirect:" + path;
        }
        return "redirect:/";
    }
}
