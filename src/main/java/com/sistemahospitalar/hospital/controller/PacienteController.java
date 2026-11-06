package com.sistemahospitalar.hospital.controller;

import com.sistemahospitalar.hospital.dto.HistoricoPacienteResponse;
import com.sistemahospitalar.hospital.model.Paciente;
import com.sistemahospitalar.hospital.service.HistoricoMedicoService;
import com.sistemahospitalar.hospital.service.PacienteService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/pacientes")
public class PacienteController {
    private final PacienteService service;
    private final HistoricoMedicoService historicos;

    public PacienteController(PacienteService service, HistoricoMedicoService historicos) {
        this.service = service;
        this.historicos = historicos;
    }

    @GetMapping
    public List<Paciente> listar() { return service.listar(); }

    @GetMapping("/{id}")
    public Paciente buscar(@PathVariable Long id) { return service.buscar(id); }

    @PostMapping
    public ResponseEntity<Paciente> criar(@Valid @RequestBody Paciente paciente) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.salvar(paciente));
    }

    @GetMapping("/{id}/historico")
    public HistoricoPacienteResponse historico(@PathVariable Long id) {
        service.buscar(id);
        return historicos.consultar(id);
    }
}
