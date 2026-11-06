package com.sistemahospitalar.hospital.service;

import com.sistemahospitalar.hospital.dto.CriarAtendimentoRequest;
import com.sistemahospitalar.hospital.model.*;
import com.sistemahospitalar.hospital.model.enums.StatusAtendimentoEnum;
import com.sistemahospitalar.hospital.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

@Service
public class AtendimentoService {
    private static final Set<StatusAtendimentoEnum> STATUS_RESERVADOS = Set.of(
        StatusAtendimentoEnum.AGENDADA, StatusAtendimentoEnum.CONFIRMADA,
        StatusAtendimentoEnum.EM_ATENDIMENTO);

    private final AtendimentoRepository repo;
    private final PacienteRepository pacientes;
    private final ProfissionalSaudeRepository profissionais;
    private final HistoricoMedicoService historicos;

    public AtendimentoService(AtendimentoRepository repo, PacienteRepository pacientes,
            ProfissionalSaudeRepository profissionais, HistoricoMedicoService historicos) {
        this.repo = repo;
        this.pacientes = pacientes;
        this.profissionais = profissionais;
        this.historicos = historicos;
    }

    @Transactional
    public Atendimento criar(CriarAtendimentoRequest dados) {
        Paciente paciente = pacientes.findById(dados.pacienteId())
            .orElseThrow(() -> new NoSuchElementException("Paciente não encontrado."));
        ProfissionalSaude profissional = profissionais.findById(dados.profissionalId())
            .orElseThrow(() -> new NoSuchElementException("Profissional não encontrado."));
        if (!profissional.estaDisponivel()) throw new IllegalStateException("Profissional está inativo.");

        Atendimento atendimento = switch (dados.tipo()) {
            case ROTINA -> {
                ConsultaRotina consulta = new ConsultaRotina();
                consulta.setMotivo(dados.motivo());
                yield consulta;
            }
            case ESPECIALIZADA -> {
                ConsultaEspecializada consulta = new ConsultaEspecializada();
                consulta.setEspecialidade(dados.especialidade());
                yield consulta;
            }
            case EMERGENCIA -> {
                AtendimentoEmergencia emergencia = new AtendimentoEmergencia();
                emergencia.setPrioridade(dados.prioridade());
                yield emergencia;
            }
            case CIRURGICO -> {
                ProcedimentoCirurgico cirurgia = new ProcedimentoCirurgico();
                cirurgia.setDescricao(dados.descricao());
                cirurgia.setTipoCirurgia(dados.tipoCirurgia());
                cirurgia.setObservacoesPreOperatorias(dados.observacoesPreOperatorias());
                if (dados.observacoesPosOperatorias() != null && !dados.observacoesPosOperatorias().isBlank()) {
                    throw new IllegalArgumentException("Observações pós-operatórias só podem ser registradas após a cirurgia.");
                }
                yield cirurgia;
            }
        };

        atendimento.setPaciente(paciente);
        atendimento.setProfissionalResponsavel(profissional);
        atendimento.setDataHora(dados.dataHora());
        if (!atendimento.validarCondicoes()) {
            throw new IllegalArgumentException("Condições não atendidas para esta modalidade e profissional.");
        }
        if (!(atendimento instanceof AtendimentoEmergencia)) {
            if (dados.dataHora().isBefore(LocalDateTime.now())) {
                throw new IllegalArgumentException("Agendamentos devem usar data e hora futuras.");
            }
            if (repo.existsByProfissionalResponsavelIdAndDataHoraAndStatusIn(
                    profissional.getId(), dados.dataHora(), STATUS_RESERVADOS)) {
                throw new IllegalStateException("Profissional já possui atendimento agendado neste horário.");
            }
        }
        if (atendimento instanceof ProcedimentoCirurgico cirurgia && profissional instanceof Cirurgiao cirurgiao) {
            cirurgiao.registrarProcedimento(cirurgia);
        }
        Atendimento salvo = repo.save(atendimento);
        historicos.adicionarAtendimento(paciente, salvo);
        return salvo;
    }

    @Transactional(readOnly = true)
    public List<Atendimento> listar() { return repo.findAll(); }

    public Atendimento buscar(Long id) {
        return repo.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Atendimento não encontrado."));
    }

    @Transactional
    public Atendimento alterarStatus(Long id, StatusAtendimentoEnum status) {
        Atendimento atendimento = buscar(id);
        if (!(atendimento instanceof ConsultaRotina || atendimento instanceof ConsultaEspecializada)) {
            throw new IllegalArgumentException("Use os endpoints específicos para emergência ou cirurgia.");
        }
        atendimento.alterarStatus(status);
        return atendimento;
    }

    @Transactional
    public Atendimento registrarObservacoes(Long id, String observacoes) {
        Atendimento atendimento = buscar(id);
        if (atendimento.getStatus() != StatusAtendimentoEnum.CONCLUIDA) {
            throw new IllegalStateException("As observações são registradas após a realização da consulta.");
        }
        if (atendimento instanceof ConsultaRotina rotina) rotina.setObservacoes(observacoes);
        else if (atendimento instanceof ConsultaEspecializada especializada) especializada.setObservacoes(observacoes);
        else throw new IllegalArgumentException("Apenas consultas permitem esse registro.");
        return atendimento;
    }

    @Transactional
    public ProcedimentoCirurgico iniciarCirurgia(Long id) {
        ProcedimentoCirurgico cirurgia = buscarCirurgia(id);
        cirurgia.iniciar();
        return cirurgia;
    }

    @Transactional
    public ProcedimentoCirurgico concluirCirurgia(Long id) {
        ProcedimentoCirurgico cirurgia = buscarCirurgia(id);
        cirurgia.concluir();
        return cirurgia;
    }

    @Transactional
    public ProcedimentoCirurgico observacoesPosOperatorias(Long id, String observacoes) {
        ProcedimentoCirurgico cirurgia = buscarCirurgia(id);
        if (cirurgia.getStatusProcedimento() != com.sistemahospitalar.hospital.model.enums.StatusProcedimentoEnum.CONCLUIDO) {
            throw new IllegalStateException("Observações pós-operatórias exigem procedimento concluído.");
        }
        cirurgia.setObservacoesPosOperatorias(observacoes);
        return cirurgia;
    }

    @Transactional
    public ProcedimentoCirurgico cancelarCirurgia(Long id) {
        ProcedimentoCirurgico cirurgia = buscarCirurgia(id);
        cirurgia.cancelar();
        return cirurgia;
    }

    @Transactional
    public AtendimentoEmergencia iniciarEmergencia(Long id) {
        AtendimentoEmergencia emergencia = buscarEmergencia(id);
        emergencia.iniciar();
        return emergencia;
    }

    @Transactional
    public AtendimentoEmergencia concluirEmergencia(Long id) {
        AtendimentoEmergencia emergencia = buscarEmergencia(id);
        emergencia.concluir();
        return emergencia;
    }

    private ProcedimentoCirurgico buscarCirurgia(Long id) {
        if (!(buscar(id) instanceof ProcedimentoCirurgico cirurgia)) {
            throw new IllegalArgumentException("O atendimento informado não é cirúrgico.");
        }
        return cirurgia;
    }

    private AtendimentoEmergencia buscarEmergencia(Long id) {
        if (!(buscar(id) instanceof AtendimentoEmergencia emergencia)) {
            throw new IllegalArgumentException("O atendimento informado não é emergência.");
        }
        return emergencia;
    }
}
