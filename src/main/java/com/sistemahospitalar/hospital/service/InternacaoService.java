package com.sistemahospitalar.hospital.service;

import com.sistemahospitalar.hospital.dto.CriarInternacaoRequest;
import com.sistemahospitalar.hospital.model.*;
import com.sistemahospitalar.hospital.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

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
        Paciente paciente = pacientes.findById(dados.pacienteId())
            .orElseThrow(() -> new NoSuchElementException("Paciente não encontrado."));
        Quarto quarto = quartos.bloquearPorId(dados.quartoId())
            .orElseThrow(() -> new NoSuchElementException("Quarto não encontrado."));
        quarto.ocuparLeito();
        Internacao internacao = new Internacao();
        internacao.setPaciente(paciente);
        internacao.setQuarto(quarto);
        LocalDateTime entrada = dados.dataEntrada() == null ? LocalDateTime.now() : dados.dataEntrada();
        if (entrada.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("A data de entrada não pode estar no futuro.");
        }
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
        Internacao internacao = buscar(id);
        Quarto quarto = quartos.bloquearPorId(internacao.getQuarto().getId())
            .orElseThrow(() -> new NoSuchElementException("Quarto não encontrado."));
        internacao.registrarAlta();
        quarto.liberarLeito();
        return internacao;
    }

    @Transactional
    public Internacao cancelar(Long id) {
        Internacao internacao = buscar(id);
        Quarto quarto = quartos.bloquearPorId(internacao.getQuarto().getId())
            .orElseThrow(() -> new NoSuchElementException("Quarto não encontrado."));
        internacao.cancelar();
        quarto.liberarLeito();
        return internacao;
    }
}
