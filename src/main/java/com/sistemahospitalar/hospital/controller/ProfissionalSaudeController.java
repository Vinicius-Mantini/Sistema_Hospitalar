package com.sistemahospitalar.hospital.controller;

import com.sistemahospitalar.hospital.dto.CriarEncaminhamentoRequest;
import com.sistemahospitalar.hospital.model.*;
import com.sistemahospitalar.hospital.service.ProfissionalSaudeService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/profissionais")
public class ProfissionalSaudeController {
    private final ProfissionalSaudeService service;
    public ProfissionalSaudeController(ProfissionalSaudeService service) { this.service = service; }

    @GetMapping
    public List<ProfissionalSaude> listar() { return service.listar(); }

    @GetMapping("/especialistas")
    public List<MedicoEspecialista> porEspecialidade(@RequestParam String especialidade) {
        return service.porEspecialidade(especialidade);
    }

    @GetMapping("/{id}")
    public ProfissionalSaude buscar(@PathVariable Long id) { return service.buscar(id); }

    @PostMapping
    public ResponseEntity<ProfissionalSaude> criar(@Valid @RequestBody ProfissionalSaude profissional) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.salvar(profissional));
    }

    @PostMapping("/{id}/encaminhamentos")
    public ResponseEntity<Encaminhamento> encaminhar(@PathVariable Long id,
            @Valid @RequestBody CriarEncaminhamentoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.encaminhar(id, request));
    }

    @GetMapping("/{id}/encaminhamentos")
    public List<Encaminhamento> encaminhamentos(@PathVariable Long id) {
        return service.listarEncaminhamentos(id);
    }

    @GetMapping("/{id}/procedimentos")
    public List<ProcedimentoCirurgico> procedimentos(@PathVariable Long id) {
        return service.cirurgiasDoProfissional(id);
    }
}
