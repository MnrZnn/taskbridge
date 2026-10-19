package com.taskbridge.model.enums;

public enum TipoNotificacao {

    PROPOSTA_RECEBIDA("Nova proposta recebida para \"%s\"."),
    PROPOSTA_ACEITA("Sua proposta para \"%s\" foi aceita."),
    PROPOSTA_RECUSADA("Sua proposta para \"%s\" não foi selecionada."),
    CONVITE_PROPOSTA("Você foi convidado a enviar uma proposta para \"%s\"."),
    PAGAMENTO_CONFIRMADO("Pagamento confirmado. O projeto \"%s\" está em andamento."),
    ENTREGA_ENVIADA("Uma entrega de \"%s\" aguarda sua revisão."),
    ENTREGA_APROVADA("Uma entrega de \"%s\" foi aprovada."),
    REVISAO_SOLICITADA("O cliente pediu revisão em uma entrega de \"%s\"."),
    CONTRATACAO_CONCLUIDA("A contratação de \"%s\" foi concluída.");

    private final String modelo;

    TipoNotificacao(String modelo) {
        this.modelo = modelo;
    }

    public String mensagemPara(String tituloProjeto) {
        return modelo.formatted(tituloProjeto);
    }
}
