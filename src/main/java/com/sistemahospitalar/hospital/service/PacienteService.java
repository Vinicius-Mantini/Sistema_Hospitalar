package com.sistemahospitalar.hospital.service;

import com.sistemahospitalar.hospital.model.Paciente;
import com.sistemahospitalar.hospital.repository.PacienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class PacienteService {
    private final PacienteRepository repo;
    private final HistoricoMedicoService historicos;

    public PacienteService(PacienteRepository repo, HistoricoMedicoService historicos) {
        this.repo = repo;
        this.historicos = historicos;
    }

    public List<Paciente> listar() { return repo.findAll(); }

    @Transactional
    public Paciente salvar(Paciente paciente) {
        if (paciente.getId() != null) throw new IllegalArgumentException("A criação não aceita ID informado.");
        Paciente salvo = repo.save(paciente);
        historicos.criar(salvo);
        return salvo;
    }

    public Paciente buscar(Long id) {
        return repo.findById(id).orElseThrow(() -> new NoSuchElementException("Paciente não encontrado."));
    }
}
