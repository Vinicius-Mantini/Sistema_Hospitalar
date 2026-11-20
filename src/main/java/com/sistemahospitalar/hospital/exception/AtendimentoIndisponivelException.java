package com.sistemahospitalar.hospital.exception;

public class AtendimentoIndisponivelException extends IllegalStateException {
    public AtendimentoIndisponivelException(String mensagem) {
        super(mensagem);
    }
}
