package com.boardflow.web;
import org.springframework.dao.OptimisticLockingFailureException;
import jakarta.persistence.OptimisticLockException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import java.util.*;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(NoSuchElementException.class)
    ResponseEntity<Map<String,String>> missing(NoSuchElementException ex) { return response(404,ex.getMessage()); }
    @ExceptionHandler({OptimisticLockingFailureException.class, OptimisticLockException.class})
    ResponseEntity<Map<String,String>> conflict(Exception ex) { return response(409,"This storyboard changed in another tab. Export your work, then reload before saving."); }
    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Map<String,String>> invalid(IllegalArgumentException ex) { return response(400,ex.getMessage()); }
    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    ResponseEntity<Map<String,String>> validation(Exception ex) { return response(400,"Check your project fields: titles are required and shot durations must be 1-3600 seconds."); }
    private ResponseEntity<Map<String,String>> response(int status, String message) { return ResponseEntity.status(status).body(Map.of("message",message)); }
}
