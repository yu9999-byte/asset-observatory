package io.observatory;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import java.util.Map;
@RestControllerAdvice
class ApiErrors {
 @ExceptionHandler(IllegalArgumentException.class)ResponseEntity<?> bad(IllegalArgumentException e){return ResponseEntity.badRequest().body(Map.of("code","INVALID_INPUT","message",e.getMessage()));}
 @ExceptionHandler({MethodArgumentNotValidException.class,HttpMessageNotReadableException.class,org.springframework.web.bind.MissingServletRequestParameterException.class})ResponseEntity<?> validation(Exception e){return ResponseEntity.badRequest().body(Map.of("code","VALIDATION_FAILED","message","请求字段不完整或格式无效"));}
 @ExceptionHandler(ResponseStatusException.class)ResponseEntity<?> status(ResponseStatusException e){return ResponseEntity.status(e.getStatusCode()).body(Map.of("code","REQUEST_REJECTED","message",e.getReason()==null?"请求失败":e.getReason()));}
 @ExceptionHandler(org.springframework.dao.DataAccessException.class)ResponseEntity<?> database(Exception e){return ResponseEntity.status(503).body(Map.of("code","STORAGE_UNAVAILABLE","message","存储暂不可用，修改未确认保存"));}
}
