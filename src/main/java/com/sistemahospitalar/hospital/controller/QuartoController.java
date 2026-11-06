package com.sistemahospitalar.hospital.controller;

import com.sistemahospitalar.hospital.model.Quarto;
import com.sistemahospitalar.hospital.model.enums.TipoQuartoEnum;
import com.sistemahospitalar.hospital.service.QuartoService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/quartos")
public class QuartoController {
    private final QuartoService service;
    public QuartoController(QuartoService service) { this.service = service; }

    @GetMapping
    public List<Quarto> listar(@RequestParam(required = false) TipoQuartoEnum tipo) {
        return service.listar(tipo);
    }

    @GetMapping("/{id}")
    public Quarto buscar(@PathVariable Long id) { return service.buscar(id); }

    @PostMapping
    public ResponseEntity<Quarto> criar(@Valid @RequestBody Quarto quarto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.salvar(quarto));
    }

    @PatchMapping("/{id}/manutencao")
    public Quarto manutencao(@PathVariable Long id) { return service.colocarEmManutencao(id); }

    @PatchMapping("/{id}/reativar")
    public Quarto reativar(@PathVariable Long id) { return service.reativar(id); }
}
