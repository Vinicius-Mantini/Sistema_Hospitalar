package com.sistemahospitalar.hospital.service;

import com.sistemahospitalar.hospital.dto.CriarEncaminhamentoRequest;
import com.sistemahospitalar.hospital.model.*;
import com.sistemahospitalar.hospital.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class ProfissionalSaudeService {
    private final ProfissionalSaudeRepository repo;
    private final MedicoEspecialistaRepository especialistas;
    private final EncaminhamentoRepository encaminhamentos;
    private final ProcedimentoCirurgicoRepository cirurgias;
    private final PacienteRepository pacientes;

    public ProfissionalSaudeService(ProfissionalSaudeRepository repo,
            MedicoEspecialistaRepository especialistas, EncaminhamentoRepository encaminhamentos,
            ProcedimentoCirurgicoRepository cirurgias, PacienteRepository pacientes) {
        this.repo = repo;
        this.especialistas = especialistas;
        this.encaminhamentos = encaminhamentos;
        this.cirurgias = cirurgias;
        this.pacientes = pacientes;
    }

    public List<ProfissionalSaude> listar() { return repo.findAll(); }
    public ProfissionalSaude buscar(Long id) {
        return repo.findById(id).orElseThrow(() -> new NoSuchElementException("Profissional não encontrado."));
    }
    public ProfissionalSaude salvar(ProfissionalSaude p) {
        if (p.getId() != null) throw new IllegalArgumentException("A criação não aceita ID informado.");
        return repo.save(p);
    }
    public List<MedicoEspecialista> porEspecialidade(String e) {
        if (e == null || e.isBlank()) throw new IllegalArgumentException("Informe a especialidade.");
        return especialistas.findByEspecialidadeIgnoreCaseAndAtivoTrue(e.trim());
    }

    @Transactional
    public Encaminhamento encaminhar(Long clinicoId, CriarEncaminhamentoRequest request) {
        if (!(buscar(clinicoId) instanceof MedicoClinicoGeral clinico)) {
            throw new IllegalArgumentException("O profissional não é clínico geral.");
        }
        if (!(buscar(request.especialistaId()) instanceof MedicoEspecialista especialista)) {
            throw new IllegalArgumentException("O destino não é médico especialista.");
        }
        Paciente paciente = pacientes.findById(request.pacienteId())
            .orElseThrow(() -> new NoSuchElementException("Paciente não encontrado."));
        return encaminhamentos.save(clinico.encaminharPaciente(paciente, especialista, request.motivo()));
    }

    public List<Encaminhamento> listarEncaminhamentos(Long clinicoId) {
        return encaminhamentos.findByClinicoId(clinicoId);
    }

    public List<ProcedimentoCirurgico> cirurgiasDoProfissional(Long profissionalId) {
        if (!(buscar(profissionalId) instanceof Cirurgiao)) {
            throw new IllegalArgumentException("O profissional não é cirurgião.");
        }
        return cirurgias.findDoCirurgiao(profissionalId);
    }
}
