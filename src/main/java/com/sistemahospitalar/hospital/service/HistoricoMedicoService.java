package com.sistemahospitalar.hospital.service;

import com.sistemahospitalar.hospital.dto.HistoricoPacienteResponse;
import com.sistemahospitalar.hospital.dto.InternacaoHistoricoResponse;
import com.sistemahospitalar.hospital.model.*;
import com.sistemahospitalar.hospital.repository.HistoricoMedicoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Comparator;
import java.util.List;
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
        List<Internacao> internacoes = historico.getInternacoes();
        List<InternacaoHistoricoResponse> detalhes = internacoes.stream()
            .sorted(Comparator.comparing(Internacao::getDataEntrada).thenComparing(Internacao::getId).reversed())
            .map(internacao -> new InternacaoHistoricoResponse(
                internacao.getId(), internacao.getDataEntrada(),
                internacao.getDataAlta() != null ? internacao.getDataAlta() : internacao.getDataCancelamento(),
                internacao.getSituacao(), internacao.getQuarto().getId(), internacao.getQuarto().getNumero()))
            .toList();
        return new HistoricoPacienteResponse(
            pacienteId,
            historico.getAtendimentos().stream().map(Atendimento::getId).toList(),
            internacoes.stream().map(Internacao::getId).toList(),
            detalhes);
    }

    private HistoricoMedico obterOuCriar(Paciente paciente) {
        return repo.findByPacienteId(paciente.getId()).orElseGet(() -> {
            HistoricoMedico historico = new HistoricoMedico();
            historico.setPaciente(paciente);
            return repo.save(historico);
        });
    }
}
