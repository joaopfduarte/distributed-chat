package br.distributed.system.chat.config;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(java.util.Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<?> handleIllegalState(IllegalStateException ex) {
        // Use 404 for not found scenarios like missing group; fallback to 409 for generic state issues if needed
        String msg = ex.getMessage() == null ? "resource not found" : ex.getMessage();
        int status = msg.toLowerCase().contains("não encontrado") || msg.toLowerCase().contains("nao encontrado")
                ? 404 : 409;
        return ResponseEntity.status(status).body(java.util.Map.of("error", msg));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> handleConflict(DataIntegrityViolationException ex) {
        return ResponseEntity.status(409).body(java.util.Map.of("error", "conflict"));
    }

    @ExceptionHandler(org.springframework.dao.EmptyResultDataAccessException.class)
    public ResponseEntity<?> handleNotFound(Exception ex) {
        return ResponseEntity.status(404).body(java.util.Map.of("error", "not found"));
    }
}
