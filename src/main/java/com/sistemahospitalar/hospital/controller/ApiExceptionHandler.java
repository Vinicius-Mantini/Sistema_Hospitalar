package com.sistemahospitalar.hospital.controller;

import com.sistemahospitalar.hospital.exception.AtendimentoIndisponivelException;
import com.sistemahospitalar.hospital.exception.InternacaoAtivaException;
import com.sistemahospitalar.hospital.exception.ProfissionalIncompativelException;
import com.sistemahospitalar.hospital.exception.QuartoSemVagasException;
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
        return responder(HttpStatus.UNPROCESSABLE_CONTENT, erro.getMessage());
    }

    @ExceptionHandler(AtendimentoIndisponivelException.class)
    public ResponseEntity<Map<String, Object>> atendimentoIndisponivel(AtendimentoIndisponivelException erro) {
        return responderRegra("ATENDIMENTO_INDISPONIVEL", erro);
    }

    @ExceptionHandler(ProfissionalIncompativelException.class)
    public ResponseEntity<Map<String, Object>> profissionalIncompativel(ProfissionalIncompativelException erro) {
        return responderRegra("PROFISSIONAL_INCOMPATIVEL", erro);
    }

    @ExceptionHandler(InternacaoAtivaException.class)
    public ResponseEntity<Map<String, Object>> internacaoAtiva(InternacaoAtivaException erro) {
        return responderRegra("INTERNACAO_ATIVA", erro);
    }

    @ExceptionHandler(QuartoSemVagasException.class)
    public ResponseEntity<Map<String, Object>> quartoSemVagas(QuartoSemVagasException erro) {
        return responderRegra("QUARTO_SEM_VAGAS", erro);
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

    private ResponseEntity<Map<String, Object>> responderRegra(String codigo, RuntimeException erro) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT)
            .body(Map.of("erro", erro.getMessage(), "codigo", codigo));
    }
}
