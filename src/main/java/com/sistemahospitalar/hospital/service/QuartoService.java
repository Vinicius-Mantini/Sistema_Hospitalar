package com.sistemahospitalar.hospital.service;

import com.sistemahospitalar.hospital.model.Quarto;
import com.sistemahospitalar.hospital.model.enums.TipoQuartoEnum;
import com.sistemahospitalar.hospital.repository.QuartoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class QuartoService {
    private final QuartoRepository repo;
    public QuartoService(QuartoRepository repo) { this.repo = repo; }
    public List<Quarto> listar(TipoQuartoEnum tipo) {
        return tipo == null ? repo.findAll() : repo.findByTipo(tipo);
    }
    public Quarto buscar(Long id) {
        return repo.findById(id).orElseThrow(() -> new NoSuchElementException("Quarto não encontrado."));
    }
    public Quarto salvar(Quarto quarto) {
        if (quarto.getId() != null) throw new IllegalArgumentException("A criação não aceita ID informado.");
        return repo.save(quarto);
    }
    @Transactional
    public Quarto colocarEmManutencao(Long id) {
        Quarto quarto = repo.bloquearPorId(id)
            .orElseThrow(() -> new NoSuchElementException("Quarto não encontrado."));
        quarto.colocarEmManutencao();
        return quarto;
    }
    @Transactional
    public Quarto reativar(Long id) {
        Quarto quarto = repo.bloquearPorId(id)
            .orElseThrow(() -> new NoSuchElementException("Quarto não encontrado."));
        quarto.reativar();
        return quarto;
    }
}
