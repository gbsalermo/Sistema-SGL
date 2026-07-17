package com.sgl.exception;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.persistence.EntityNotFoundException;

@RestControllerAdvice
public class RestExceptionHandler {
	
	@ExceptionHandler(EntityNotFoundException.class)
	public ResponseEntity<Map<String, Object>> handleNotFound(EntityNotFoundException ex){
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
				"timestamp", LocalDateTime.now().toString(),
				"status", 404,
				"erro", ex.getMessage()
				));
	}

}
