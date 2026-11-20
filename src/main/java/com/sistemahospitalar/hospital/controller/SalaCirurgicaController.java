package com.sistemahospitalar.hospital.controller;

import com.sistemahospitalar.hospital.model.SalaCirurgica;
import com.sistemahospitalar.hospital.service.SalaCirurgicaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/salas-cirurgicas")
public class SalaCirurgicaController {
    private final SalaCirurgicaService salas;

    public SalaCirurgicaController(SalaCirurgicaService salas) {
        this.salas = salas;
    }

    @GetMapping
    public List<SalaCirurgica> listar() {
        return salas.listar();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SalaCirurgica cadastrar(@Valid @RequestBody CadastroSala request) {
        return salas.cadastrar(request.nome());
    }

    public record CadastroSala(@NotBlank @Size(max = 80) String nome) {}
}
