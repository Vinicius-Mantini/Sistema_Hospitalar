package com.sistemahospitalar.hospital.service;

import com.sistemahospitalar.hospital.dto.CriarAtendimentoRequest;
import com.sistemahospitalar.hospital.exception.AtendimentoIndisponivelException;
import com.sistemahospitalar.hospital.exception.ProfissionalIncompativelException;
import com.sistemahospitalar.hospital.model.*;
import com.sistemahospitalar.hospital.model.enums.StatusAtendimentoEnum;
import com.sistemahospitalar.hospital.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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
    private final SalaCirurgicaRepository salas;
    private final ProcedimentoCirurgicoRepository procedimentos;
    private final HistoricoMedicoService historicos;

    public AtendimentoService(AtendimentoRepository repo, PacienteRepository pacientes,
            ProfissionalSaudeRepository profissionais, SalaCirurgicaRepository salas,
            ProcedimentoCirurgicoRepository procedimentos, HistoricoMedicoService historicos) {
        this.repo = repo;
        this.pacientes = pacientes;
        this.profissionais = profissionais;
        this.salas = salas;
        this.procedimentos = procedimentos;
        this.historicos = historicos;
    }

    @Transactional
    public Atendimento criar(CriarAtendimentoRequest dados) {
        // Adquira as travas na ordem paciente, profissional e, para cirurgias, sala.
        Paciente paciente = pacientes.bloquearPorId(dados.pacienteId())
            .orElseThrow(() -> new NoSuchElementException("Paciente não encontrado."));
        ProfissionalSaude profissional = profissionais.bloquearPorId(dados.profissionalId())
            .orElseThrow(() -> new NoSuchElementException("Profissional não encontrado."));
        if (!profissional.estaDisponivel()) {
            throw new ProfissionalIncompativelException("Profissional está inativo.");
        }

        Atendimento atendimento = switch (dados.tipo()) {
            case ROTINA -> {
                ConsultaRotina consulta = new ConsultaRotina();
                consulta.setMotivo(dados.motivo());
                yield consulta;
            }
            case ESPECIALIZADA -> {
                if (dados.especialidade() == null || dados.especialidade().isBlank()) {
                    throw new IllegalArgumentException("Informe a especialidade da consulta.");
                }
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
        if (!atendimento.profissionalCompativel()) {
            throw new ProfissionalIncompativelException("Profissional incompatível com a modalidade ou especialidade do atendimento.");
        }
        if (!atendimento.validarCondicoes()) {
            throw new IllegalArgumentException("Condições não atendidas para esta modalidade e profissional.");
        }
        validarDisponibilidade(atendimento, dados);
        if (atendimento instanceof ProcedimentoCirurgico cirurgia && profissional instanceof Cirurgiao cirurgiao) {
            cirurgiao.registrarProcedimento(cirurgia);
        }
        Atendimento salvo = repo.save(atendimento);
        historicos.adicionarAtendimento(paciente, salvo);
        return salvo;
    }

    private void validarDisponibilidade(Atendimento atendimento, CriarAtendimentoRequest dados) {
        LocalDateTime dataHora = atendimento.getDataHora();
        if (!(atendimento instanceof AtendimentoEmergencia) && dataHora.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Agendamentos devem usar data e hora futuras.");
        }

        LocalDateTime inicio = horarioNaPrecisaoDoBanco(dataHora);
        LocalDateTime fim = inicio;
        if (atendimento instanceof ProcedimentoCirurgico cirurgia) {
            fim = reservarSala(cirurgia, dados, inicio);
        }

        if (repo.existeConflitoProfissional(
                atendimento.getProfissionalResponsavel().getId(), inicio, fim, STATUS_RESERVADOS)) {
            throw new AtendimentoIndisponivelException("Profissional já possui atendimento agendado neste horário.");
        }
        if (repo.existeConflitoPaciente(atendimento.getPaciente().getId(), inicio, fim, STATUS_RESERVADOS)) {
            throw new AtendimentoIndisponivelException("Paciente já possui atendimento agendado neste horário.");
        }
        atendimento.setDataHora(inicio);
    }

    private LocalDateTime reservarSala(ProcedimentoCirurgico cirurgia, CriarAtendimentoRequest dados,
            LocalDateTime inicio) {
        if (dados.salaCirurgicaId() == null || dados.fimPrevisto() == null) {
            throw new IllegalArgumentException("Informe a sala cirúrgica e o fim previsto da cirurgia.");
        }
        LocalDateTime fim = horarioNaPrecisaoDoBanco(dados.fimPrevisto());
        if (!fim.isAfter(inicio)) {
            throw new IllegalArgumentException("O fim previsto da cirurgia deve ser posterior ao início.");
        }

        SalaCirurgica sala = salas.bloquearPorId(dados.salaCirurgicaId())
            .orElseThrow(() -> new NoSuchElementException("Sala cirúrgica não encontrada."));
        if (procedimentos.existeConflitoSala(sala.getId(), inicio, fim, STATUS_RESERVADOS)) {
            throw new AtendimentoIndisponivelException("Sala cirúrgica já reservada neste período.");
        }
        cirurgia.setSalaCirurgica(sala);
        cirurgia.setFimPrevisto(fim);
        return fim;
    }

    private static LocalDateTime horarioNaPrecisaoDoBanco(LocalDateTime dataHora) {
        try {
            // O H2 arredonda a coluna de horário para microssegundos.
            return dataHora.plusNanos(500).truncatedTo(ChronoUnit.MICROS);
        } catch (DateTimeException erro) {
            throw new IllegalArgumentException("Data e hora fora do intervalo suportado.", erro);
        }
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
