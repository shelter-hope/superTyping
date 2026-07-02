package com.typinggame.common.exception;

import com.typinggame.vo.ResultMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ResultMessage<Object>> handleBusinessException(BusinessException e) {
        return ResponseEntity.status(e.getCode())
                .body(ResultMessage.error(e.getCode(), e.getMessage(), null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResultMessage<Map<String, String>>> handleValidationException(MethodArgumentNotValidException e) {
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return ResponseEntity.badRequest()
                .body(ResultMessage.error(400, "参数校验失败", errors));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ResultMessage<Object>> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        return ResponseEntity.badRequest()
                .body(ResultMessage.error(400, "上传文件大小超过限制", null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResultMessage<Object>> handleGenericException(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResultMessage.error(500, "服务器内部错误", null));
    }
}