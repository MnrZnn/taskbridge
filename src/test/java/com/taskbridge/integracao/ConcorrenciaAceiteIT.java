package com.taskbridge.integracao;

import com.taskbridge.model.Usuario;
import com.taskbridge.repository.ContratacaoRepository;
import com.taskbridge.service.PropostaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Dois aceites simultâneos no mesmo projeto (duas abas, duplo clique, dois dispositivos):
 * o lock pessimista no projeto serializa as transações e só uma contratação nasce.
 */
class ConcorrenciaAceiteIT extends TesteIntegracao {

    @Autowired
    private PropostaService propostaService;

    @Autowired
    private ContratacaoRepository contratacaoRepository;

    @Test
    void doisAceitesSimultaneosGeramUmaUnicaContratacao() throws Exception {
        Usuario cliente = novoCliente();
        long projetoId = criarProjetoPublicado(cliente);
        long propostaA = enviarProposta(novoPrestador(), projetoId);
        long propostaB = enviarProposta(novoPrestador(), projetoId);

        CountDownLatch largada = new CountDownLatch(1);
        Callable<Boolean> aceitarA = aceitarApos(largada, propostaA, cliente.getId());
        Callable<Boolean> aceitarB = aceitarApos(largada, propostaB, cliente.getId());

        List<Boolean> resultados;
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            List<Future<Boolean>> futuros = List.of(executor.submit(aceitarA), executor.submit(aceitarB));
            largada.countDown();
            resultados = List.of(futuros.get(0).get(), futuros.get(1).get());
        }

        assertThat(resultados).containsExactlyInAnyOrder(true, false);
        long contratacoesDoProjeto = contratacaoRepository.findAll().stream()
                .filter(contratacao -> contratacao.getProjeto().getId() == projetoId)
                .count();
        assertThat(contratacoesDoProjeto).isEqualTo(1);
    }

    private Callable<Boolean> aceitarApos(CountDownLatch largada, long propostaId, Long clienteId) {
        return () -> {
            largada.await();
            try {
                propostaService.aceitar(propostaId, clienteId);
                return true;
            } catch (RuntimeException perdeuACorrida) {
                return false;
            }
        };
    }
}
