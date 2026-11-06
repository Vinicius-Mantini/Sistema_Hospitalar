package com.sistemahospitalar.hospital.controller;

import com.sistemahospitalar.hospital.dto.CriarInternacaoRequest;
import com.sistemahospitalar.hospital.model.Internacao;
import com.sistemahospitalar.hospital.service.InternacaoService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/internacoes")
public class InternacaoController {
    private final InternacaoService service;
    public InternacaoController(InternacaoService service) { this.service = service; }

    @GetMapping
    public List<Internacao> listar() { return service.listar(); }

    @GetMapping("/{id}")
    public Internacao buscar(@PathVariable Long id) { return service.buscar(id); }

    @PostMapping
    public ResponseEntity<Internacao> criar(@Valid @RequestBody CriarInternacaoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(request));
    }

    @PatchMapping("/{id}/alta")
    public Internacao registrarAlta(@PathVariable Long id) { return service.registrarAlta(id); }

    @PatchMapping("/{id}/cancelar")
    public Internacao cancelar(@PathVariable Long id) { return service.cancelar(id); }
}
