package com.sistemahospitalar.hospital.service;

import com.sistemahospitalar.hospital.model.Atendimento;
import com.sistemahospitalar.hospital.repository.AtendimentoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class AtendimentoService {

    @Autowired
    private AtendimentoRepository repository;

    public Atendimento agendar(Atendimento atendimento) {
        if (!atendimento.validarCondicoes()) {
            throw new IllegalArgumentException("As condicoes para este tipo de atendimento nao foram atendidas.");
        }
        return repository.save(atendimento);
    }

    public List<Atendimento> listarTodos() {
        return repository.findAll();
    }
}