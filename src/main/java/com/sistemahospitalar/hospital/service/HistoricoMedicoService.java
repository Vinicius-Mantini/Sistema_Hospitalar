package com.sistemahospitalar.hospital.service;

import com.sistemahospitalar.hospital.dto.HistoricoPacienteResponse;
import com.sistemahospitalar.hospital.model.*;
import com.sistemahospitalar.hospital.repository.HistoricoMedicoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.NoSuchElementException;

@Service
public class HistoricoMedicoService {
    private final HistoricoMedicoRepository repo;

    public HistoricoMedicoService(HistoricoMedicoRepository repo) { this.repo = repo; }

    @Transactional
    public void criar(Paciente paciente) {
        HistoricoMedico historico = new HistoricoMedico();
        historico.setPaciente(paciente);
        repo.save(historico);
    }

    @Transactional
    public void adicionarAtendimento(Paciente paciente, Atendimento atendimento) {
        HistoricoMedico historico = obterOuCriar(paciente);
        historico.adicionarAtendimento(atendimento);
        repo.save(historico);
    }

    @Transactional
    public void adicionarInternacao(Paciente paciente, Internacao internacao) {
        HistoricoMedico historico = obterOuCriar(paciente);
        historico.adicionarInternacao(internacao);
        repo.save(historico);
    }

    @Transactional(readOnly = true)
    public HistoricoPacienteResponse consultar(Long pacienteId) {
        HistoricoMedico historico = repo.findByPacienteId(pacienteId)
            .orElseThrow(() -> new NoSuchElementException("Histórico médico não encontrado."));
        return new HistoricoPacienteResponse(
            pacienteId,
            historico.getAtendimentos().stream().map(Atendimento::getId).toList(),
            historico.getInternacoes().stream().map(Internacao::getId).toList());
    }

    private HistoricoMedico obterOuCriar(Paciente paciente) {
        return repo.findByPacienteId(paciente.getId()).orElseGet(() -> {
            HistoricoMedico historico = new HistoricoMedico();
            historico.setPaciente(paciente);
            return repo.save(historico);
        });
    }
}
