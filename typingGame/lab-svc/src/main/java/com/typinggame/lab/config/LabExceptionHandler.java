package com.typinggame.lab.config;

import com.typinggame.vo.ResultMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class LabExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ResultMessage<Void>> badRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ResultMessage.error(400, ex.getMessage()));
    }

    @ExceptionHandler(LabNotFoundException.class)
    public ResponseEntity<ResultMessage<Void>> missing(LabNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ResultMessage.error(404, ex.getMessage()));
    }
}
