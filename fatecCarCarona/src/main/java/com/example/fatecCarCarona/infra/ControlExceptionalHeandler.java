package com.example.fatecCarCarona.infra;

import java.util.Map;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

@ControllerAdvice
public class ControlExceptionalHeandler {


		/*@ExceptionHandler(Exception.class)
		public ResponseEntity theatGeneralExceptional(Exception exception) {
			ExceptionalDTO exceptionalDTO = new ExceptionalDTO(exception.getMessage(), "500");
			return ResponseEntity.internalServerError().body(exceptionalDTO);
		}*/
	
	 @ExceptionHandler(ResponseStatusException.class)
	    public ResponseEntity<?> handleResponseStatusException(ResponseStatusException ex, HttpServletRequest request) {
	        if (isSseRequest(request)) {
	            return ResponseEntity.status(ex.getStatusCode()).build();
	        }
	        return ResponseEntity
	                .status(ex.getStatusCode())
	                .body(Map.of(
	                        "status", ex.getStatusCode().value(),
	                        "message", Optional.ofNullable(ex.getReason()).orElse("Erro interno do servidor")
	                ));
	    }

	    @ExceptionHandler(Exception.class)
	    public ResponseEntity<?> handleGenericException(Exception ex, HttpServletRequest request) {
	        if (isSseRequest(request)) {
	            return ResponseEntity.status(500).build();
	        }
	        return ResponseEntity
	                .status(500)
	                .body(Map.of(
	                        "status", 500,
	                        "message", "Erro interno do servidor"
	                ));
	    }

	    private boolean isSseRequest(HttpServletRequest request) {
	        if (request == null) {
	            return false;
	        }
	        String accept = request.getHeader(HttpHeaders.ACCEPT);
	        String contentType = request.getContentType();
	        return containsTextEventStream(accept) || containsTextEventStream(contentType);
	    }

	    private boolean containsTextEventStream(String value) {
	        return Optional.ofNullable(value)
	                .map(v -> v.toLowerCase().contains(MediaType.TEXT_EVENT_STREAM_VALUE))
	                .orElse(false);
	    }
	}

