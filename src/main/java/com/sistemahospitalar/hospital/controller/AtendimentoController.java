package com.sistemahospitalar.hospital.controller;

import com.sistemahospitalar.hospital.dto.*;
import com.sistemahospitalar.hospital.model.*;
import com.sistemahospitalar.hospital.model.enums.StatusAtendimentoEnum;
import com.sistemahospitalar.hospital.service.AtendimentoService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/consultas")
public class AtendimentoController {
    private final AtendimentoService service;

    public AtendimentoController(AtendimentoService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<Atendimento> criar(@Valid @RequestBody CriarAtendimentoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(request));
    }

    @GetMapping
    public List<Atendimento> listar() { return service.listar(); }

    @GetMapping("/{id}")
    public Atendimento buscar(@PathVariable Long id) { return service.buscar(id); }

    @PatchMapping(value = "/{id}/status", consumes = MediaType.APPLICATION_JSON_VALUE, params = "!valor")
    public Atendimento status(@PathVariable Long id, @Valid @RequestBody AlterarStatusConsultaRequest request) {
        return service.alterarStatus(id, request.status());
    }

    @PatchMapping(value = "/{id}/status", params = "valor")
    public Atendimento statusPorParametro(@PathVariable Long id, @RequestParam StatusAtendimentoEnum valor) {
        return service.alterarStatus(id, valor);
    }

    @PatchMapping("/{id}/observacoes")
    public Atendimento observacoes(@PathVariable Long id, @Valid @RequestBody ObservacoesConsultaRequest request) {
        return service.registrarObservacoes(id, request.observacoes());
    }

    @PatchMapping("/{id}/cirurgia/iniciar")
    public ProcedimentoCirurgico iniciarCirurgia(@PathVariable Long id) { return service.iniciarCirurgia(id); }

    @PatchMapping("/{id}/cirurgia/concluir")
    public ProcedimentoCirurgico concluirCirurgia(@PathVariable Long id) { return service.concluirCirurgia(id); }

    @PatchMapping("/{id}/cirurgia/pos-operatorio")
    public ProcedimentoCirurgico posOperatorio(@PathVariable Long id,
            @Valid @RequestBody ObservacoesConsultaRequest request) {
        return service.observacoesPosOperatorias(id, request.observacoes());
    }

    @PatchMapping("/{id}/cirurgia/cancelar")
    public ProcedimentoCirurgico cancelarCirurgia(@PathVariable Long id) { return service.cancelarCirurgia(id); }

    @PatchMapping("/{id}/emergencia/iniciar")
    public AtendimentoEmergencia iniciarEmergencia(@PathVariable Long id) { return service.iniciarEmergencia(id); }

    @PatchMapping("/{id}/emergencia/concluir")
    public AtendimentoEmergencia concluirEmergencia(@PathVariable Long id) { return service.concluirEmergencia(id); }
}
