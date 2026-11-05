package com.sistemahospitalar.hospital.controller;

import com.sistemahospitalar.hospital.model.Atendimento;
import com.sistemahospitalar.hospital.service.AtendimentoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/atendimentos")
public class AtendimentoController {

    @Autowired
    private AtendimentoService service;

    @PostMapping
    public ResponseEntity<Atendimento> agendar(@RequestBody Atendimento atendimento) {
        return ResponseEntity.ok(service.agendar(atendimento));
    }

    @GetMapping
    public ResponseEntity<List<Atendimento>> listarTodos() {
        return ResponseEntity.ok(service.listarTodos());
    }

    @GetMapping("/status-db")
    public ResponseEntity<String> statusDatabase() {
        return ResponseEntity.ok("Banco H2 ativo e operacional com sucesso na Sprint 2!");
    }
}