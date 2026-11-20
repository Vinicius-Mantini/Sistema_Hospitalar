package com.sistemahospitalar.hospital.service;

import com.sistemahospitalar.hospital.dto.CriarInternacaoRequest;
import com.sistemahospitalar.hospital.exception.InternacaoAtivaException;
import com.sistemahospitalar.hospital.model.*;
import com.sistemahospitalar.hospital.model.enums.SituacaoInternacaoEnum;
import com.sistemahospitalar.hospital.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;

@Service
public class InternacaoService {
    private final InternacaoRepository repo;
    private final QuartoRepository quartos;
    private final PacienteRepository pacientes;
    private final HistoricoMedicoService historicos;

    public InternacaoService(InternacaoRepository repo, QuartoRepository quartos,
            PacienteRepository pacientes, HistoricoMedicoService historicos) {
        this.repo = repo;
        this.quartos = quartos;
        this.pacientes = pacientes;
        this.historicos = historicos;
    }

    @Transactional
    public Internacao criar(CriarInternacaoRequest dados) {
        Paciente paciente = pacientes.bloquearPorId(dados.pacienteId())
            .orElseThrow(() -> new NoSuchElementException("Paciente não encontrado."));
        if (repo.existsByPacienteIdAndSituacao(paciente.getId(), SituacaoInternacaoEnum.ATIVA)) {
            throw new InternacaoAtivaException("Paciente já possui uma internação ativa.");
        }
        Quarto quarto = bloquearQuarto(dados.quartoId());
        LocalDateTime agora = LocalDateTime.now();
        LocalDateTime entrada = dados.dataEntrada() == null ? agora : dados.dataEntrada();
        if (entrada.isAfter(agora)) {
            throw new IllegalArgumentException("A data de entrada não pode estar no futuro.");
        }

        quarto.ocuparLeito();
        Internacao internacao = new Internacao();
        internacao.setPaciente(paciente);
        internacao.setQuarto(quarto);
        internacao.setDataEntrada(entrada);
        internacao.setObservacoes(dados.observacoes());
        Internacao salvo = repo.save(internacao);
        historicos.adicionarInternacao(paciente, salvo);
        return salvo;
    }

    @Transactional(readOnly = true)
    public List<Internacao> listar() { return repo.findAll(); }
    public Internacao buscar(Long id) {
        return repo.findById(id).orElseThrow(() -> new NoSuchElementException("Internação não encontrada."));
    }

    @Transactional
    public Internacao registrarAlta(Long id) {
        return encerrar(id, Internacao::registrarAlta);
    }

    @Transactional
    public Internacao cancelar(Long id) {
        return encerrar(id, Internacao::cancelar);
    }

    private Internacao encerrar(Long id, Consumer<Internacao> operacao) {
        // Busque só o ID para adquirir a trava do quarto antes de carregar a ocupação.
        Long quartoId = repo.buscarQuartoId(id)
            .orElseThrow(() -> new NoSuchElementException("Internação não encontrada."));
        Quarto quarto = bloquearQuarto(quartoId);
        Internacao internacao = repo.bloquearPorId(id)
            .orElseThrow(() -> new NoSuchElementException("Internação não encontrada."));
        operacao.accept(internacao);
        quarto.liberarLeito();
        return internacao;
    }

    private Quarto bloquearQuarto(Long id) {
        return quartos.bloquearPorId(id)
            .orElseThrow(() -> new NoSuchElementException("Quarto não encontrado."));
    }
}
