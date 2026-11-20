package com.sistemahospitalar.hospital.service;

import com.sistemahospitalar.hospital.model.SalaCirurgica;
import com.sistemahospitalar.hospital.repository.SalaCirurgicaRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SalaCirurgicaService {
    private final SalaCirurgicaRepository salas;

    public SalaCirurgicaService(SalaCirurgicaRepository salas) {
        this.salas = salas;
    }

    public List<SalaCirurgica> listar() {
        return salas.findAll(Sort.by("nome").and(Sort.by("id")));
    }

    public SalaCirurgica cadastrar(String nome) {
        return salas.save(new SalaCirurgica(nome.strip()));
    }
}
