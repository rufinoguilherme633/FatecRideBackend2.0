package com.example.fatecCarCarona.infra;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

import com.example.fatecCarCarona.dto.ExceptionalDTO;

@ControllerAdvice
public class ControlExceptionalHeandler {


		/*@ExceptionHandler(Exception.class)
		public ResponseEntity theatGeneralExceptional(Exception exception) {
			ExceptionalDTO exceptionalDTO = new ExceptionalDTO(exception.getMessage(), "500");
			return ResponseEntity.internalServerError().body(exceptionalDTO);
		}*/
	
	 @ExceptionHandler(ResponseStatusException.class)
	    public ResponseEntity<?> handleResponseStatusException(ResponseStatusException ex) {
	        return ResponseEntity
	                .status(ex.getStatusCode())
	                .body(Map.of(
	                        "status", ex.getStatusCode().value(),
	                        "message", ex.getReason()
	                ));
	    }

	    @ExceptionHandler(Exception.class)
	    public ResponseEntity<?> handleGenericException(Exception ex) {
	        return ResponseEntity
	                .status(500)
	                .body(Map.of(
	                        "status", 500,
	                        "message", "Erro interno do servidor"
	                ));
	    }
	}

