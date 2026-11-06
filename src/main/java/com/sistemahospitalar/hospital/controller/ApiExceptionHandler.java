package com.sistemahospitalar.hospital.controller;

import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, Object>> naoEncontrado(NoSuchElementException erro) {
        return responder(HttpStatus.NOT_FOUND, erro.getMessage());
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<Map<String, Object>> regra(RuntimeException erro) {
        return responder(HttpStatus.UNPROCESSABLE_ENTITY, erro.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validacao(MethodArgumentNotValidException erro) {
        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("erro", "Dados inválidos");
        resposta.put("campos", erro.getBindingResult().getFieldErrors().stream()
            .collect(Collectors.toMap(item -> item.getField(),
                item -> item.getDefaultMessage() == null ? "Inválido" : item.getDefaultMessage(),
                (primeiro, segundo) -> primeiro, LinkedHashMap::new)));
        return ResponseEntity.badRequest().body(resposta);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> formato(HttpMessageNotReadableException erro) {
        return responder(HttpStatus.BAD_REQUEST, "JSON inválido ou valor de enum desconhecido.");
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> restricao(ConstraintViolationException erro) {
        return responder(HttpStatus.BAD_REQUEST, "Violação de validação: " + erro.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> banco(DataIntegrityViolationException erro) {
        return responder(HttpStatus.CONFLICT, "Registro duplicado ou restrição de integridade violada.");
    }

    private ResponseEntity<Map<String, Object>> responder(HttpStatus status, String mensagem) {
        return ResponseEntity.status(status).body(Map.of("erro", mensagem));
    }
}
